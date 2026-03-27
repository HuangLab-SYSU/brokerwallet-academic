package com.example.brokerfi.xc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.adapter.ProfileAdapter;
import com.example.brokerfi.xc.api.PostApi;
import com.example.brokerfi.xc.api.ProfileApi;
import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.xc.dto.ProfileHeaderDTO;
import com.example.brokerfi.xc.net.ApiCallback;
import com.example.brokerfi.xc.net.PageResponse;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private RecyclerView rvProfile;
    private ProfileAdapter adapter;
    private List<Object> dataList = new ArrayList<>();
    private Long userId;
    private String username;
    private int page = 0;
    private final int size = 10;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_profile);

        rvProfile = findViewById(R.id.rv_profile);

        userId = getIntent().getLongExtra("userId", -1);
        username = getIntent().getStringExtra("userName");

        rvProfile.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProfileAdapter(this, dataList);
        rvProfile.setAdapter(adapter);

        adapter.setOnPostClickListener(post -> {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("postId", post.getId());
            intent.putExtra("title", post.getTitle());
            intent.putExtra("content", post.getContent());
            intent.putExtra("userName", post.getUserName());
            startActivity(intent);
        });

        loadHeader();     //先加载头部
        loadPosts(true);  //加载第一页帖子
    }

    private void loadHeader() {
        new ProfileApi().getProfileHeader(userId,
                new ApiCallback<ProfileHeaderDTO>() {
                    @Override
                    public void onSuccess(ProfileHeaderDTO header) {
                        dataList.clear();
                        dataList.add(header);
                        adapter.notifyDataSetChanged();

                        loadPosts(true);
                    }
                    @Override
                    public void onFail(String errorMsg) {

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
                            // ⚠️ 注意：header 在 index=0
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
