package com.example.PrivScan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FastApiClient {
    private static final String API_URL = "https://shorten-966231927754.australia-southeast1.run.app/process_image/";
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Used to send image and URL to FastAPI
    public static void sendImageToFastApi(byte[] fileBytes, String urlString, FastApiCallback callback) {
        executorService.submit(() -> {
            try {
                byte[] zipRaw = uploadImage(fileBytes, urlString);
                List<Bitmap> bitmaps = processZipFile(zipRaw);

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) callback.onImagesProcessed(bitmaps);
                });
            } catch (IOException e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        // You may define an onFailure callback; here we just return null
                        callback.onImagesProcessed(null);
                    }
                });
            }
        });
    }

    // Upload image and URL to FastAPI
    private static byte[] uploadImage(byte[] fileBytes, String urlString) throws IOException {

        // Check if the current thread is the main thread
        if (Looper.getMainLooper().isCurrentThread()) {
            Log.e("Network", "You are making network request on the main thread! This will cause ANR.");
        }

        // -----------------------------------------------------------
        // Use multipart/form-data for uploading
        // -----------------------------------------------------------
        Log.d("FastApiClient", "Starting uploadImage method (multipart)...");

        // 1) Convert Bitmap to PNG byte array (already provided as fileBytes)

        // 2) Construct boundary
        String boundary = "Boundary-" + System.currentTimeMillis();

        // 3) Create HTTP connection
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setConnectTimeout(60000);  // Connection timeout
        connection.setReadTimeout(60000);     // Read timeout

        // 4) Write multipart data
        OutputStream outputStream = connection.getOutputStream();

        // -- (a) Write the "data" field (plain text)
        String dataPart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"data\"\r\n\r\n"
                + urlString + "\r\n";
        outputStream.write(dataPart.getBytes());

        // -- (b) Write the "file" field (file content)
        String filePartHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"myimage.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n";
        outputStream.write(filePartHeader.getBytes());

        // Write fileBytes
        outputStream.write(fileBytes);
        outputStream.write("\r\n".getBytes());

        // End boundary
        String end = "--" + boundary + "--\r\n";
        outputStream.write(end.getBytes());

        outputStream.flush();
        outputStream.close();

        // 5) Get response code
        int responseCode = connection.getResponseCode();

        // 6) Read response body (use getInputStream() for 2xx, else getErrorStream())
        InputStream inputStream;
        if (responseCode >= 200 && responseCode < 300) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        byte[] responseBytes = baos.toByteArray();

        connection.disconnect();
        return responseBytes;
    }

    // Convert input stream to string
    private static String convertStreamToString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        return response.toString();
    }

    // Unzip the returned ZIP file and extract images from it
    private static List<Bitmap> processZipFile(byte[] zipRawBytes) {
        List<Bitmap> bitmapList = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipRawBytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().endsWith(".png")) {
                    // 1) Read the file content inside ZIP into memory
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zipInputStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    byte[] imageData = baos.toByteArray();

                    // 2) Step 1: Get only width/height without decoding pixels
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);

                    int outWidth = options.outWidth;
                    int outHeight = options.outHeight;

                    // 3) Calculate suitable inSampleSize to downscale the image
                    //    Example: limit dimensions to a max of 2000 pixels (can be adjusted)
                    int maxSide = 2000;
                    int sampleSize = 1;
                    while ((outWidth / sampleSize) > maxSide || (outHeight / sampleSize) > maxSide) {
                        sampleSize *= 2;  // Each increment halves the decode size
                    }

                    // 4) Step 2: Actually decode (with scaling)
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = sampleSize;
                    // Optional: save memory using RGB_565 or other config
                    // options.inPreferredConfig = Bitmap.Config.RGB_565;
                    Bitmap scaledBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);

                    bitmapList.add(scaledBitmap);
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmapList;
    }

    // Callback interface to return the processed image list
    public interface FastApiCallback {
        void onImagesProcessed(List<Bitmap> bitmaps);
    }
}