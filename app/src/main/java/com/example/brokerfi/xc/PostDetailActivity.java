package com.example.brokerfi.xc;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
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
import com.example.brokerfi.xc.dto.CommentDTO;
import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.xc.net.ApiCallback;
import com.example.brokerfi.xc.net.PageResponse;

import java.util.ArrayList;
import java.util.List;

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

        adapter.setOnPostActionListener((post, position) -> {
            showRewardDialog(post, position);
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

        // 1. 加载帖子详情
        new PostApi().getPostDetail(postId, new ApiCallback<PostDTO>() {
            @Override
            public void onSuccess(PostDTO data) {

                post = data;
                dataList.add(post);
                adapter.notifyItemInserted(0);

                // 2. 加载评论
                // 加载帖子成功后
                currentPage = 0;
                hasMore = true;
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

                    // 获取收款地址
                    String toAddress = post.getAddress();

                    // 执行打赏
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
        Log.d("Reward", "Sending transaction: to=" + toAddress + ", amount=" + amount + ", privateKey=" + privateKey);

        new Thread(() -> {

            runOnUiThread(() -> {
                Toast.makeText(this, "交易已提交，等待确认...", Toast.LENGTH_LONG).show();
            });

            try {
                String txHash = Web3jTransferUtil.sendTransaction(
                        privateKey,
                        toAddress,
                        amount
                );

                Log.d("Reward", "转账结果: txHash=" + txHash);

                runOnUiThread(() -> {

                    if (txHash != null && txHash.startsWith("0x")) {

                        Toast.makeText(this, "打赏成功，txHash=" + txHash, Toast.LENGTH_LONG).show();

                        // 更新UI
                         post.setRewardAmount(post.getRewardAmount() + Double.parseDouble(amount));
                         adapter.notifyItemChanged(position);

                    } else {
                        Toast.makeText(this, "打赏失败：" + txHash, Toast.LENGTH_LONG).show();
                    }

                });

            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(() -> {
                    Toast.makeText(this, "打赏异常", Toast.LENGTH_SHORT).show();
                });

            } finally {
                rewarding = false;
            }

        }).start();
    }
}
