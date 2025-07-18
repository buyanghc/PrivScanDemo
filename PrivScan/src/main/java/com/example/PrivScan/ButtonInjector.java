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
 * Helper class to automatically inject the SeePrivacyButton into each Activity (floating on the top layer),
 * and control the button's visibility based on the toggle state stored in SharedPreferences.
 */
public class ButtonInjector {

    public interface ButtonCustomizer {
        void customize(SeePrivacyButton button, Activity activity);
    }

    private static ButtonCustomizer customizer;

    // Save a reference to the injected button in each Activity (to avoid duplicate creation)
    private static final WeakHashMap<Activity, SeePrivacyButton> buttonMap = new WeakHashMap<>();


    public static void init(Application application, ButtonCustomizer buttonCustomizer) {
        customizer = buttonCustomizer;
        // 3️⃣ Register Activity lifecycle callbacks to inject the button
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                FrameLayout decorView = (FrameLayout) activity.getWindow().getDecorView();

                // Add a transparent container as the host for the button
                FrameLayout overlay = new FrameLayout(activity);
                overlay.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));
                overlay.setClickable(false);
                decorView.addView(overlay);

                // Create the button and add it to the overlay
                SeePrivacyButton button = new SeePrivacyButton(activity);
                overlay.addView(button);
                buttonMap.put(activity, button);

                // Apply custom styles provided by the app developer
                if (customizer != null) {
                    customizer.customize(button, activity);
                }

            }

            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}
            @Override public void onActivityDestroyed(Activity activity) {
                buttonMap.remove(activity); // Clean up references to prevent memory leaks
            }
        });
    }


}