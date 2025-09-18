package com.example.brokerfi.xc;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.example.brokerfi.config.ServerConfig;

/**
 * 一体化提交工具类
 * 处理证明文件、NFT图片、用户信息的一次性提交
 */
public class SubmissionUtil {
    
    private static final String TAG = "SubmissionUtil";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    
    /**
     * 一体化提交：多个证明文件 + NFT图片 + 用户信息
     * @param context Android上下文
     * @param proofFileUris 证明文件URI列表（必填，1-3个）
     * @param nftImageUri NFT图片URI（可选）
     * @param walletAddress 钱包地址（自动获取）
     * @param displayName 用户花名（可选）
     * @param representativeWork 代表作描述（可选）
     * @param showRepresentativeWork 是否展示代表作
     * @param callback 提交结果回调
     */
    public static void submitComplete(Context context, List<Uri> proofFileUris, Uri nftImageUri,
                                    String walletAddress, String displayName, String representativeWork,
                                    boolean showRepresentativeWork, SubmissionCallback callback) {
        
        new Thread(() -> {
            try {
                // 构建请求体
                MultipartBody.Builder requestBuilder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM);
                
                // 1. 添加所有证明文件（必填）
                if (proofFileUris == null || proofFileUris.isEmpty()) {
                    callback.onError("请选择至少一个证明文件");
                    return;
                }
                
                for (int i = 0; i < proofFileUris.size(); i++) {
                    Uri proofFileUri = proofFileUris.get(i);
                    File proofFile = getFileFromUri(context, proofFileUri);
                    if (proofFile == null) {
                        callback.onError("无法获取证明文件 " + (i + 1));
                        return;
                    }
                    
                    RequestBody proofFileBody = RequestBody.create(
                        MediaType.parse("application/octet-stream"), 
                        proofFile
                    );
                    requestBuilder.addFormDataPart("proofFiles", proofFile.getName(), proofFileBody);
                }
                
                // 2. 添加NFT图片（可选）
                if (nftImageUri != null) {
                    File nftImageFile = getFileFromUri(context, nftImageUri);
                    if (nftImageFile != null) {
                        RequestBody nftImageBody = RequestBody.create(
                            MediaType.parse("image/*"), 
                            nftImageFile
                        );
                        requestBuilder.addFormDataPart("nftImage", nftImageFile.getName(), nftImageBody);
                    }
                }
                
                // 3. 添加钱包地址（必填）
                requestBuilder.addFormDataPart("walletAddress", walletAddress);
                
                // 4. 添加用户信息（可选）
                if (displayName != null && !displayName.trim().isEmpty()) {
                    requestBuilder.addFormDataPart("displayName", displayName.trim());
                }
                
                if (representativeWork != null && !representativeWork.trim().isEmpty()) {
                    requestBuilder.addFormDataPart("representativeWork", representativeWork.trim());
                }
                
                requestBuilder.addFormDataPart("showRepresentativeWork", String.valueOf(showRepresentativeWork));
                
                // 5. 构建请求
                MultipartBody requestBody = requestBuilder.build();
                
                Request request = new Request.Builder()
                        .url(ServerConfig.BASE_URL + "/api/upload/complete")
                        .post(requestBody)
                        .build();
                
                // 6. 执行请求
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "一体化提交成功: " + responseBody);
                    callback.onSuccess(responseBody);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "未知错误";
                    Log.e(TAG, "一体化提交失败: " + response.code() + " - " + errorBody);
                    callback.onError("提交失败: " + response.code() + " - " + errorBody);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "一体化提交异常", e);
                callback.onError("提交异常: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * 从URI获取文件（兼容Android 10+分区存储）
     */
    private static File getFileFromUri(Context context, Uri uri) {
        try {
            Log.d(TAG, "处理URI: " + uri.toString());
            
            // 获取原始文件名和扩展名
            String originalFileName = getFileNameFromUri(context, uri);
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            
            // 创建临时文件（保留扩展名）
            String fileName = "temp_" + System.currentTimeMillis() + extension;
            File tempFile = new File(context.getCacheDir(), fileName);
            
            // 从URI读取内容到临时文件
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "无法打开输入流");
                return null;
            }
            
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            inputStream.close();
            outputStream.close();
            
            Log.d(TAG, "临时文件创建成功: " + tempFile.getAbsolutePath() + " (大小: " + tempFile.length() + " bytes)");
            return tempFile;
            
        } catch (Exception e) {
            Log.e(TAG, "获取文件失败", e);
            return null;
        }
    }
    
    /**
     * 从URI获取原始文件名
     */
    private static String getFileNameFromUri(Context context, Uri uri) {
        String fileName = null;
        
        if ("content".equals(uri.getScheme())) {
            // 对于content://类型的URI，查询文件名
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } else if ("file".equals(uri.getScheme())) {
            // 对于file://类型的URI，直接从路径获取文件名
            fileName = new File(uri.getPath()).getName();
        }
        
        Log.d(TAG, "获取到的文件名: " + fileName);
        return fileName;
    }
    
    /**
     * 获取当前钱包地址
     */
    public static String getCurrentWalletAddress(Context context) {
        try {
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                
                // 获取当前私钥
                String privateKey = StorageUtil.getCurrentPrivatekey((androidx.appcompat.app.AppCompatActivity) activity);
                
                if (privateKey != null) {
                    // 从私钥生成钱包地址
                    return SecurityUtil.GetAddress(privateKey);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取当前钱包地址失败", e);
        }
        
        return null;
    }
    
    // ==================== 回调接口 ====================
    
    public interface SubmissionCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}
