package com.example.brokerfi.xc;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
    private Button btnReward;
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

        btnSend.setOnClickListener(v -> {
            String content = etComment.getText().toString().trim();

            if (content.isEmpty()) return;

            CommentDTO comment = new CommentDTO();
            comment.username = "当前用户"; // 后面换登录用户
            comment.content = content;
            comment.time = "刚刚";

            dataList.add(comment);

            adapter.notifyItemInserted(dataList.size() - 1);
            rvDetail.scrollToPosition(dataList.size() - 1);

            etComment.setText("");
        });

        adapter.setOnPostActionListener((post, position) -> {
            showRewardDialog(post, position);
        });
    }

    private void loadData() {
        // 接收数据（先用传参，后面换接口）
        Long postId = getIntent().getLongExtra("postId", -1);
        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");
        String username = getIntent().getStringExtra("username");

        // 构造一个 PostDTO
        PostDTO post = new PostDTO();
        post.setId(postId);
        post.setTitle(title);
        post.setContent(content);
        post.setUserName(username);

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


    // 打赏弹窗
    private void showRewardDialog(PostDTO post, int position) {

        EditText input = new EditText(this);
        input.setHint("输入BKC数量");

        new AlertDialog.Builder(this)
                .setTitle("打赏")
                .setView(input)
                .setPositiveButton("确认", (dialog, which) -> {

                    String value = input.getText().toString().trim();

                    if (value.isEmpty()) {
                        Toast.makeText(this, "请输入金额", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double amount;

                    try {
                        amount = Double.parseDouble(value);
                    } catch (Exception e) {
                        Toast.makeText(this, "金额格式错误", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (amount <= 0) {
                        Toast.makeText(this, "金额必须大于0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // TODO：⭐⭐⭐ 这里改成调用后端 ⭐⭐⭐
                    //doRewardRequest(post, amount, position);

                })
                .setNegativeButton("取消", null)
                .show();
    }
}
