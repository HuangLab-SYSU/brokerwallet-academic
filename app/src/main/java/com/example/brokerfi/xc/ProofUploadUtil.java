package com.example.brokerfi.xc;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

// 导入配置包中的ServerConfig
import com.example.brokerfi.config.ServerConfig;

/**
 * 证明文件上传工具类
 * 负责处理证明文件的上传、下载、删除等操作
 */
public class ProofUploadUtil {
    
    private static final String TAG = "ProofUploadUtil";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    
    /**
     * 上传证明文件（更新版：支持个人信息）
     * @param context Android上下文
     * @param fileUri 文件URI
     * @param fileName 文件名
     * @param fileType 文件类型（如：image/jpeg, application/pdf等）
     * @param walletAddress 钱包地址
     * @param displayName 用户花名（可选）
     * @param representativeWork 代表作描述（可选）
     * @param showRepresentativeWork 是否展示代表作
     * @param callback 上传结果回调
     */
    public static void uploadProofFile(Context context, Uri fileUri, String fileName, 
                                     String fileType, String walletAddress, String displayName,
                                     String representativeWork, boolean showRepresentativeWork,
                                     UploadCallback callback) {
        new Thread(() -> {
            try {
                // 从URI获取文件
                File file = getFileFromUri(context, fileUri);
                if (file == null) {
                    callback.onError("无法获取文件");
                    return;
                }
                
                // 创建请求体
                RequestBody fileBody = RequestBody.create(
                    MediaType.parse(fileType), 
                    file
                );
                
                MultipartBody.Builder requestBuilder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileName, fileBody)
                        .addFormDataPart("walletAddress", walletAddress)
                        .addFormDataPart("fileType", fileType)
                        .addFormDataPart("uploadTime", String.valueOf(System.currentTimeMillis()));
                
                // 添加个人信息参数（如果有提供的话）
                if (displayName != null && !displayName.trim().isEmpty()) {
                    requestBuilder.addFormDataPart("displayName", displayName.trim());
                }
                
                if (representativeWork != null && !representativeWork.trim().isEmpty()) {
                    requestBuilder.addFormDataPart("representativeWork", representativeWork.trim());
                }
                
                requestBuilder.addFormDataPart("showRepresentativeWork", String.valueOf(showRepresentativeWork));
                
                MultipartBody requestBody = requestBuilder.build();
                
                // 创建请求
                Request request = new Request.Builder()
                        .url(ServerConfig.UPLOAD_PROOF_API)
                        .post(requestBody)
                        .build();
                
                // 执行请求
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "上传成功: " + responseBody);
                    callback.onSuccess(responseBody);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "未知错误";
                    Log.e(TAG, "上传失败: " + response.code() + " - " + errorBody);
                    callback.onError("上传失败: " + response.code() + " - " + errorBody);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "上传异常", e);
                callback.onError("上传异常: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * 上传证明文件（兼容旧版本）
     * @param context Android上下文
     * @param fileUri 文件URI
     * @param fileName 文件名
     * @param fileType 文件类型
     * @param callback 上传结果回调
     */
    public static void uploadProofFile(Context context, Uri fileUri, String fileName, 
                                     String fileType, UploadCallback callback) {
        // 获取当前钱包地址
        String walletAddress = getCurrentWalletAddress(context);
        
        // 调用新版本方法
        uploadProofFile(context, fileUri, fileName, fileType, walletAddress, 
                       null, null, false, callback);
    }
    
    /**
     * 获取当前钱包地址
     */
    private static String getCurrentWalletAddress(Context context) {
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
        
        // 如果获取失败，返回null（让调用方处理错误）
        return null;
    }
    
    /**
     * 获取证明文件列表
     * @param callback 结果回调
     */
    public static void getProofList(ProofListCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(ServerConfig.GET_PROOF_LIST_API)
                        .get()
                        .build();
                
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "获取列表成功: " + responseBody);
                    callback.onSuccess(responseBody);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "未知错误";
                    Log.e(TAG, "获取列表失败: " + response.code() + " - " + errorBody);
                    callback.onError("获取列表失败: " + response.code() + " - " + errorBody);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "获取列表异常", e);
                callback.onError("获取列表异常: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * 删除证明文件
     * @param fileId 文件ID
     * @param callback 结果回调
     */
    public static void deleteProofFile(String fileId, DeleteCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(ServerConfig.DELETE_PROOF_API + "?fileId=" + fileId)
                        .delete()
                        .build();
                
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "删除成功: " + responseBody);
                    callback.onSuccess(responseBody);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "未知错误";
                    Log.e(TAG, "删除失败: " + response.code() + " - " + errorBody);
                    callback.onError("删除失败: " + response.code() + " - " + errorBody);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "删除异常", e);
                callback.onError("删除异常: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * 从URI获取文件
     */
    private static File getFileFromUri(Context context, Uri uri) {
        try {
            // 这里需要根据URI类型处理文件获取
            // 简化实现，实际项目中需要更复杂的处理
            return new File(uri.getPath());
        } catch (Exception e) {
            Log.e(TAG, "获取文件失败", e);
            return null;
        }
    }
    
    // ==================== 回调接口 ====================
    
    public interface UploadCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    public interface ProofListCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    public interface DeleteCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}
