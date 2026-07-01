package com.example.brokerfi.proof;

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

// Import ServerConfig in the configuration package
import com.example.brokerfi.core.config.ApiConfig;
import com.example.brokerfi.core.security.SecurityUtil;
import com.example.brokerfi.core.storage.StorageUtil;
import com.example.brokerfi.nft.model.NFT;


/**
 * Document upload tool / 证明文件上传工具类
 * Responsible for handling operations such as uploading, downloading, and deleting certification documents. / 负责处理证明文件的上传、下载、删除等操作
 */
public class ProofUploadUtil {

    private static final String TAG = "ProofUploadUtil";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * Upload supporting documents (updated version: supports personal information) / 上传证明文件（更新版：支持个人信息）
     * @param context Android context / Android上下文
     * @param fileUri File URI / 文件URI
     * @param fileName file name / 文件名
     * @param fileType File type (such as: image/jpeg, application/pdf, etc.) / 文件类型（如：image/jpeg, application/pdf等）
     * @param walletAddress wallet address / 钱包地址
     * @param displayName Display name (optional) / 用户花名（可选）
     * @param representativeWork Representative work description (optional) / 代表作描述（可选）
     * @param showRepresentativeWork Whether to show the representative work / 是否展示代表作
     * @param callback Upload result callback / 上传结果回调
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

                // Build the request body
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

                // Add personal information parameters (if provided)
                if (displayName != null && !displayName.trim().isEmpty()) {
                    requestBuilder.addFormDataPart("displayName", displayName.trim());
                }

                if (representativeWork != null && !representativeWork.trim().isEmpty()) {
                    requestBuilder.addFormDataPart("representativeWork", representativeWork.trim());
                }

                requestBuilder.addFormDataPart("showRepresentativeWork", String.valueOf(showRepresentativeWork));

                MultipartBody requestBody = requestBuilder.build();

                // Create request
                Request request = new Request.Builder()
                        .url(ApiConfig.API_PROOF_UPLOAD)
                        .post(requestBody)
                        .build();

                // Execute request
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
     * Upload supporting documents (compatible with older versions) / 上传证明文件（兼容旧版本）
     * @param context Android context / Android上下文
     * @param fileUri File URI / 文件URI
     * @param fileName file name / 文件名
     * @param fileType File type / 文件类型
     * @param callback Upload result callback / 上传结果回调
     */
    public static void uploadProofFile(Context context, Uri fileUri, String fileName,
                                     String fileType, UploadCallback callback) {
        // Get current wallet address
        String walletAddress = getCurrentWalletAddress(context);

        // Call new version method
        uploadProofFile(context, fileUri, fileName, fileType, walletAddress,
                       null, null, false, callback);
    }

    /**
     * Get current wallet address / 获取当前钱包地址
     */
    private static String getCurrentWalletAddress(Context context) {
        try {
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;

                // Get the current private key
                String privateKey = StorageUtil.getCurrentPrivatekey((androidx.appcompat.app.AppCompatActivity) activity);

                if (privateKey != null) {
                    // Generate wallet address from private key
                    return SecurityUtil.GetAddress(privateKey);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取当前钱包地址失败", e);
        }

        // If the acquisition fails, return null (let the caller handle the error)
        return null;
    }

    /**
     * Get a list of supporting documents / 获取证明文件列表
     * @param callback result callback / 结果回调
     */
    public static void getProofList(ProofListCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(ApiConfig.API_PROOF_LIST)
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
     * Delete supporting documents / 删除证明文件
     * @param fileId File ID / 文件ID
     * @param callback result callback / 结果回调
     */
    public static void deleteProofFile(String fileId, DeleteCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(ApiConfig.API_PROOF_DELETE + "?fileId=" + fileId)
                        .delete()
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "删除成功: " + responseBody);
                    callback.onSuccess(responseBody);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "删除失败: " + response.code() + " - " + errorBody);
                    callback.onError("Delete failed: " + response.code() + " - " + errorBody);
                }

            } catch (Exception e) {
                Log.e(TAG, "删除异常", e);
                callback.onError("Delete exception: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Get file from URI / 从URI获取文件
     */
    private static File getFileFromUri(Context context, Uri uri) {
        try {
            // Here you need to process file acquisition according to URI type.
            // Simplified implementation, more complex processing is required in actual projects.
            return new File(uri.getPath());
        } catch (Exception e) {
            Log.e(TAG, "获取文件失败", e);
            return null;
        }
    }

    /**
     * Upload proof file and include user information (synchronous method, used for ProofSubmissionActivity) / 上传证明文件并包含用户信息（同步方法，用于ProofSubmissionActivity）
     * @param filePath file path / 文件路径
     * @param originalFileName original file name / 原始文件名
     * @param walletAddress wallet address / 钱包地址
     * @param displayName display name / 花名
     * @param representativeWork Representative works / 代表作
     * @param showRepresentativeWork Whether to show the representative work / 是否展示代表作
     * @return response string / 响应字符串
     */
    public static String uploadProofWithUserInfo(String filePath, String originalFileName,
                                                String walletAddress, String displayName,
                                                String representativeWork, boolean showRepresentativeWork) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new Exception("File not found: " + filePath);
        }

        // Use the original file name, or if empty use the name from the file path.
        String fileName = (originalFileName != null && !originalFileName.isEmpty()) ?
            originalFileName : file.getName();

        // Build the request body
        RequestBody fileBody = RequestBody.create(
            MediaType.parse("application/octet-stream"),
            file
        );

        MultipartBody.Builder requestBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("proofFiles", fileName, fileBody)  // Use original file name
                .addFormDataPart("walletAddress", walletAddress);

        // Add user information
        if (displayName != null && !displayName.trim().isEmpty()) {
            requestBuilder.addFormDataPart("displayName", displayName.trim());
        }

        if (representativeWork != null && !representativeWork.trim().isEmpty()) {
            requestBuilder.addFormDataPart("representativeWork", representativeWork.trim());
        }

        requestBuilder.addFormDataPart("showRepresentativeWork", String.valueOf(showRepresentativeWork));

        MultipartBody requestBody = requestBuilder.build();

        // Create request
        Request request = new Request.Builder()
                .url(ApiConfig.API_NFT_DAO_UPLOAD_COMPLETE)
                .post(requestBody)
                .build();

        // Execute request
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
     * Parse error messages and extract friendly tips / 解析错误信息，提取友好的提示
     */
    private static String parseErrorMessage(String errorBody, int statusCode) {
        try {
            // Try parsing JSON error response
            if (errorBody != null && errorBody.contains("{")) {
                org.json.JSONObject jsonError = new org.json.JSONObject(errorBody);

                // Check if it's a duplicate NFT image error
                if (jsonError.has("errorCode") && "DUPLICATE_NFT_IMAGE".equals(jsonError.optString("errorCode"))) {
                    return "NFT image uniqueness constraint: This NFT already exists, please select a different image to mint";
                }

                // Extract message field
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

    // ==================== callback interface ====================

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
