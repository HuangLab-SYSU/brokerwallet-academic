package com.example.brokerfi.xc.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.brokerfi.R;
import com.example.brokerfi.xc.dto.CommentDTO;
import com.example.brokerfi.xc.dto.PostDTO;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PostDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_POST = 0;
    private static final int TYPE_COMMENT = 1;
    private Long currentUserId;

    private Context context;
    private List<Object> dataList;

    public PostDetailAdapter(Context context, List<Object> dataList, Long currentUserId) {
        this.context = context;
        this.dataList = dataList;
        this.currentUserId = currentUserId;
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

    public interface OnPostActionListener {
        void onRewardClick(PostDTO post, int position);
        void onLikeClick(PostDTO post, int position);
    }

    private OnPostActionListener listener;

    public void setOnPostActionListener(OnPostActionListener listener) {
        this.listener = listener;
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
            PostViewHolder vh = (PostViewHolder) holder;

            // 基础绑定
            vh.tvTitle.setText(post.getTitle());
            vh.tvContent.setText(post.getContent());
            vh.tvUsername.setText(post.getUserName());
            vh.btnComment.setText("💬 " + post.getCommentCount());
            vh.btnLike.setText("👍 " + post.getLikeCount());
            // 格式化时间
            try {
                vh.tvTime.setText(
                        new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(
                                Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(post.getCreateTime()))
                        )
                );
            } catch (Exception e) {
                vh.tvTime.setText(post.getCreateTime());
            }

            String rewardStr = "0";
            if (post.getRewardAmount() != null) {
                rewardStr = post.getRewardAmount().stripTrailingZeros().toPlainString();
            }
            vh.tvRewardTotal.setText("Total Reward(BKC): " + rewardStr);

            if (post.getUserId().equals(currentUserId)) {
                // 自己发的帖子：隐藏打赏按钮
                vh.btnReward.setVisibility(View.GONE);
            } else {
                // 别人的帖子：正常显示
                vh.btnReward.setVisibility(View.VISIBLE);
            }

            // 点赞
            vh.btnLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(post, position);
                }
            });

            // 打赏
            vh.btnReward.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRewardClick(post, position);
                }
            });

            //图片展示
            List<String> imageList = post.getImages();
            if (imageList != null && !imageList.isEmpty()) {
                vh.rvImages.setVisibility(View.VISIBLE);

                // 九宫格 3列
                GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 3);
                vh.rvImages.setLayoutManager(gridLayoutManager);
                vh.rvImages.setNestedScrollingEnabled(false);  // 关闭嵌套滚动
                vh.rvImages.setHasFixedSize(false);            // 允许高度自适应
                ImageAdapter imageAdapter = new ImageAdapter(context, imageList);
                vh.rvImages.setAdapter(imageAdapter);

            } else {
                vh.rvImages.setVisibility(View.GONE);
            }

        } else if (holder instanceof CommentViewHolder) {
            CommentDTO comment = (CommentDTO) dataList.get(position);

            CommentViewHolder vh = (CommentViewHolder) holder;

            // 格式化时间
            try {
                vh.tvTime.setText(
                        new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(
                                Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(comment.getCreateTime()))
                        )
                );
            } catch (Exception e) {
                vh.tvTime.setText(comment.getCreateTime());
            }

            vh.tvUsername.setText(comment.getUserName());
            vh.tvContent.setText(comment.getContent());

            // 头像（用 Glide 或默认图）
            if (comment.getAvatarUrl() != null && !comment.getAvatarUrl().isEmpty()) {
                Glide.with(context)
                        .load(comment.getAvatarUrl())
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

        TextView tvTitle, tvContent, tvUsername, tvTime;
        TextView btnLike, btnComment, btnReward, tvRewardTotal;
        RecyclerView rvImages;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvTime = itemView.findViewById(R.id.tv_time);

            btnLike = itemView.findViewById(R.id.btn_like);
            btnComment = itemView.findViewById(R.id.btn_comment);
            btnReward = itemView.findViewById(R.id.btn_reward);
            tvRewardTotal = itemView.findViewById(R.id.tv_reward_total);

            rvImages = itemView.findViewById(R.id.rv_images);
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

}
