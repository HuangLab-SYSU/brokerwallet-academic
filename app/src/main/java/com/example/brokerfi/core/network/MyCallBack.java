package com.example.brokerfi.core.network;

public interface MyCallBack {
    void onSuccess(String result);

    Void onError(Exception e);
}
