package com.example.brokerfi.xc.api;

import static com.example.brokerfi.config.ServerConfig.BASE_URL_HTTP;

import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.xc.dto.ProfileHeaderDTO;
import com.example.brokerfi.xc.net.ApiCallback;
import com.example.brokerfi.xc.net.ApiResponse;
import com.example.brokerfi.xc.net.BaseApi;
import com.example.brokerfi.xc.net.PageResponse;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class ProfileApi extends BaseApi {
    public void getProfileHeader(Long userId,
                                 ApiCallback<ProfileHeaderDTO> callback) {

        String url = BASE_URL_HTTP + "/users/header/" + userId;

        Type type = new TypeToken<ApiResponse<ProfileHeaderDTO>>() {}.getType();

        executeGet(url, type, callback);
    }

    public void getUserPosts(Long userId, int page, int size,
                             ApiCallback<PageResponse<PostDTO>> callback) {

        String url = BASE_URL_HTTP + "/users/posts/"
                + userId + "?page=" + page + "&size=" + size;

        Type type = new TypeToken<ApiResponse<PageResponse<PostDTO>>>() {}.getType();

        executeGet(url, type, callback);
    }
}
