package com.example.brokerfi.core.network;

public interface ApiCallback<T> {
    void onSuccess(T data);
    void onFail(String msg);
}
