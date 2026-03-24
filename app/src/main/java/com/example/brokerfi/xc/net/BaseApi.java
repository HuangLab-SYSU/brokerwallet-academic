package com.example.brokerfi.xc.net;

import java.lang.reflect.Type;

public abstract class BaseApi {

    protected <T> void executeGet(String url, Type type, ApiCallback<T> callback) {

        OkHttpManager.get(url, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {

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

            @Override
            public void onFail(String msg) {
                callback.onFail(msg);
            }
        });
    }
}