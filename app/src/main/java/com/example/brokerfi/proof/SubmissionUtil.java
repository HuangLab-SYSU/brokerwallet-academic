package com.example.brokerfi.proof;

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

import com.example.brokerfi.core.config.ApiConfig;
import com.example.brokerfi.core.security.SecurityUtil;
import com.example.brokerfi.core.storage.StorageUtil;


/**
 * Integrated submission utility / 一体化提交工具类
 * Handles one-time submission of proof files, NFT images, and user information. / 处理证明文件、NFT图片、用户信息的一次性提交
 */
public class SubmissionUtil {

    private static final String TAG = "SubmissionUtil";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * Integrated submission: multiple supporting documents + NFT images + user information. / 一体化提交：多个证明文件 + NFT图片 + 用户信息
     * @param context Android context / Android上下文
     * @param proofFileUris List of supporting document URIs (required, 1-3) / 证明文件URI列表（必填，1-3个）
     * @param nftImageUri NFT image URI (optional) / NFT图片URI（可选）
     * @param walletAddress Wallet address (obtained automatically) / 钱包地址（自动获取）
     * @param displayName Display name (optional) / 用户花名（可选）
     * @param representativeWork Representative work description (optional) / 代表作描述（可选）
     * @param showRepresentativeWork Whether to show the representative work / 是否展示代表作
     * @param callback Submission result callback / 提交结果回调
     */
    public static void submitComplete(Context context, List<Uri> proofFileUris, Uri nftImageUri,
                                    String walletAddress, String displayName, String representativeWork,
                                    boolean showRepresentativeWork, SubmissionCallback callback) {

        new Thread(() -> {
            try {
                // Build the request body
                MultipartBody.Builder requestBuilder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM);

                // 1. Add all supporting documents (required)
                if (proofFileUris == null || proofFileUris.isEmpty()) {
                    callback.onError("Please select at least one proof file");
                    return;
                }

                for (int i = 0; i < proofFileUris.size(); i++) {
                    Uri proofFileUri = proofFileUris.get(i);

                    // Get the original file name
                    String originalFileName = getFileNameFromUri(context, proofFileUri);
                    if (originalFileName == null || originalFileName.isEmpty()) {
                        originalFileName = "proof_file_" + (i + 1) + ".dat";
                    }

                    File proofFile = getFileFromUri(context, proofFileUri);
                    if (proofFile == null) {
                        callback.onError("Unable to get proof file " + (i + 1));
                        return;
                    }

                    RequestBody proofFileBody = RequestBody.create(
                        MediaType.parse("application/octet-stream"),
                        proofFile
                    );
                    // Use original filename instead of temporary filename
                    requestBuilder.addFormDataPart("proofFiles", originalFileName, proofFileBody);
                    Log.d(TAG, "添加证明文件: " + originalFileName + " (大小: " + proofFile.length() + " bytes)");
                }

                // 2. Add NFT image (optional)
                if (nftImageUri != null) {
                    // Get the original image file name
                    String originalImageName = getFileNameFromUri(context, nftImageUri);
                    if (originalImageName == null || originalImageName.isEmpty()) {
                        originalImageName = "nft_image.jpg";
                    }

                    File nftImageFile = getFileFromUri(context, nftImageUri);
                    if (nftImageFile != null) {
                        RequestBody nftImageBody = RequestBody.create(
                            MediaType.parse("image/*"),
                            nftImageFile
                        );
                        // Use original image file name
                        requestBuilder.addFormDataPart("nftImage", originalImageName, nftImageBody);
                        Log.d(TAG, "添加NFT图片: " + originalImageName + " (大小: " + nftImageFile.length() + " bytes)");
                    }
                }

                // 3. Add wallet address (required)
                requestBuilder.addFormDataPart("walletAddress", walletAddress);

                // 4. Add user information (optional)
                if (displayName != null && !displayName.trim().isEmpty()) {
                    requestBuilder.addFormDataPart("displayName", displayName.trim());
                }

                if (representativeWork != null && !representativeWork.trim().isEmpty()) {
                    requestBuilder.addFormDataPart("representativeWork", representativeWork.trim());
                }

                requestBuilder.addFormDataPart("showRepresentativeWork", String.valueOf(showRepresentativeWork));

                // 5. Build request
                MultipartBody requestBody = requestBuilder.build();

                Request request = new Request.Builder()
                        .url(ApiConfig.API_UPLOAD_COMPLETE)
                        .post(requestBody)
                        .build();

                // 6. Execute the request
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "一体化提交成功: " + responseBody);
                    callback.onSuccess(responseBody);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "一体化提交失败: " + response.code() + " - " + errorBody);
                    callback.onError("Submission failed: " + response.code() + " - " + errorBody);
                }

            } catch (Exception e) {
                Log.e(TAG, "一体化提交异常", e);
                callback.onError("Submission exception: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Get a file from a URI (compatible with Android 10+ scoped storage)
     */
    private static File getFileFromUri(Context context, Uri uri) {
        try {
            Log.d(TAG, "处理URI: " + uri.toString());

            // Get original file name and extension
            String originalFileName = getFileNameFromUri(context, uri);
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            // Create temporary file (keep extension)
            String fileName = "temp_" + System.currentTimeMillis() + extension;
            File tempFile = new File(context.getCacheDir(), fileName);

            // Read content from URI to temporary file
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
     * Get original filename from URI / 从URI获取原始文件名
     */
    private static String getFileNameFromUri(Context context, Uri uri) {
        String fileName = null;

        if ("content".equals(uri.getScheme())) {
            // For content:// type URI, query the file name.
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
            // For file:// type URI, get the file name directly from the path.
            fileName = new File(uri.getPath()).getName();
        }

        Log.d(TAG, "获取到的文件名: " + fileName);
        return fileName;
    }

    /**
     * Get current wallet address / 获取当前钱包地址
     */
    public static String getCurrentWalletAddress(Context context) {
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

        return null;
    }

    // ==================== callback interface ====================

    public interface SubmissionCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}
