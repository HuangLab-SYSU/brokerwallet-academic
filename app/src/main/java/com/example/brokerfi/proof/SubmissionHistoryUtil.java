package com.example.brokerfi.proof;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.example.brokerfi.core.config.ApiConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Submit history tool class / 提交历史工具类
 * Handle queries on user submission history / 处理用户提交历史的查询
 */
public class SubmissionHistoryUtil {

    private static final String TAG = "SubmissionHistoryUtil";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * Get user submission history / 获取用户提交历史
     * @param walletAddress wallet address / 钱包地址
     * @param page Page number (starting from 0) / 页码（从0开始）
     * @param size page size / 每页大小
     * @param callback callback interface / 回调接口
     */
    public static void getUserSubmissions(String walletAddress, int page, int size, SubmissionHistoryCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.API_UPLOAD_USER_SUBMISSIONS +
                    "?walletAddress=" + walletAddress +
                    "&page=" + page +
                    "&size=" + size;

                Log.d(TAG, "获取用户提交历史: " + url);

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body != null) {
                        String responseStr = body.string();
                        Log.d(TAG, "获取提交历史成功: " + responseStr);
                        callback.onSuccess(responseStr);
                    } else {
                        callback.onError("Response body is empty");
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "获取提交历史失败: " + response.code() + " - " + errorBody);
                    callback.onError("Fetch failed: " + response.code() + " - " + errorBody);
                }

            } catch (IOException e) {
                Log.e(TAG, "网络请求异常", e);
                callback.onError("Network connection failed: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "获取提交历史异常", e);
                callback.onError("Fetch exception: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Get details of a single commit / 获取单个提交的详细信息
     * @param submissionId Submit ID / 提交ID
     * @param callback callback interface / 回调接口
     */
    public static void getSubmissionDetail(Long submissionId, SubmissionDetailCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.API_UPLOAD_SUBMISSION_DETAIL + "/" + submissionId;

                Log.d(TAG, "获取提交详情: " + url);

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body != null) {
                        String responseStr = body.string();
                        Log.d(TAG, "获取提交详情成功: " + responseStr);
                        callback.onSuccess(responseStr);
                    } else {
                        callback.onError("Response body is empty");
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "获取提交详情失败: " + response.code() + " - " + errorBody);
                    callback.onError("Fetch failed: " + response.code() + " - " + errorBody);
                }

            } catch (IOException e) {
                Log.e(TAG, "网络请求异常", e);
                callback.onError("Network connection failed: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "获取提交详情异常", e);
                callback.onError("Fetch exception: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Submit history callback interface / 提交历史回调接口
     */
    public interface SubmissionHistoryCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    /**
     * Submit details callback interface / 提交详情回调接口
     */
    public interface SubmissionDetailCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}
