package com.example.brokerfi.xc.api;

import static com.example.brokerfi.core.config.ApiConfig.BASE_URL_HTTP;

import com.example.brokerfi.xc.dto.UserAccountDTO;
import com.example.brokerfi.core.network.ApiCallback;
import com.example.brokerfi.core.network.ApiResponse;
import com.example.brokerfi.core.network.BaseApi;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import com.example.brokerfi.core.config.ApiConfig;


public class UserApi extends BaseApi {
    // Get nonce
    public void getNonce(String walletAddress, ApiCallback<String> callback) {

        String url = BASE_URL_HTTP + "/login/nonce?walletAddress=" + walletAddress;

        Type type = new TypeToken<ApiResponse<String>>() {}.getType();

        executeGet(url, type, callback);
    }

    // Signed login (return token)
    public void login(Map<String, String> body, ApiCallback<UserAccountDTO> callback) {

        String url = BASE_URL_HTTP + "/login/sign";

        Type type = new TypeToken<ApiResponse<UserAccountDTO>>() {}.getType();

        executePost(url, body, type, callback);
    }
}
