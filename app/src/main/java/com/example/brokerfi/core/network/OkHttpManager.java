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

    // ==================== Obtain Token uniformly ====================
    private static String getToken() {
        // Return your locally stored Token here
        // For example: SharedPreferences read
        return SharedPrefsUtil.getString("wallet_token", "");
    }

    // ==================== Add request headers to Request uniformly ====================
    private static Request addAuthHeader(Request originalRequest) {
        String token = getToken();
        if (token.isEmpty()) {
            return originalRequest; // Not logged in, do not add
        }

        // Standard format: Bearer + Token
        return originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    /* ==================== External interface ==================== */

    public static void get(String url, ApiCallback<String> callback) {
        Request original = new Request.Builder().url(url).get().build();
        Request request = addAuthHeader(original); // Automatically add header
        execute(request, callback);
    }

    public static void post(String url, String json, ApiCallback<String> callback) {
        RequestBody body = RequestBody.create(json, JSON);
        Request original = new Request.Builder().url(url).post(body).build();
        Request request = addAuthHeader(original); // Automatically add header
        execute(request, callback);
    }

    public static void delete(String url, String json, ApiCallback<String> callback) {
        RequestBody body = (json == null || json.isEmpty())
                ? RequestBody.create(new byte[0], null)
                : RequestBody.create(json, JSON);

        Request original = new Request.Builder().url(url).delete(body).build();
        Request request = addAuthHeader(original); // Automatically add header
        execute(request, callback);
    }

    /* ==================== core execution logic ==================== */

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

    /* ==================== Main thread distribution ==================== */

    private static void postSuccess(ApiCallback<String> callback, String data) {
        handler.post(() -> callback.onSuccess(data));
    }

    private static void postFail(ApiCallback<String> callback, String msg) {
        handler.post(() -> callback.onFail(msg));
    }
}
