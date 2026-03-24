package com.example.brokerfi.xc.net;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpManager {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static void get(String url, ApiCallback<String> callback) {

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handler.post(() -> callback.onFail(e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                String result = response.body().string();

                handler.post(() -> callback.onSuccess(result));
            }
        });
    }

    public static void post(String url, String json, ApiCallback<String> callback) {

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFail(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body().string());
                } else {
                    callback.onFail("网络错误");
                }
            }
        });
    }
}
