package com.example.mobilecpp4app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends Fragment {

    public SettingsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Account group
        setItem(view, R.id.row_profile, "Profile", "", true);
//        setItem(view, R.id.row_basic_info, "Basic Info", "", true);
        setItem(view, R.id.row_account_security, "Account & Security", "", true);
        setItem(view, R.id.row_minor_mode, "Minor Mode", "Disabled", true);
        setItem(view, R.id.row_privacy, "Privacy Settings", "", true);
//        setItem(view, R.id.row_address, "Shipping Address", "", true);

        // General group
//        setItem(view, R.id.row_workout_setting, "Manage Permissions", "", true);
        setItem(view, R.id.row_notifications, "Notifications", "", true);
        setItem(view, R.id.row_general_setting, "General Settings", "", true);
        setItem(view, R.id.row_network_diagnose, "Network Diagnosis", "", true);
        setItem(view, R.id.row_labs, "Labs", "", true);
        setItem(view, R.id.row_app_custom, "App Customization", "", true);

        return view;
    }

    /**
     * Utility method to configure a setting row.
     */
    private void setItem(View parent, int rowId, String title, String subtitle, boolean showArrow) {
        View row = parent.findViewById(rowId);
        TextView titleView = row.findViewById(R.id.title);
        TextView subtitleView = row.findViewById(R.id.subtitle);
        ImageView arrow = row.findViewById(R.id.arrow);

        titleView.setText(title);
        subtitleView.setText(subtitle);
        arrow.setVisibility(showArrow ? View.VISIBLE : View.GONE);

        row.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Clicked: " + title, Toast.LENGTH_SHORT).show();
            // You can replace this with actual navigation logic.
        });
    }
}
