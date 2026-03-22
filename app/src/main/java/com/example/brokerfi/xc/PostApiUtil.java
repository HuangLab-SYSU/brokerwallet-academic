package com.example.brokerfi.xc;

import com.example.brokerfi.xc.dto.PostDTO;

import java.util.ArrayList;
import java.util.List;

public class PostApiUtil {
    private static PostApiUtil instance;
    private final List<PostDTO> postList = new ArrayList<>();
    private long currentId = 1;

    private PostApiUtil() {}

    public static PostApiUtil getInstance() {
        if (instance == null) {
            instance = new PostApiUtil();
        }
        return instance;
    }

    // 获取所有帖子（模拟接口）
    public List<PostDTO> getPosts() {

        for (int i = 0; i < 10; i++) {
            PostDTO post = new PostDTO();
            post.id = (long) i;
            post.username = "User " + i;
            post.title = "Test Title " + i;
            post.content = "This is a test content for post " + i + ". It is used for preview.";
            post.likeCount = i * 3;
            post.commentCount = i;
            post.isRewarded = i % 2 == 0;
            post.avatarUrl = "";
            post.firstImageUrl = "";

            postList.add(post);
        }

        return postList;
    }

    // 新增帖子（模拟接口）
    public void addPost(PostDTO post) {
        post.setId(currentId++);
        postList.add(0, post); // 最新的放最前
    }
}
