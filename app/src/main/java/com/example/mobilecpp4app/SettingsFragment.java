package com.example.mobilecpp4app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends Fragment {

    public SettingsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 找到 Switch 控件
        Switch seePrivacySwitch = view.findViewById(R.id.switch_seeprivacy);

        // 获取 SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // 设置 Switch 初始状态
        boolean isEnabled = prefs.getBoolean("seeprivacy_enabled", true);
        seePrivacySwitch.setChecked(isEnabled);

        // 监听开关变化，保存到 SharedPreferences
        seePrivacySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("seeprivacy_enabled", isChecked).apply();
        });

        return view;
    }
}
