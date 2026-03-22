package com.example.brokerfi.xc;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.adapter.ProfileAdapter;
import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.xc.dto.ProfileHeaderDTO;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private RecyclerView rvProfile;
    private ProfileAdapter adapter;
    private List<Object> dataList = new ArrayList<>();

    private Long userId;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_profile);

        rvProfile = findViewById(R.id.rv_profile);

        userId = getIntent().getLongExtra("userId", -1);
        username = getIntent().getStringExtra("username");

        rvProfile.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProfileAdapter(this, dataList);
        rvProfile.setAdapter(adapter);

        adapter.setOnPostClickListener(post -> {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("postId", post.id);
            intent.putExtra("title", post.title);
            intent.putExtra("content", post.content);
            intent.putExtra("username", post.username);
            startActivity(intent);
        });

        mockData(); // ⭐ 模拟数据
    }

    private void mockData() {

        // 1️⃣ Header
        ProfileHeaderDTO header = new ProfileHeaderDTO();
        header.username = username;
        header.postCount = 3;
        header.rewardTotal = 100;

        dataList.add(header);

        // 2️⃣ 帖子
        for (int i = 0; i < 3; i++) {
            PostDTO post = new PostDTO();
            post.title = "我的帖子 " + i;
            post.content = "这是内容 " + i;
            post.username = username;
            post.likeCount = i * 5;

            dataList.add(post);
        }

        adapter.notifyDataSetChanged();
    }
}
