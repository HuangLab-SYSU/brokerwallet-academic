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
import com.example.brokerfi.xc.api.PostApi;
import com.example.brokerfi.xc.api.UserApi;
import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.xc.dto.UserAccountDTO;
import com.example.brokerfi.xc.manager.UserManager;
import com.example.brokerfi.xc.net.ApiCallback;
import com.example.brokerfi.xc.net.SharedPrefsUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.widget.RelativeLayout;
import com.example.brokerfi.xc.menu.NavigationHelper;

public class CommunityActivity extends AppCompatActivity {

    private RecyclerView rvPosts;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmpty;
    private FloatingActionButton fabPost;
    private ImageView profileButton;
    private PostAdapter adapter;
    private List<PostDTO> postList = new ArrayList<>();
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout actionBar;
    private NavigationHelper navigationHelper;

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
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        actionBar = findViewById(R.id.action_bar);

        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(CommunityActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initRecyclerView() {
        rvPosts.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PostAdapter(this, postList);
        rvPosts.setAdapter(adapter);

        // 跳转详情页
        adapter.setOnItemClickListener(post -> {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("postId", post.getId());
            intent.putExtra("title", post.getTitle());
            intent.putExtra("content", post.getContent());
            intent.putExtra("userName", post.getUserName());
            startActivity(intent);
        });
    }

    private void initListener() {
        navigationHelper = new NavigationHelper(menu, actionBar, this, notificationBtn);

        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadPosts();
        });

        // 发帖按钮
        fabPost.setOnClickListener(v -> {
            Intent intent = new Intent(this, PostPublishActivity.class);
            startActivityForResult(intent, 1001);
        });

        // 个人主页
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);

            Long userId = UserManager.getInstance().getUserId();
            String username = UserStorageUtil.getUsername(this);
            intent.putExtra("userId", userId);
            intent.putExtra("userName", username);

            startActivity(intent);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 来自发帖页面的返回
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            // 拿到刚发布的帖子
            PostDTO newPost = (PostDTO) data.getSerializableExtra("newPost");
            if (newPost != null) {
                postList.add(0, newPost);
                adapter.notifyItemInserted(0);
                rvPosts.scrollToPosition(0);
            }
        }
    }

    /**
     * 加载数据
     */
    private void loadPosts() {

        swipeRefreshLayout.setRefreshing(true);

        new PostApi().getPosts(new ApiCallback<List<PostDTO>>() {
            @Override
            public void onSuccess(List<PostDTO> data) {
                adapter.setData(data);
                tvEmpty.setVisibility(postList.isEmpty() ? View.VISIBLE : View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFail(String msg) {
                Log.e("loadPosts", "error: " + msg);
                Toast.makeText(CommunityActivity.this, msg, Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }


    private void initUser() {

        UserManager.getInstance().init(this);
        String address = getCurrentWalletAddress();

        if (address == null) {
            Log.e("UserInit", "wallet address is null");
            return;
        }

        UserApi api = new UserApi();

        // ================== 1. 获取 nonce ==================
        api.getNonce(address, new ApiCallback<String>() {
            @Override
            public void onSuccess(String nonce) {

                try {
                    // ================== 2. 获取私钥 ==================
                    String account = StorageUtil.getPrivateKey(CommunityActivity.this);
                    String acc = StorageUtil.getCurrentAccount(CommunityActivity.this);
                    int i = (acc == null) ? 0 : Integer.parseInt(acc);

                    if (account == null) {
                        Log.e("UserInit", "no account");
                        return;
                    }

                    String privateKey = account.split(";")[i];

                    // ================== 3. 签名 ==================
                    Map<String, String> signMap =
                            SecurityUtil.signMessage(privateKey, nonce);

                    // ================== 4. 组装请求 ==================
                    Map<String, String> body = new HashMap<>();
                    body.put("walletAddress", address);
                    body.put("r", signMap.get("r"));
                    body.put("s", signMap.get("s"));
                    body.put("v", signMap.get("v"));
                    body.put("message", nonce);

                    // ================== 5. 登录 ==================
                    api.login(body, new ApiCallback<UserAccountDTO>() {
                        @Override
                        public void onSuccess(UserAccountDTO user) {

                            // 存 token
                            SharedPrefsUtil.putString("wallet_token", user.getToken());

                            UserManager.getInstance().setUser(
                                    CommunityActivity.this,
                                    user.getUserId(),
                                    user.getUsername(),
                                    user.getAvatarUrl(),
                                    user.getWalletAddress()
                            );

                            Log.d("UserInit", "login success");
                            loadPosts();
                        }

                        @Override
                        public void onFail(String msg) {
                            Log.e("UserInit", "login failed: " + msg);
                        }
                    });

                } catch (Exception e) {
                    Log.e("UserInit", "sign error", e);
                }
            }

            @Override
            public void onFail(String msg) {
                Log.e("UserInit", "get nonce failed: " + msg);
            }
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