package com.example.PrivScan;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
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

import com.example.PrivScan.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SeePrivacyButton extends View {
    private float x;
    private float y;
    private float radius;
    private float lastX;
    private float lastY;
    private float initialX;
    private float initialY;
    private static final int CLICK_THRESHOLD = 10;  // Threshold for click detection (in pixels)
    private Paint paint;
    private String url;
    PopupWindow popupWindow;
    private FrameLayout loadingOverlay;  // Dynamically created overlay
    private ProgressBar loadingSpinner;  // Center spinner
    private Bitmap buttonBitmap;

    // Constructors
    public SeePrivacyButton(Context context) {
        super(context);
        init();
    }

    public SeePrivacyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SeePrivacyButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        radius = 100f;  // Default radius
        paint = new Paint();
        paint.setColor(Color.BLUE);  // Default color
        paint.setAntiAlias(true);  // Smooth edges

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
            case MotionEvent.ACTION_DOWN: {
                // Check if the touch point is within the circular area
                float dx = touchX - x;
                float dy = touchY - y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance > radius) {
                    // If the touch is outside the circle, ignore the event
                    return false;
                }

                // Record initial press position
                initialX = event.getRawX();
                initialY = event.getRawY();
                lastX = initialX;
                lastY = initialY;
                break;
            }

            case MotionEvent.ACTION_MOVE:
                // Calculate movement distance
                float dx = event.getRawX() - lastX;
                float dy = event.getRawY() - lastY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                // If moved beyond threshold, treat as drag, not click
                if (distance > CLICK_THRESHOLD) {
                    x += dx;
                    y += dy;
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    invalidate();  // Redraw the button
                }
                break;

            case MotionEvent.ACTION_UP:
                // Determine if this was a click (not a drag)
                float upDx = event.getRawX() - initialX;
                float upDy = event.getRawY() - initialY;
                float upDistance = (float) Math.sqrt(upDx * upDx + upDy * upDy);

                if (upDistance <= CLICK_THRESHOLD && isTouchInsideButton(event.getRawX(), event.getRawY())) {
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

    // Check if the touch is inside the button area
    private boolean isTouchInsideButton(float touchX, float touchY) {
        float dx = touchX - x;
        float dy = touchY - y;
        return Math.sqrt(dx * dx + dy * dy) <= radius;
    }

    // Set the position of the button
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        invalidate();
    }

    // Set the button size
    public void setSize(float radius) {
        this.radius = radius;

        // Rescale bitmap if needed
        if (buttonBitmap != null) {
            int size = (int)(radius * 2);
            buttonBitmap = Bitmap.createScaledBitmap(buttonBitmap, size, size, true);
        }

        invalidate();
    }

    // Set the button color
    public void setColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    public void displayImage(Context context, Bitmap bitmap) {
        ImageView imageView = new ImageView(context);
        imageView.setImageBitmap(bitmap);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        imageView.setLayoutParams(layoutParams);

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            FrameLayout rootLayout = activity.findViewById(android.R.id.content);
            rootLayout.addView(imageView);
        }
    }

    public void clickButton() throws IOException {
        // Show loading overlay
        showLoadingOverlay();

        Bitmap screenShot = ScreenshotHelper.captureScreenshotWithoutView((Activity) getContext(), this);
        Toast.makeText(getContext(), "Start Privacy Scan!", Toast.LENGTH_SHORT).show();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        screenShot.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] screenshotBytes = baos.toByteArray();

        // Test mode: load local raw image if needed
        byte[] testBytes = readRawResource(R.raw.test);

        FastApiClient.sendImageToFastApi(screenshotBytes, url, new FastApiClient.FastApiCallback() {
            @Override
            public void onImagesProcessed(List<Bitmap> bitmaps) {
                // Hide loading overlay regardless of success/failure
                hideLoadingOverlay();

                if (bitmaps == null || bitmaps.isEmpty()) {
                    Toast toast = Toast.makeText(getContext(), "No privacy-related data collection detected on this page.", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }

                // Show results in popup
                showImagePopup(bitmaps);
            }
        });
    }

    private void showImagePopup(List<Bitmap> bitmaps) {
        // 1) Load layout into container
        FrameLayout popupLayout = new FrameLayout(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.image_popup_layout, popupLayout);

        // 2) Get views from layout
        ViewPager2 viewPager = popupLayout.findViewById(R.id.viewPager);
        ImageView closeButton = popupLayout.findViewById(R.id.closeButton);
        ImageView saveButton = popupLayout.findViewById(R.id.saveButton);
        TextView pageIndicator = popupLayout.findViewById(R.id.pageIndicator);
        TextView policyButton = popupLayout.findViewById(R.id.policyButton);

        // 3) Set adapter
        ImageAdapter imageAdapter = new ImageAdapter(bitmaps);
        viewPager.setAdapter(imageAdapter);

        // 4) Set close button click
        closeButton.setOnClickListener(v -> popupWindow.dismiss());

        saveButton.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();
            Bitmap currentBitmap = bitmaps.get(currentPosition);
            saveImageToGallery(currentBitmap);
        });

        int totalCount = bitmaps.size();
        if (totalCount > 0) {
            pageIndicator.setText("1/" + totalCount);
        }

        policyButton.setOnClickListener(v -> {
            if (url != null && !url.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            } else {
                Toast.makeText(getContext(), "No policy URL set", Toast.LENGTH_SHORT).show();
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int currentIndex = position + 1;
                pageIndicator.setText(currentIndex + "/" + totalCount);
            }
        });

        // 5) Create popup window
        popupWindow = new PopupWindow(
                popupLayout,
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                true
        );

        popupWindow.setOutsideTouchable(true);
        // 6) Show in center
        popupWindow.showAtLocation(getRootView(), Gravity.CENTER, 0, 0);
    }

    private void saveImageToGallery(Bitmap bitmap) {
        Context context = getContext();
        if (!(context instanceof Activity)) return;

        String filename = "privacy_capture_" + System.currentTimeMillis() + ".png";

        OutputStream fos = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");

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
     * Read raw resource file as byte array
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
     * Show semi-transparent overlay + spinner
     */
    private void showLoadingOverlay() {
        Context context = getContext();
        if (!(context instanceof Activity)) return;

        Activity activity = (Activity) context;
        FrameLayout rootLayout = activity.findViewById(android.R.id.content);

        // Remove old overlay if exists
        if (loadingOverlay != null && loadingOverlay.getParent() != null) {
            rootLayout.removeView(loadingOverlay);
        }

        loadingOverlay = new FrameLayout(context);
        loadingOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        loadingOverlay.setBackgroundColor(0x99000000);

        loadingOverlay.setClickable(true);
        loadingOverlay.setFocusable(true);
        loadingOverlay.setFocusableInTouchMode(true);
        loadingOverlay.setOnTouchListener((v, event) -> true);

        loadingSpinner = new ProgressBar(context);
        FrameLayout.LayoutParams spinnerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        spinnerParams.gravity = Gravity.CENTER;
        loadingOverlay.addView(loadingSpinner, spinnerParams);

        rootLayout.addView(loadingOverlay);
    }

    /**
     * Hide overlay
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