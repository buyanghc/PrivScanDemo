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

    // 用于发送图像和 URL 到 FastAPI
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
                        // 你也可以再定义 onFailure 回调，这里简单地传空列表
                        callback.onImagesProcessed(null);
                    }
                });
            }
        });
    }

    // 上传图像和 URL 到 FastAPI
    private static byte[] uploadImage(byte[] fileBytes, String urlString) throws IOException {

        // 检查当前线程是否为主线程
        if (Looper.getMainLooper().isCurrentThread()) {
            Log.e("Network", "You are making network request on the main thread! This will cause ANR.");
        }

        // -----------------------------------------------------------
        // 改成 multipart/form-data 上传
        // -----------------------------------------------------------
        Log.d("FastApiClient", "Starting uploadImage method (multipart)...");

        // 1) 先把要上传的 Bitmap 转成 PNG 的二进制数组
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
//        byte[] fileBytes = byteArrayOutputStream.toByteArray();

        // 2) 构造 boundary（分界线）
        String boundary = "Boundary-" + System.currentTimeMillis();

        // 3) 创建 HTTP 连接
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        // 设置 multipart/form-data
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setConnectTimeout(60000);  // 连接超时
        connection.setReadTimeout(60000);     // 读取超时

        // 4) 写入 multipart 数据
        OutputStream outputStream = connection.getOutputStream();

        // -- (a) 写入 data字段 (纯文本)
        String dataPart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"data\"\r\n\r\n"
                + urlString + "\r\n";
        outputStream.write(dataPart.getBytes());

        // -- (b) 写入 file字段 (文件)
        String filePartHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"myimage.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n";
        outputStream.write(filePartHeader.getBytes());

        // 写入 fileBytes
        outputStream.write(fileBytes);
        outputStream.write("\r\n".getBytes());

        // 结束分界
        String end = "--" + boundary + "--\r\n";
        outputStream.write(end.getBytes());

        outputStream.flush();
        outputStream.close();

        // 5) 拿到响应码
        int responseCode = connection.getResponseCode();

        // 6) 读取响应体 （和以前一样：2xx用 getInputStream()，否则用 getErrorStream()）
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

    // 将输入流转换为字符串
    private static String convertStreamToString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        return response.toString();
    }

    // 解压返回的 ZIP 文件，并提取其中的图像
    private static List<Bitmap> processZipFile(byte[] zipRawBytes) {
        List<Bitmap> bitmapList = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipRawBytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().endsWith(".png")) {
                    // 1) 先把 ZIP 内该文件的字节读到内存
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zipInputStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    byte[] imageData = baos.toByteArray();

                    // 2) 第一步：只获取宽高（不真正解码像素）
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);

                    int outWidth = options.outWidth;
                    int outHeight = options.outHeight;

                    // 3) 计算合适的 inSampleSize 来缩小图像
                    //    举例：限制长宽不超过 2000 像素 (可根据需求调整)
                    int maxSide = 2000;
                    int sampleSize = 1;
                    while ((outWidth / sampleSize) > maxSide || (outHeight / sampleSize) > maxSide) {
                        sampleSize *= 2;  // 每倍增 1 次，就把解码尺寸减半
                    }

                    // 4) 第二步：真正解码（含缩放）
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = sampleSize;
                    // 如果想用 RGB_565 或其他方式节省内存，还可以：
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

    // 回调接口，用于返回处理后的图像列表
    public interface FastApiCallback {
        void onImagesProcessed(List<Bitmap> bitmaps);
    }
}
