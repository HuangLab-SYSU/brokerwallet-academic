package com.example.brokerfi.xc.api;

import static com.example.brokerfi.core.config.ApiConfig.BASE_URL_HTTP;

import com.example.brokerfi.xc.dto.CommentDTO;
import com.example.brokerfi.xc.dto.LikeStatusDTO;
import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.core.network.ApiCallback;
import com.example.brokerfi.core.network.ApiResponse;
import com.example.brokerfi.core.network.BaseApi;
import com.example.brokerfi.core.network.PageResponse;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.brokerfi.core.config.ApiConfig;



public class PostApi extends BaseApi {

    // Get list of posts
    public void getPosts(ApiCallback<List<PostDTO>> callback) {

        String url = BASE_URL_HTTP + "/posts";

        Type type = new TypeToken<ApiResponse<PageResponse<PostDTO>>>() {}.getType();

        executeGet(url, type, new ApiCallback<PageResponse<PostDTO>>() {
            @Override
            public void onSuccess(PageResponse<PostDTO> data) {
                callback.onSuccess(data.getContent());
            }

            @Override
            public void onFail(String msg) {
                callback.onFail(msg);
            }
        });
    }

    // Get post details
    public void getPostDetail(Long postId, ApiCallback<PostDTO> callback) {

        String url = BASE_URL_HTTP + "/posts/" + postId;

        Type type = new TypeToken<ApiResponse<PostDTO>>() {}.getType();

        executeGet(url, type, callback);
    }

    // post
    public void addPost(PostDTO postDTO, ApiCallback<PostDTO> callback) {
        String url = BASE_URL_HTTP + "/posts";

        Type type = new TypeToken<ApiResponse<PostDTO>>() {}.getType();

        executePost(url, postDTO, type, callback);
    }

    // Get list of comments
    public void getComments(Long postId, int page, int size,
                            ApiCallback<PageResponse<CommentDTO>> callback) {

        String url = BASE_URL_HTTP + "/comments/post/"
                + postId + "?page=" + page + "&size=" + size;

        Type type = new TypeToken<ApiResponse<PageResponse<CommentDTO>>>() {}.getType();

        executeGet(url, type, callback);
    }

    // Send a comment
    public void addComment(Long postId, Long userId, String content, ApiCallback<CommentDTO> callback) {

        String url = BASE_URL_HTTP + "/comments";

        Map<String, Object> body = new HashMap<>();
        body.put("postId", postId);
        body.put("userId", userId);
        body.put("content", content);

        Type type = new TypeToken<ApiResponse<CommentDTO>>() {}.getType();

        executePost(url, body, type, callback);
    }

    // Like
    public void likePost(Long postId, Long userId, ApiCallback<LikeStatusDTO> callback) {

        String url = BASE_URL_HTTP + "/likes"
                + "?postId=" + postId
                + "&userId=" + userId;

        Type type = new TypeToken<ApiResponse<LikeStatusDTO>>() {}.getType();

        executePost(url, null, type, callback);
    }

    // Cancel like
    public void unlikePost(Long postId, Long userId, ApiCallback<LikeStatusDTO> callback) {

        String url = BASE_URL_HTTP + "/likes"
                + "?postId=" + postId
                + "&userId=" + userId;

        Type type = new TypeToken<ApiResponse<LikeStatusDTO>>() {}.getType();

        executeDelete(url, null, type, callback);
    }
}
