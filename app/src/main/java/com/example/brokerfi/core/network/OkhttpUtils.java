package com.example.brokerfi.core.network;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import androidx.annotation.NonNull;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkhttpUtils {
    private OkhttpUtils() {
    }

    private static OkhttpUtils instance = new OkhttpUtils();

    public static OkhttpUtils getInstance() {
        return instance;
    }

    private OkHttpClient okHttpClient = new OkHttpClient();

    private Handler handler = new Handler(Looper.getMainLooper());


    public void doGet(String url, MyCallBack callBack) {

        // Create request object
        Request request = new Request
                .Builder()
                .url(url)
                .build();

        // Create call object
        Call call = okHttpClient.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handler.post(()->{
                    callBack.onError(e);

                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String str = null;
                try {
                    str = response.body().string();
                } catch (IOException e) {
                    handler.post(() -> {
                        callBack.onError(e);
                    });
                }
                String finalStr = str;
                handler.post(() -> {
                    callBack.onSuccess(finalStr);
                });
            }
        });

    }

    public void doPost(String url, String requestBody, MyCallBack callBack) {
        // Build the request body
        RequestBody body = RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8"));

        // Create request object
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        // Create Call object
        Call call = okHttpClient.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handler.post(() -> {
                    callBack.onError(e);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String str = null;
                try {
                    str = response.body().string();
                } catch (IOException e) {
                    handler.post(() -> {
                        callBack.onError(e);
                    });
                }
                String finalStr = str;
                handler.post(() -> {
                    callBack.onSuccess(finalStr);
                });
            }
        });
    }

}
