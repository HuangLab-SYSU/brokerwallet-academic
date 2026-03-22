package com.example.brokerfi.xc.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.brokerfi.R;
import com.example.brokerfi.xc.dto.CommentDTO;
import com.example.brokerfi.xc.dto.PostDTO;

import java.util.List;

public class PostDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_POST = 0;
    private static final int TYPE_COMMENT = 1;

    private Context context;
    private List<Object> dataList;

    public PostDetailAdapter(Context context, List<Object> dataList) {
        this.context = context;
        this.dataList = dataList;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = dataList.get(position);

        if (item instanceof PostDTO) {
            return TYPE_POST;
        } else if (item instanceof CommentDTO) {
            return TYPE_COMMENT;
        } else {
            throw new IllegalArgumentException("Unknown type");
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == TYPE_POST) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_post_detail_header, parent, false);
            return new PostViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof PostViewHolder) {
            PostDTO post = (PostDTO) dataList.get(position);

            ((PostViewHolder) holder).tvTitle.setText(post.title);
            ((PostViewHolder) holder).tvContent.setText(post.content);
            ((PostViewHolder) holder).tvUsername.setText(post.username);

            PostViewHolder vh = (PostViewHolder) holder;

            // 基础绑定
            vh.tvTitle.setText(post.title);
            vh.tvContent.setText(post.content);
            vh.tvUsername.setText(post.username);
            vh.tvRewardTotal.setText("Total Reward: " + post.rewardTotal);

            // 点赞数显示
            vh.btnLike.setText("👍 " + post.likeCount);

            // 点赞点击
            vh.btnLike.setOnClickListener(v -> {

                if (post.isLiked) {
                    post.likeCount--;
                } else {
                    post.likeCount++;
                }

                post.isLiked = !post.isLiked;

                notifyItemChanged(position);
            });

            // 打赏
            vh.btnReward.setOnClickListener(v -> {
                showRewardDialog(post, position);
            });

        } else if (holder instanceof CommentViewHolder) {
            CommentDTO comment = (CommentDTO) dataList.get(position);

            CommentViewHolder vh = (CommentViewHolder) holder;

            vh.tvUsername.setText(comment.username);
            vh.tvTime.setText(comment.time);
            vh.tvContent.setText(comment.content);

            // 头像（用 Glide 或默认图）
            if (comment.avatarUrl != null && !comment.avatarUrl.isEmpty()) {
                Glide.with(context)
                        .load(comment.avatarUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .into(vh.ivAvatar);
            } else {
                vh.ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvContent, tvUsername;
        TextView btnLike, btnReward, tvRewardTotal;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvUsername = itemView.findViewById(R.id.tv_username);

            btnLike = itemView.findViewById(R.id.btn_like);
            btnReward = itemView.findViewById(R.id.btn_reward);
            tvRewardTotal = itemView.findViewById(R.id.tv_reward_total);
        }
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar;
        TextView tvUsername, tvTime, tvContent;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);

            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvContent = itemView.findViewById(R.id.tv_content);
        }
    }

    // TODO：换个地方写
    private void showRewardDialog(PostDTO post, int position) {

        EditText input = new EditText(context);
        input.setHint("输入BKC数量");

        new AlertDialog.Builder(context)
                .setTitle("打赏")
                .setView(input)
                .setPositiveButton("确认", (dialog, which) -> {

                    String value = input.getText().toString().trim();

                    if (value.isEmpty()) {
                        Toast.makeText(context, "请输入金额", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double amount;

                    try {
                        amount = Double.parseDouble(value);
                    } catch (Exception e) {
                        Toast.makeText(context, "金额格式错误", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ===== 校验逻辑 =====

                    if (amount <= 0) {
                        Toast.makeText(context, "金额必须大于0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double userBalance = 100.0; // 👉 模拟用户余额

                    if (amount > userBalance) {
                        Toast.makeText(context, "余额不足", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ===== 成功逻辑（本地更新）=====

                    post.rewardTotal += amount;

                    notifyItemChanged(position);

                    Toast.makeText(context, "打赏成功 +" + amount, Toast.LENGTH_SHORT).show();

                })
                .setNegativeButton("取消", null)
                .show();
    }
}
