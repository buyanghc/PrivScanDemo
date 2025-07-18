package com.example.mobilecpp4app;

import android.app.Application;
import android.graphics.Color;
import com.example.PrivScan.ButtonInjector;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ButtonInjector.init(this, (button, activity) -> {
            button.setPosition(900f, 1950f);
            button.setSize(100f);
            button.setImage(com.example.PrivScan.R.drawable.button_bg);
            button.setColor(Color.parseColor("#8800FF"));
            button.setPolicyUrl("https://buyanghc.github.io/fittrack-policy/index.html");
        });
    }
}
