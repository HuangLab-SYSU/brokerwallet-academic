package com.example.brokerfi.core.network;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.*;
import com.example.brokerfi.core.storage.SharedPrefsUtil;


public class OkHttpManager {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // ==================== 统一获取 Token ====================
    private static String getToken() {
        // 这里返回你本地存储的 Token
        // 例如：SharedPreferences 读取
        return SharedPrefsUtil.getString("wallet_token", "");
    }

    // ==================== 给 Request 统一添加请求头 ====================
    private static Request addAuthHeader(Request originalRequest) {
        String token = getToken();
        if (token.isEmpty()) {
            return originalRequest; // 未登录，不添加
        }

        // 标准格式：Bearer + Token
        return originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    /* ==================== 对外接口 ==================== */

    public static void get(String url, ApiCallback<String> callback) {
        Request original = new Request.Builder().url(url).get().build();
        Request request = addAuthHeader(original); // 自动加头
        execute(request, callback);
    }

    public static void post(String url, String json, ApiCallback<String> callback) {
        RequestBody body = RequestBody.create(json, JSON);
        Request original = new Request.Builder().url(url).post(body).build();
        Request request = addAuthHeader(original); // 自动加头
        execute(request, callback);
    }

    public static void delete(String url, String json, ApiCallback<String> callback) {
        RequestBody body = (json == null || json.isEmpty())
                ? RequestBody.create(new byte[0], null)
                : RequestBody.create(json, JSON);

        Request original = new Request.Builder().url(url).delete(body).build();
        Request request = addAuthHeader(original); // 自动加头
        execute(request, callback);
    }

    /* ==================== 核心执行逻辑 ==================== */

    private static void execute(Request request, ApiCallback<String> callback) {

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postFail(callback, "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                Response res = response;
                try {
                    if (!res.isSuccessful()) {
                        postFail(callback, "HTTP error: " + res.code());
                        return;
                    }

                    ResponseBody body = res.body();
                    if (body == null) {
                        postFail(callback, "Response body is empty");
                        return;
                    }

                    postSuccess(callback, body.string());

                } catch (Exception e) {
                    postFail(callback, "Parse exception: " + e.getMessage());
                } finally {
                    res.close();
                }
            }
        });
    }

    /* ==================== 主线程分发 ==================== */

    private static void postSuccess(ApiCallback<String> callback, String data) {
        handler.post(() -> callback.onSuccess(data));
    }

    private static void postFail(ApiCallback<String> callback, String msg) {
        handler.post(() -> callback.onFail(msg));
    }
}
