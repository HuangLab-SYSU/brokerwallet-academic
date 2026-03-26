package com.example.brokerfi.xc.net;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.*;

public class OkHttpManager {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /* ==================== 对外接口 ==================== */

    public static void get(String url, ApiCallback<String> callback) {
        Request request = new Request.Builder().url(url).get().build();
        execute(request, callback);
    }

    public static void post(String url, String json, ApiCallback<String> callback) {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(url).post(body).build();
        execute(request, callback);
    }

    public static void delete(String url, String json, ApiCallback<String> callback) {
        RequestBody body = (json == null || json.isEmpty())
                ? RequestBody.create(new byte[0], null)
                : RequestBody.create(json, JSON);

        Request request = new Request.Builder().url(url).delete(body).build();
        execute(request, callback);
    }

    /* ==================== 核心执行逻辑 ==================== */

    private static void execute(Request request, ApiCallback<String> callback) {

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postFail(callback, "请求失败：" + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                Response res = response;
                try {
                    if (!res.isSuccessful()) {
                        postFail(callback, "HTTP错误：" + res.code());
                        return;
                    }

                    ResponseBody body = res.body();
                    if (body == null) {
                        postFail(callback, "响应体为空");
                        return;
                    }

                    postSuccess(callback, body.string());

                } catch (Exception e) {
                    postFail(callback, "解析异常：" + e.getMessage());
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