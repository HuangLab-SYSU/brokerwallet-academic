package com.example.brokerfi.xc.api;

import static com.example.brokerfi.core.config.ApiConfig.BASE_URL_HTTP;

import com.example.brokerfi.xc.dto.CosCredentialDTO;
import com.example.brokerfi.core.network.ApiCallback;
import com.example.brokerfi.core.network.ApiResponse;
import com.example.brokerfi.core.network.BaseApi;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import com.example.brokerfi.core.config.ApiConfig;



public class CosApi extends BaseApi {

    public void getTempCredential(ApiCallback<CosCredentialDTO> callback) {

        String url = BASE_URL_HTTP + "/cos/temp-credential";

        Type type = new TypeToken<ApiResponse<CosCredentialDTO>>() {}.getType();

        executeGet(url, type, callback);
    }
}
