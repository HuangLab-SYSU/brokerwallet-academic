package com.example.brokerfi.xc.net;

public interface MyCallBack {
    void onSuccess(String result);

    Void onError(Exception e);
}
