package com.example.mobilecpp4app;

import android.app.Application;
import android.graphics.Color;
import com.example.PrivScan.ButtonInjector;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the privacy button injector for all activities
        ButtonInjector.init(this, (button, activity) -> {
            // Set the initial position of the button on the screen (x, y in pixels)
            button.setPosition(900f, 1950f);
            // Set the size (diameter) of the button in pixels
            button.setSize(100f);
            // Set the button\'s background image (e.g., icon or custom style)
            button.setImage(com.example.PrivScan.R.drawable.button_bg);
            // Set the background color of the button (used when no image is set)
            button.setColor(Color.parseColor("#8800FF"));
            // Set the official privacy policy URL of your app
            button.setPolicyUrl("https://buyanghc.github.io/fittrack-policy/index.html");
        });
    }
}
