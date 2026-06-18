package com.example.brokerfi.xc;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.adapter.ProfileAdapter;
import com.example.brokerfi.xc.api.ProfileApi;
import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.xc.dto.ProfileHeaderDTO;
import com.example.brokerfi.main.menu.NavigationHelper;
import com.example.brokerfi.core.network.ApiCallback;
import com.example.brokerfi.core.network.PageResponse;

import java.util.ArrayList;
import java.util.List;
import com.example.brokerfi.main.MainActivity;


public class ProfileActivity extends AppCompatActivity {

    private RecyclerView rvProfile;
    private ProfileAdapter adapter;
    private List<Object> dataList = new ArrayList<>();

    private Long userId;
    private String username;
    private int page = 0;
    private final int size = 10;
    private boolean isLoading = false;

    // menu 相关控件
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout actionBar;
    private NavigationHelper navigationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_profile);

        initView();
        initMenu();
        initData();
        initEvent();

        loadHeader();     // 先加载头部
    }

    private void initView() {
        rvProfile = findViewById(R.id.rv_profile);

        // 绑定 topbar 中的 menu 控件
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        actionBar = findViewById(R.id.action_bar);

        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(ProfileActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initMenu() {
        // 初始化原 wallet 的顶部 menu 弹窗逻辑
        navigationHelper = new NavigationHelper(menu, actionBar, this, notificationBtn);
    }

    private void initData() {
        userId = getIntent().getLongExtra("userId", -1);
        username = getIntent().getStringExtra("userName");

        rvProfile.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProfileAdapter(this, dataList);
        rvProfile.setAdapter(adapter);
    }

    private void initEvent() {
        adapter.setOnPostClickListener(post -> {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("postId", post.getId());
            intent.putExtra("title", post.getTitle());
            intent.putExtra("content", post.getContent());
            intent.putExtra("userName", post.getUserName());
            startActivity(intent);
        });

        adapter.setOnEditClickListener(() -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("userName", username);
            startActivity(intent);
        });
    }

    private void loadHeader() {
        new ProfileApi().getProfileHeader(userId,
                new ApiCallback<ProfileHeaderDTO>() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onSuccess(ProfileHeaderDTO header) {
                        dataList.clear();
                        dataList.add(header);
                        adapter.notifyDataSetChanged();

                        loadPosts(true);
                    }

                    @Override
                    public void onFail(String errorMsg) {
                        isLoading = false;
                    }
                });
    }

    private void loadPosts(boolean isRefresh) {

        if (isLoading) return;
        isLoading = true;

        if (isRefresh) {
            page = 0;
        }

        new ProfileApi().getUserPosts(userId, page, size,
                new ApiCallback<PageResponse<PostDTO>>() {
                    @Override
                    public void onSuccess(PageResponse<PostDTO> response) {
                        List<PostDTO> list = response.getContent();

                        if (isRefresh) {
                            // 注意：header 在 index=0
                            if (dataList.size() > 1) {
                                dataList.subList(1, dataList.size()).clear();
                            }
                        }

                        int start = dataList.size();
                        dataList.addAll(list);
                        adapter.notifyItemRangeInserted(start, list.size());

                        page++;
                        isLoading = false;

                        Log.d("Profile", "dataList size=" + dataList.size());
                    }

                    @Override
                    public void onFail(String errorMsg) {
                        isLoading = false;
                    }
                });
    }
}
