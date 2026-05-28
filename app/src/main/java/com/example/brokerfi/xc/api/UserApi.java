package com.example.brokerfi.xc.api;

import static com.example.brokerfi.config.ApiConfig.BASE_URL_HTTP;

import com.example.brokerfi.xc.dto.UserAccountDTO;
import com.example.brokerfi.xc.net.ApiCallback;
import com.example.brokerfi.xc.net.ApiResponse;
import com.example.brokerfi.xc.net.BaseApi;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class UserApi extends BaseApi {
    //获取 nonce
    public void getNonce(String walletAddress, ApiCallback<String> callback) {

        String url = BASE_URL_HTTP + "/login/nonce?walletAddress=" + walletAddress;

        Type type = new TypeToken<ApiResponse<String>>() {}.getType();

        executeGet(url, type, callback);
    }

    //签名登录（返回 token）
    public void login(Map<String, String> body, ApiCallback<UserAccountDTO> callback) {

        String url = BASE_URL_HTTP + "/login/sign";

        Type type = new TypeToken<ApiResponse<UserAccountDTO>>() {}.getType();

        executePost(url, body, type, callback);
    }
}
