package com.example.brokerfi.xc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.adapter.PostAdapter;
import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.xc.manager.UserManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class CommunityActivity extends AppCompatActivity {

    private RecyclerView rvPosts;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmpty;
    private FloatingActionButton fabPost;
    private ImageView profileButton;
    private PostAdapter adapter;
    private List<PostDTO> postList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        initView();
        initRecyclerView();
        initListener();

        initUser();
    }

    private void initView() {
        rvPosts = findViewById(R.id.rv_posts);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tvEmpty = findViewById(R.id.tv_empty);
        fabPost = findViewById(R.id.fab_post);
        profileButton = findViewById(R.id.profileButton);
    }

    private void initRecyclerView() {
        rvPosts.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PostAdapter(this, postList);
        rvPosts.setAdapter(adapter);

        // 跳转详情页
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
            Intent intent = new Intent(this, PostPublishActivity.class);
            startActivityForResult(intent, 1001);
        });

        // 个人主页按钮
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);

            Long userId = UserManager.getInstance().getUserId();
            String username = UserStorageUtil.getUsername(this);

            intent.putExtra("userId", userId);
            intent.putExtra("username", username);

            startActivity(intent);
        });
    }

    /**
     * 加载数据
     */
    private void loadPosts() {
        swipeRefreshLayout.setRefreshing(true);
        postList.clear();
        //接口获得帖子列表 TODO：分页？
        adapter.setData(PostApiUtil.getInstance().getPosts());
        tvEmpty.setVisibility(postList.isEmpty() ? View.VISIBLE : View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }


    private void initUser() {

        // 1. 初始化本地缓存 → 内存
        UserManager.getInstance().init(this);

        // 2. 获取钱包地址
        String address = getCurrentWalletAddress();

        if (address == null) {
            Log.e("UserInit", "wallet address is null");
            return;
        }

        // 3. 调后端登录接口
        CommunityAccountApiUtil.getInstance().login(address, user -> {

            // 4. 存用户（内存 + 本地）
            UserManager.getInstance().setUser(
                    this,
                    user.getUserId(),
                    user.getUsername(),
                    user.getAvatarUrl(),
                    user.getWalletAddress()
            );

            Log.d("UserInit", "user init success: " + user.getUserId());

            // 5. 用户准备好之后，再加载数据
            loadPosts();

        }, error -> {
            Log.e("UserInit", "login failed: " + error);
        });
    }


    /**
     * 获取当前钱包地址
     */
    private String getCurrentWalletAddress() {
        try {
            // 获取当前私钥
            String privateKey = StorageUtil.getCurrentPrivatekey(this);

            if (privateKey != null) {
                // 从私钥生成钱包地址
                return SecurityUtil.GetAddress(privateKey);
            } else {
                Log.e("WalletAddress", "Cannot get current private key");
                Toast.makeText(this, "Cannot get wallet address, please add an account first", Toast.LENGTH_SHORT).show();
                return null;
            }
        } catch (Exception e) {
            Log.e("WalletAddress", "Failed to get wallet address", e);
            Toast.makeText(this, "Failed to get wallet address: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

}