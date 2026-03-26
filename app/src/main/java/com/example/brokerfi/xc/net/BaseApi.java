package com.example.brokerfi.xc.net;

import java.lang.reflect.Type;

public abstract class BaseApi {

    protected <T> void executeGet(String url, Type type, ApiCallback<T> callback) {

        OkHttpManager.get(url, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                handleResponse(result, type, callback);
            }

            @Override
            public void onFail(String msg) {
                callback.onFail(msg);
            }
        });
    }

    protected <T> void executePost(String url, Object body, Type type, ApiCallback<T> callback) {

        String json = GsonConverter.toJson(body);

        OkHttpManager.post(url, json, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                handleResponse(result, type, callback);
            }

            @Override
            public void onFail(String msg) {
                callback.onFail(msg);
            }
        });
    }


    protected <T> void executeDelete(String url, Object body, Type type, ApiCallback<T> callback) {
        String json = null;
        if (body != null) {
            json = GsonConverter.toJson(body);
        }

        OkHttpManager.delete(url, json, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                handleResponse(result, type, callback);
            }

            @Override
            public void onFail(String msg) {
                callback.onFail(msg);
            }
        });
    }

    private <T> void handleResponse(String result, Type type, ApiCallback<T> callback) {
        try {
            ApiResponse<T> response =
                    GsonConverter.fromJson(result, type);

            if (response.getCode() == 0) {
                callback.onSuccess(response.getData());
            } else {
                callback.onFail(response.getMessage());
            }

        } catch (Exception e) {
            callback.onFail("解析错误");
        }
    }
}