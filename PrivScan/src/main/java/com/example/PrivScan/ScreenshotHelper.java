package com.example.PrivScan;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

public class ScreenshotHelper {

    /**
     * Captures a screenshot of the current Activity excluding the given button view.
     *
     * @param activity The target Activity.
     * @param button   The View to temporarily hide during capture (e.g., a floating button).
     * @return A Bitmap representing the screenshot.
     */
    public static Bitmap captureScreenshotWithoutView(Activity activity, View button) {
        // 1) Temporarily hide the button to avoid including it in the screenshot
        int originalVisibility = button.getVisibility();
        button.setVisibility(View.INVISIBLE);

        // 2) Get the content area of the Activity (excluding system bars like status/title bar)
        View contentView = activity.findViewById(android.R.id.content);

        // 3) Create a Bitmap with the same size as the contentView
        int width = contentView.getWidth();
        int height = contentView.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // 4) Use Canvas to make the contentView draw itself onto the Bitmap
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        contentView.draw(canvas);

        // 5) Restore the button's original visibility
        button.setVisibility(originalVisibility);

        // 6) Return the final screenshot (including the background)
        return bitmap;
    }
}