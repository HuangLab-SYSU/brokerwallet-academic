package com.example.brokerfi.xc.net;

public interface ApiCallback<T> {
    void onSuccess(T data);
    void onFail(String msg);
}
