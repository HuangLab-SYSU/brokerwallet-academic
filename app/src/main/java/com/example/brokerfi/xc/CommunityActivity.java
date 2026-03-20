package com.example.brokerfi.xc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.adapter.PostAdapter;
import com.example.brokerfi.xc.dto.PostDTO;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class CommunityActivity extends AppCompatActivity {

    private RecyclerView rvPosts;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmpty;
    private FloatingActionButton fabPost;

    private PostAdapter adapter;
    private List<PostDTO> postList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        initView();
        initRecyclerView();
        initListener();

        loadPosts(); // 页面启动就加载数据
    }

    private void initView() {
        rvPosts = findViewById(R.id.rv_posts);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tvEmpty = findViewById(R.id.tv_empty);
        fabPost = findViewById(R.id.fab_post);
    }

    private void initRecyclerView() {
        rvPosts.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PostAdapter(this, postList);
        rvPosts.setAdapter(adapter);

        // 点击事件（跳详情页）
        adapter.setOnItemClickListener(post -> {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("postId", post.id);
            intent.putExtra("title", post.title);
            intent.putExtra("content", post.content);
            intent.putExtra("username", post.username);
            startActivity(intent);
        });
    }

    private void initListener() {

        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadPosts();
        });

        // 发帖按钮
        fabPost.setOnClickListener(v -> {
            // TODO: 跳转发帖页
        });
    }

    /**
     * 模拟加载数据（后面换成接口）
     */
    private void loadPosts() {

        swipeRefreshLayout.setRefreshing(true);

        // ===== mock数据（先跑通流程）=====
        postList.clear();

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

        adapter.notifyDataSetChanged();

        tvEmpty.setVisibility(postList.isEmpty() ? View.VISIBLE : View.GONE);

        swipeRefreshLayout.setRefreshing(false);
    }
}