package com.example.brokerfi.xc.manager;

import java.util.List;

public interface UploadCallback {
    void onSuccess(List<String> urls);
    void onFail(String error);
}
