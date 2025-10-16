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
                // Get file from URI
                File file = getFileFromUri(context, fileUri);
                if (file == null) {
                    callback.onError("Cannot get file");
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
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Upload failed: " + response.code() + " - " + errorBody);
                    
                    // Parse error message to extract friendly hints
                    String friendlyError = parseErrorMessage(errorBody, response.code());
                    callback.onError(friendlyError);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Upload exception", e);
                callback.onError("Upload exception: " + e.getMessage());
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
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Get list failed: " + response.code() + " - " + errorBody);
                    callback.onError("Get list failed: " + response.code() + " - " + errorBody);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Get list exception", e);
                callback.onError("Get list exception: " + e.getMessage());
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
    
    /**
     * 上传证明文件并包含用户信息（同步方法，用于ProofSubmissionActivity）
     * @param filePath 文件路径
     * @param originalFileName 原始文件名
     * @param walletAddress 钱包地址
     * @param displayName 花名
     * @param representativeWork 代表作
     * @param showRepresentativeWork 是否展示代表作
     * @return 响应字符串
     */
    public static String uploadProofWithUserInfo(String filePath, String originalFileName, 
                                                String walletAddress, String displayName, 
                                                String representativeWork, boolean showRepresentativeWork) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new Exception("File not found: " + filePath);
        }
        
        // 使用原始文件名，如果为空则使用文件路径中的名称
        String fileName = (originalFileName != null && !originalFileName.isEmpty()) ? 
            originalFileName : file.getName();
        
        // 创建请求体
        RequestBody fileBody = RequestBody.create(
            MediaType.parse("application/octet-stream"), 
            file
        );
        
        MultipartBody.Builder requestBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("proofFiles", fileName, fileBody)  // 使用原始文件名
                .addFormDataPart("walletAddress", walletAddress);
        
        // 添加用户信息
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
                .url("http://academic.broker-chain.com:5000/api/upload/complete")
                .post(requestBody)
                .build();
        
        // 执行请求
        Response response = client.newCall(request).execute();
        
        if (response.isSuccessful()) {
            String responseBody = response.body().string();
            Log.d(TAG, "Upload successful: " + responseBody);
            return responseBody;
        } else {
            String errorBody = response.body() != null ? response.body().string() : "Unknown error";
            Log.e(TAG, "Upload failed: " + response.code() + " - " + errorBody);
            
            // Parse error message to extract friendly hints
            String friendlyError = parseErrorMessage(errorBody, response.code());
            throw new Exception(friendlyError);
        }
    }
    
    /**
     * 解析错误信息，提取友好的提示
     */
    private static String parseErrorMessage(String errorBody, int statusCode) {
        try {
            // 尝试解析JSON错误响应
            if (errorBody != null && errorBody.contains("{")) {
                org.json.JSONObject jsonError = new org.json.JSONObject(errorBody);
                
                // Check if it's a duplicate NFT image error
                if (jsonError.has("errorCode") && "DUPLICATE_NFT_IMAGE".equals(jsonError.optString("errorCode"))) {
                    return "NFT image uniqueness constraint: This NFT already exists, please select a different image to mint";
                }
                
                // 提取message字段
                if (jsonError.has("message")) {
                    String message = jsonError.optString("message");
                    if (message != null && !message.isEmpty()) {
                        return message;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析错误信息失败", e);
        }
        
        // If unable to parse, return generic error message
        if (statusCode == 400) {
            return "Upload failed, please check file format and content";
        } else if (statusCode == 500) {
            return "Server error, please try again later";
        } else {
            return "Upload failed (Error code: " + statusCode + ")";
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
