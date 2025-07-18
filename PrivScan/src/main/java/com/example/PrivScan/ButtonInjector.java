package com.example.PrivScan;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.preference.PreferenceManager;

import java.util.WeakHashMap;

/**
 * 自动将 SeePrivacyButton 注入到每个 Activity 的辅助类（悬浮在最上层），
 * 并根据 SharedPreferences 中的开关状态自动控制按钮显示/隐藏。
 */
public class ButtonInjector {

    public interface ButtonCustomizer {
        void customize(SeePrivacyButton button, Activity activity);
    }

    private static ButtonCustomizer customizer;

    // 保存每个 Activity 中注入的按钮引用（避免重复创建）
    private static final WeakHashMap<Activity, SeePrivacyButton> buttonMap = new WeakHashMap<>();

    // 是否启用按钮（根据 SharedPreferences 中 seeprivacy_enabled 决定）
    private static boolean isEnabled = true;

    public static void init(Application application, ButtonCustomizer buttonCustomizer) {
        customizer = buttonCustomizer;

        // 1️⃣ 获取当前开关状态
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);
        isEnabled = prefs.getBoolean("seeprivacy_enabled", true);

        // 2️⃣ 监听 SharedPreferences 变化，自动控制按钮显示/隐藏
        prefs.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if ("seeprivacy_enabled".equals(key)) {
                boolean enabled = sharedPreferences.getBoolean(key, true);
                setVisible(enabled); // 实时更新所有按钮显示状态
            }
        });

        // 3️⃣ 注册 Activity 生命周期监听，注入按钮
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                FrameLayout decorView = (FrameLayout) activity.getWindow().getDecorView();

                // 添加一个透明容器作为按钮的宿主
                FrameLayout overlay = new FrameLayout(activity);
                overlay.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));
                overlay.setClickable(false);
                decorView.addView(overlay);

                // 创建按钮并添加到 overlay 中
                SeePrivacyButton button = new SeePrivacyButton(activity);
                overlay.addView(button);
                buttonMap.put(activity, button);

                // 应用开发者自定义样式
                if (customizer != null) {
                    customizer.customize(button, activity);
                }

                // 应用当前开关状态，首次注入就生效
                button.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
            }

            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}
            @Override public void onActivityDestroyed(Activity activity) {
                buttonMap.remove(activity); // 清理引用防止内存泄露
            }
        });
    }

    // 显示或隐藏所有已注入按钮
    public static void setVisible(boolean visible) {
        isEnabled = visible; // 记录状态，供后续新 Activity 使用
        for (SeePrivacyButton button : buttonMap.values()) {
            if (button != null) {
                button.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }
    }

    // 外部调用获取当前按钮是否显示
    public static boolean isVisible() {
        return isEnabled;
    }
}