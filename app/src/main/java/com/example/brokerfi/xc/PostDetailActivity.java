package com.example.brokerfi.xc;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.adapter.PostDetailAdapter;
import com.example.brokerfi.xc.api.PostApi;
import com.example.brokerfi.xc.api.RewardApi;
import com.example.brokerfi.xc.dto.CommentDTO;
import com.example.brokerfi.xc.dto.LikeStatusDTO;
import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.xc.manager.UserManager;
import com.example.brokerfi.xc.net.ApiCallback;
import com.example.brokerfi.xc.net.PageResponse;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.example.brokerfi.xc.menu.NavigationHelper;

public class PostDetailActivity extends AppCompatActivity {

    private RecyclerView rvDetail;
    private EditText etComment;
    private Button btnSend;
    private Button btnReward;
    private PostDetailAdapter adapter;
    private List<Object> dataList = new ArrayList<>();
    private Long postId;
    private PostDTO post;
    private int currentPage = 0;
    private final int pageSize = 10;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout actionBar;
    private NavigationHelper navigationHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_posts);

        initView();
        initRecyclerView();
        loadData();
    }

    private void initView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        actionBar = findViewById(R.id.action_bar);
        navigationHelper = new NavigationHelper(menu, actionBar, this, notificationBtn);
        rvDetail = findViewById(R.id.rv_post_detail);
        etComment = findViewById(R.id.et_comment);
        btnSend = findViewById(R.id.btn_send);

        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(PostDetailActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initRecyclerView() {
        rvDetail.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostDetailAdapter(this, dataList, UserManager.getInstance().getUserId());
        rvDetail.setAdapter(adapter);

        rvDetail.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                if (dy <= 0) return;

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();

                int total = lm.getItemCount();
                int lastVisible = lm.findLastVisibleItemPosition();

                // 提前2个加载
                if (lastVisible >= total - 2) {
                    loadComments();
                }
            }
        });

        // 发表评论
        btnSend.setOnClickListener(v -> {
            String content = etComment.getText().toString().trim();

            if (content.isEmpty()) return;

            new PostApi().addComment(postId, UserStorageUtil.getUserId(this), content, new ApiCallback<CommentDTO>() {
                @Override
                public void onSuccess(CommentDTO comment) {
                    int insertPosition = dataList.size();
                    dataList.add(comment);
                    adapter.notifyItemInserted(insertPosition);
                    rvDetail.scrollToPosition(insertPosition);
                    etComment.setText("");
                }

                @Override
                public void onFail(String msg) {
                    Toast.makeText(PostDetailActivity.this, "评论失败：" + msg, Toast.LENGTH_SHORT).show();
                }
            });

        });

        adapter.setOnPostActionListener(new PostDetailAdapter.OnPostActionListener() {

            @Override
            public void onRewardClick(PostDTO post, int position) {
                showRewardDialog(post, position);
            }

            @Override
            public void onLikeClick(PostDTO post, int position) {
                handleLike(post, position);
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadData() {

        postId = getIntent().getLongExtra("postId", -1);
        if (postId == -1) {
            Toast.makeText(this, "帖子ID错误", Toast.LENGTH_SHORT).show();
            return;
        }

        dataList.clear();
        adapter.notifyDataSetChanged();

        // 加载帖子详情
        new PostApi().getPostDetail(postId, new ApiCallback<PostDTO>() {
            @Override
            public void onSuccess(PostDTO data) {
                post = data;
                dataList.add(post);
                adapter.notifyItemInserted(0);
                currentPage = 0;
                hasMore = true;
                // 加载评论详情
                loadComments();
            }

            @Override
            public void onFail(String msg) {
                Toast.makeText(PostDetailActivity.this, "加载帖子失败：" + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadComments() {

        if (isLoading || !hasMore) return;

        isLoading = true;

        new PostApi().getComments(postId, currentPage, pageSize,
                new ApiCallback<PageResponse<CommentDTO>>() {

                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onSuccess(PageResponse<CommentDTO> pageData) {

                        List<CommentDTO> list = pageData.getContent();
                        dataList.addAll(list);
                        adapter.notifyDataSetChanged();
                        currentPage++;
                        // 是否还有下一页
                        if (currentPage >= pageData.getTotalPages()) {
                            hasMore = false;
                        }
                        isLoading = false;
                    }

                    @Override
                    public void onFail(String msg) {
                        isLoading = false;
                        Toast.makeText(PostDetailActivity.this, "加载评论失败：" + msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isLikeRequesting = false;
    private void handleLike(PostDTO post, int position) {

        Long userId = UserStorageUtil.getUserId(this);
        if (userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 防重复请求
        if (isLikeRequesting) {
            return;
        }
        isLikeRequesting = true;

        if (post.getIsLiked()) {
            // 取消点赞
            new PostApi().unlikePost(post.getId(), userId, new LikeCallback(post, position) {
                @Override
                public void onFail(String msg) {
                    super.onFail(msg);
                    Toast.makeText(PostDetailActivity.this, "取消点赞失败", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // 点赞
            new PostApi().likePost(post.getId(), userId, new LikeCallback(post, position) {
                @Override
                public void onFail(String msg) {
                    super.onFail(msg);
                    Toast.makeText(PostDetailActivity.this, "点赞失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private abstract class LikeCallback implements ApiCallback<LikeStatusDTO> {
        private final PostDTO post;
        private final int position;

        public LikeCallback(PostDTO post, int position) {
            this.post = post;
            this.position = position;
        }

        @Override
        public void onSuccess(LikeStatusDTO res) {
            isLikeRequesting = false;
            if (res == null) return;
            post.setIsLiked(res.isLiked());
            post.setLikeCount(res.getLikeCount());
            adapter.notifyItemChanged(position, "payload_like");
        }

        @Override
        public void onFail(String msg) {
            isLikeRequesting = false;
        }
    }


    // 打赏
    private volatile boolean rewarding = false;
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

                    String toAddress = post.getAddress();
                    doReward(toAddress, value, post, position);

                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void doReward(String toAddress, String amount, PostDTO post, int position) {

        if (rewarding) {
            Toast.makeText(this, "请勿重复提交", Toast.LENGTH_SHORT).show();
            return;
        }

        rewarding = true;

        // 获取私钥
        String account = StorageUtil.getPrivateKey(this);
        String acc = StorageUtil.getCurrentAccount(this);
        int i = (acc == null) ? 0 : Integer.parseInt(acc);
        if (account == null) {
            Toast.makeText(this, "未找到账户", Toast.LENGTH_SHORT).show();
            rewarding = false;
            return;
        }
        String[] split = account.split(";");
        String privateKey = split[i];
        String fromAddress = Credentials.create(privateKey).getAddress();

        //Log.d("Reward", "Sending transaction: to=" + toAddress + ", amount=" + amount + ", privateKey=" + privateKey);

        new Thread(() -> {

            runOnUiThread(() -> {
                Toast.makeText(this, "交易已提交，等待确认...", Toast.LENGTH_LONG).show();
            });

            try {
                //发送交易
                String txHash = Web3jTransferUtil.sendTransaction(
                        privateKey,
                        toAddress,
                        amount
                );
                if (txHash == null || !txHash.startsWith("0x")) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "打赏失败：" + txHash, Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                // 构造 message
                long timestamp = System.currentTimeMillis() / 1000;
                String nonce = UUID.randomUUID().toString();
                String message = txHash + "|" + fromAddress + "|" + toAddress + "|" + timestamp + "|" + nonce;
                Map<String, String> sigMap = SecurityUtil.signMessage(privateKey, message);

                RewardApi api = new RewardApi();
                api.verifyReward(
                        txHash,
                        fromAddress,
                        toAddress,
                        timestamp,
                        nonce,
                        sigMap.get("r"),
                        sigMap.get("s"),
                        sigMap.get("v"),
                        amount,
                        post.getId(),
                        new ApiCallback<Boolean>() {

                            @Override
                            public void onSuccess(Boolean success) {
                                runOnUiThread(() -> {
                                    if (success) {
                                        Toast.makeText(PostDetailActivity.this, "打赏成功", Toast.LENGTH_LONG).show();

                                        BigDecimal current = post.getRewardAmount() == null ? BigDecimal.ZERO : post.getRewardAmount();
                                        BigDecimal addAmount = new BigDecimal(amount.trim());
                                        post.setRewardAmount(current.add(addAmount));

                                        adapter.notifyItemChanged(position);

                                    } else {
                                        Toast.makeText(PostDetailActivity.this, "后端验证失败", Toast.LENGTH_LONG).show();
                                    }
                                });
                                rewarding = false;
                            }

                            @Override
                            public void onFail(String msg) {
                                runOnUiThread(() ->
                                        Toast.makeText(PostDetailActivity.this, "请求失败：" + msg, Toast.LENGTH_SHORT).show()
                                );
                                rewarding = false;
                            }
                        }
                );

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "打赏异常", Toast.LENGTH_SHORT).show()
                );

            } finally {
                rewarding = false;
            }

        }).start();
    }
}
