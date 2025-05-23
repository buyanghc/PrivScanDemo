package com.example.floatingbutton;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.viewpager2.widget.ViewPager2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private FrameLayout loadingOverlay;  // 动态创建的遮罩层
    private ProgressBar loadingSpinner;  // 中央转圈
    private Bitmap buttonBitmap;

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

        if (buttonBitmap != null) {
            float left = x - buttonBitmap.getWidth() / 2f;
            float top = y - buttonBitmap.getHeight() / 2f;
            canvas.drawBitmap(buttonBitmap, left, top, paint);
        } else {
            canvas.drawCircle(x, y, radius, paint);
        }
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

        // 当大小变化时，也要重新缩放图片
        if (buttonBitmap != null) {
            int size = (int)(radius * 2);
            buttonBitmap = Bitmap.createScaledBitmap(buttonBitmap, size, size, true);
        }

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
        // 显示遮罩
        showLoadingOverlay();

        Bitmap screenShot = ScreenshotHelper.captureScreenshotWithoutView((Activity) getContext(), this);
        Toast.makeText(getContext(), "Start Analyzing User Privacy Information Collected on the Current Page!", Toast.LENGTH_SHORT).show();

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
                // 不管成功失败，先隐藏遮罩
                hideLoadingOverlay();

                if (bitmaps == null || bitmaps.isEmpty()) {
                    // 没有图片时，弹出居中提示
                    Toast toast = Toast.makeText(getContext(), "There is no collection of personal privacy data on the current page.", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return;
                }

                // 如果有图像
                showImagePopup(bitmaps);
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
        ImageView saveButton = popupLayout.findViewById(R.id.saveButton);
        TextView pageIndicator = popupLayout.findViewById(R.id.pageIndicator);

        // 3) 给 ViewPager2 设置适配器
        ImageAdapter imageAdapter = new ImageAdapter(bitmaps);
        viewPager.setAdapter(imageAdapter);

        // 4) 点击关闭按钮，关闭弹窗
        closeButton.setOnClickListener(v -> {
            popupWindow.dismiss();  // 关闭
        });

        saveButton.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();  // 当前页面索引
            Bitmap currentBitmap = bitmaps.get(currentPosition);
            // 调用保存方法
            saveImageToGallery(currentBitmap);
        });

        // 获取总图片数
        int totalCount = bitmaps.size();
        // 如果至少有1张图，让它先显示 "1/3" 之类的
        if (totalCount > 0) {
            pageIndicator.setText("1/" + totalCount);
        }

        // 注册页面切换的回调
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // position 是0-based，所以要 +1
                int currentIndex = position + 1;
                pageIndicator.setText(currentIndex + "/" + totalCount);
            }
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

    private void saveImageToGallery(Bitmap bitmap) {
        Context context = getContext();
        if (!(context instanceof Activity)) return;

        // 文件名
        String filename = "privacy_capture_" + System.currentTimeMillis() + ".png";

        OutputStream fos = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");

            // 保存到 Pictures 目录
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            }

            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (imageUri == null) {
                Toast.makeText(context, "Failed to create MediaStore entry", Toast.LENGTH_SHORT).show();
                return;
            }

            fos = resolver.openOutputStream(imageUri);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);

            Toast.makeText(context, "Image saved to Gallery", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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

    /**
     * 显示半透明黑幕 + 中间转圈
     */
    private void showLoadingOverlay() {
        Context context = getContext();
        if (!(context instanceof Activity)) return;

        Activity activity = (Activity) context;
        FrameLayout rootLayout = activity.findViewById(android.R.id.content);

        // 先把旧的 overlay 移除，避免重复叠加
        if (loadingOverlay != null && loadingOverlay.getParent() != null) {
            rootLayout.removeView(loadingOverlay);
        }

        // 无论如何，重新 new 一个
        loadingOverlay = new FrameLayout(context);
        loadingOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        loadingOverlay.setBackgroundColor(0x99000000);

        // 让遮罩本身可点击 & 可抢占焦点
        loadingOverlay.setClickable(true);
        loadingOverlay.setFocusable(true);
        loadingOverlay.setFocusableInTouchMode(true);

        // 拦截触摸事件
        loadingOverlay.setOnTouchListener((v, event) -> true);

        // 转圈
        loadingSpinner = new ProgressBar(context);
        FrameLayout.LayoutParams spinnerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        spinnerParams.gravity = Gravity.CENTER;
        loadingOverlay.addView(loadingSpinner, spinnerParams);

        // 再把新的 overlay 添加到根布局
        rootLayout.addView(loadingOverlay);
    }

    /**
     * 隐藏遮罩
     */
    private void hideLoadingOverlay() {
        if (loadingOverlay != null) {
            if (loadingOverlay.getParent() != null) {
                FrameLayout rootLayout = ((Activity) getContext()).findViewById(android.R.id.content);
                rootLayout.removeView(loadingOverlay);
            }
            loadingOverlay = null;
        }
    }

    public void setImage(int resId) {
        Bitmap original = BitmapFactory.decodeResource(getResources(), resId);
        int size = (int)(radius * 2);
        buttonBitmap = Bitmap.createScaledBitmap(original, size, size, true);
        invalidate();
    }

}