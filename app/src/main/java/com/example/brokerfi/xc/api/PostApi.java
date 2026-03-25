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

//public class PostApi {
//    private static final String TAG = "PostApiUtil";
//    private static final OkHttpClient client = new OkHttpClient();
//    private static PostApi instance;
//    private final List<PostDTO> postList = new ArrayList<>();
//    private long currentId = 1;
//
//    private PostApi() {}
//
//    public static PostApi getInstance() {
//        if (instance == null) {
//            instance = new PostApi();
//        }
//        return instance;
//    }
//
//    // 获取所有帖子（模拟接口）
//    public List<PostDTO> getPosts() {
//
////        for (int i = 0; i < 10; i++) {
////            PostDTO post = new PostDTO();
////            post.setId(i);
////            post.setUsername("User " + i);
////            post.setTitle("Test Title " + i);
////            post.setContent("This is a test content for post " + i + ". It is used for preview.");
////            post.setLikeCount(i * 3);
////            post.setCommentCount(i);
////            post.setFirstImageUrl("");
////
////            postList.add(post);
////        }
//
//        Request request = new Request.Builder()
//                .url("http://127.0.0.1:56741" + "/posts")
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (response.isSuccessful()) {
//                ResponseBody body = response.body();
//                if (body != null) {
//                    String result = body.string();
//                    Log.d(TAG, "Global stats response: " + result);
//                    return result;
//                } else {
//                    Log.w(TAG, "Response body is null for global stats");
//                }
//            } else {
//                Log.w(TAG, "Failed to get global stats, response code: " + response.code());
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Network error while fetching global stats", e);
//        } catch (Exception e) {
//            Log.e(TAG, "Unexpected error while fetching global stats", e);
//        }
//
//        return postList;
//    }
//
//    // 新增帖子（模拟接口）
//    public void addPost(PostDTO post) {
//        post.setId(currentId++);
//        postList.add(0, post); // 最新的放最前
//    }
//}
