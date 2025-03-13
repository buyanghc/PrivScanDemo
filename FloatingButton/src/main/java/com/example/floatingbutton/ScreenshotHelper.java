package com.example.floatingbutton;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.PixelCopy;
import android.view.View;
import android.view.Window;

public class ScreenshotHelper {
    public static Bitmap captureScreenshotWithoutView(Activity activity, View button) {
        // 1) 暂时隐藏按钮，避免把它也截进来
        int originalVisibility = button.getVisibility();
        button.setVisibility(View.INVISIBLE);

        // 2) 获取 Activity 的内容区域（不含系统状态栏/标题栏等）
        View contentView = activity.findViewById(android.R.id.content);

        // 3) 创建一张与 contentView 尺寸相同的 Bitmap
        int width = contentView.getWidth();
        int height = contentView.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // 4) 用 Canvas 让 contentView 自己“画”到这张 Bitmap 上
        Canvas canvas = new Canvas(bitmap);
        contentView.draw(canvas);

        // 5) 恢复按钮的可见性
        button.setVisibility(originalVisibility);

        // 6) 返回最终的截图（包含背景）
        return bitmap;
    }
}
