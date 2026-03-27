package com.example.brokerfi.xc.api;

import com.example.brokerfi.xc.dto.UserAccountDTO;
import com.example.brokerfi.xc.net.ApiCallback;
import com.example.brokerfi.xc.net.ApiResponse;
import com.example.brokerfi.xc.net.BaseApi;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class UserApi extends BaseApi {
    public void login(String walletAddress, ApiCallback<UserAccountDTO> callback) {

        String url = "http://172.27.71.58:5001/api/users/login";

        Map<String, String> body = new HashMap<>();
        body.put("walletAddress", walletAddress);

        Type type = new TypeToken<ApiResponse<UserAccountDTO>>() {}.getType();

        executePost(url, body, type, callback);
    }
}
