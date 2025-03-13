package com.example.floatingbutton;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.viewpager2.widget.ViewPager2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class FloatingButtonView extends View {
    private float x;
    private float y;
    private float radius;
    private float lastX;
    private float lastY;
    private float initialX;
    private float initialY;
    private static final int CLICK_THRESHOLD = 10;  // 设置点击阈值，单位像素
    private Paint paint;
    private String url;
    PopupWindow popupWindow;

    // 构造方法
    public FloatingButtonView(Context context) {
        super(context);
        init();
    }

    public FloatingButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FloatingButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        radius = 100f;  // 默认半径
        paint = new Paint();
        paint.setColor(Color.BLUE);  // 默认颜色
        paint.setAntiAlias(true);  // 平滑边缘

        this.setFocusable(true);
        this.setClickable(true);
    }

    public void setPolicyUrl(String url) {
        this.url = url;
    }

    @Override
    protected void onDraw(android.graphics.Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(x, y, radius, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:{
                // 判断按下点是否在圆形范围内
                float dx = touchX - x;
                float dy = touchY - y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance > radius) {
                    // 如果按下位置不在按钮圆形里，就不消费这次事件
                    return false;
                }

                // 记录按下的初始位置
                initialX = event.getRawX();
                initialY = event.getRawY();
                lastX = initialX;
                lastY = initialY;
                break;
            }

            case MotionEvent.ACTION_MOVE:
                // 计算按下后的移动距离
                float dx = event.getRawX() - lastX;
                float dy = event.getRawY() - lastY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                // 如果移动超过了阈值，则认为是拖动，不触发点击事件
                if (distance > CLICK_THRESHOLD) {
                    x += dx;
                    y += dy;
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    invalidate();  // 重新绘制按钮
                }
                break;

            case MotionEvent.ACTION_UP:
                // 计算松开时的移动距离，判断是否没有移动
                float upDx = event.getRawX() - initialX;
                float upDy = event.getRawY() - initialY;
                float upDistance = (float) Math.sqrt(upDx * upDx + upDy * upDy);

                // 如果移动距离小于阈值，触发点击事件
                if (upDistance <= CLICK_THRESHOLD && isTouchInsideButton(event.getRawX(), event.getRawY())) {
                    // 触发截图方法
                    try {
                        clickButton();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
        }
        return true;
    }

    // 判断触摸是否在按钮内部
    private boolean isTouchInsideButton(float touchX, float touchY) {
        float dx = touchX - x;
        float dy = touchY - y;
        return Math.sqrt(dx * dx + dy * dy) <= radius;
    }

    // 设置按钮的位置
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        invalidate();
    }

    // 设置按钮的大小
    public void setSize(float radius) {
        this.radius = radius;
        invalidate();
    }

    // 设置按钮的颜色
    public void setColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    public void displayImage(Context context, Bitmap bitmap) {
        // 创建一个新的 ImageView
        ImageView imageView = new ImageView(context);
        imageView.setImageBitmap(bitmap);

        // 设置 ImageView 的布局参数
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        imageView.setLayoutParams(layoutParams);

        // 获取当前 Activity 并将 ImageView 添加到其中
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            // 假设你要添加到 Activity 的根布局中
            FrameLayout rootLayout = activity.findViewById(android.R.id.content);
            rootLayout.addView(imageView);
        }
    }

    public void clickButton() throws IOException {
        Bitmap screenShot = ScreenshotHelper.captureScreenshotWithoutView((Activity) getContext(), this);
        Toast.makeText(getContext(), "Floating Button Clicked!", Toast.LENGTH_SHORT).show();

        // 2) 将截屏 Bitmap 压缩为 PNG，得到原始二进制
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        screenShot.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] screenshotBytes = baos.toByteArray();

//        // 2) 从 raw 目录读取 "test.png" 的原始数据
        byte[] testBytes = readRawResource(R.raw.test);

        // 3) 调用 FastApiClient 发送
        FastApiClient.sendImageToFastApi(screenshotBytes, url, new FastApiClient.FastApiCallback() {
            @Override
            public void onImagesProcessed(List<Bitmap> bitmaps) {
                Log.d("FastApiClient", "11111111");
                if (bitmaps != null && !bitmaps.isEmpty()) {
                    Log.d("FastApiClient", "22222222222");
                    showImagePopup(bitmaps);
                    Log.d("FastApiClient", "333333333");
                }
            }
        });
    }


    private void showImagePopup(List<Bitmap> bitmaps) {
        // 1) 先用一个容器加载布局
        FrameLayout popupLayout = new FrameLayout(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        // 将 image_popup_layout.xml 填充到 popupLayout
        inflater.inflate(R.layout.image_popup_layout, popupLayout);

        // 2) 获取布局里的控件
        ViewPager2 viewPager = popupLayout.findViewById(R.id.viewPager);
        ImageView closeButton = popupLayout.findViewById(R.id.closeButton);

        // 3) 给 ViewPager2 设置适配器
        ImageAdapter imageAdapter = new ImageAdapter(bitmaps);
        viewPager.setAdapter(imageAdapter);

        // 4) 点击关闭按钮，关闭弹窗
        closeButton.setOnClickListener(v -> {
            popupWindow.dismiss();  // 关闭
        });

        // 5) 创建 PopupWindow 来包裹这个 popupLayout
        popupWindow = new PopupWindow(
                popupLayout,
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                true  // 设置可获得焦点
        );

        popupWindow.setOutsideTouchable(true);
        // 6) 显示在当前界面正中央
        popupWindow.showAtLocation(getRootView(), Gravity.CENTER, 0, 0);
    }

    private void saveImage(Bitmap bitmap) {
        // 保存图像的逻辑
        // 你可以将其保存到本地文件或图库
        try {
            FileOutputStream outStream = new FileOutputStream(new File(getContext().getExternalFilesDir(null), "saved_image.png"));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
            Toast.makeText(getContext(), "Image Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 从 raw 目录读取文件的原始二进制
     */
    private byte[] readRawResource(int resId) throws IOException {
        InputStream is = getContext().getResources().openRawResource(resId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        is.close();
        return baos.toByteArray();
    }

}