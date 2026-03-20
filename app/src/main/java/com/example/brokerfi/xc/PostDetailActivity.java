package com.example.brokerfi.xc;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.adapter.PostDetailAdapter;
import com.example.brokerfi.xc.dto.CommentDTO;
import com.example.brokerfi.xc.dto.PostDTO;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {

    private RecyclerView rvDetail;
    private EditText etComment;
    private Button btnSend;

    private PostDetailAdapter adapter;
    private List<Object> dataList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_posts);

        initView();
        initRecyclerView();
        loadData();
    }

    private void initView() {
        rvDetail = findViewById(R.id.rv_post_detail);
        etComment = findViewById(R.id.et_comment);
        btnSend = findViewById(R.id.btn_send);
    }

    private void initRecyclerView() {
        rvDetail.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostDetailAdapter(this, dataList);
        rvDetail.setAdapter(adapter);
    }

    private void loadData() {
        // 接收数据（先用传参，后面换接口）
        Long postId = getIntent().getLongExtra("postId", -1);
        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");
        String username = getIntent().getStringExtra("username");

        // 构造一个 PostDTO
        PostDTO post = new PostDTO();
        post.id = postId;
        post.title = title;
        post.content = content;
        post.username = username;

        dataList.clear();
        dataList.add(post);

        // mock 评论
        for (int i = 0; i < 25; i++) {

            CommentDTO c = new CommentDTO();
            c.username = "User" + i;
            c.content = "Nice post!";
            c.time = "2026-03-20";
            c.avatarUrl = "";

            dataList.add(c);
        }

        adapter.notifyDataSetChanged();
    }
}
