package com.example.brokerfi.xc.api;

import com.example.brokerfi.xc.dto.CommentDTO;
import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.xc.net.ApiCallback;
import com.example.brokerfi.xc.net.ApiResponse;
import com.example.brokerfi.xc.net.BaseApi;
import com.example.brokerfi.xc.net.PageResponse;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PostApi extends BaseApi {

    // 获取帖子列表
    public void getPosts(ApiCallback<List<PostDTO>> callback) {

        String url = "http://10.0.2.2:5001/posts";

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

    // 获取帖子详情
    public void getPostDetail(Long postId, ApiCallback<PostDTO> callback) {

        String url = "http://10.0.2.2:5001/posts/" + postId;

        Type type = new TypeToken<ApiResponse<PostDTO>>() {}.getType();

        executeGet(url, type, callback);
    }

    //TODO:发帖

    // 获取评论列表
    public void getComments(Long postId, int page, int size,
                            ApiCallback<PageResponse<CommentDTO>> callback) {

        String url = "http://10.0.2.2:5001/comments/post/"
                + postId + "?page=" + page + "&size=" + size;

        Type type = new TypeToken<ApiResponse<PageResponse<CommentDTO>>>() {}.getType();

        executeGet(url, type, callback);
    }

    // 发送评论
    public void addComment(Long postId, Long userId, String content, ApiCallback<CommentDTO> callback) {

        String url = "http://10.0.2.2:5001/comments";

        Map<String, Object> body = new HashMap<>();
        body.put("postId", postId);
        body.put("userId", userId);
        body.put("content", content);

        Type type = new TypeToken<ApiResponse<CommentDTO>>() {}.getType();

        executePost(url, body, type, callback);
    }
}
