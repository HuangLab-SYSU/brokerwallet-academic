package com.example.brokerfi.xc.adapter;

import android.app.AlertDialog;
import android.content.Context;
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

    public interface OnPostActionListener {
        void onRewardClick(PostDTO post, int position);
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

            ((PostViewHolder) holder).tvTitle.setText(post.getTitle());
            ((PostViewHolder) holder).tvContent.setText(post.getContent());
            ((PostViewHolder) holder).tvUsername.setText(post.getUserName());

            PostViewHolder vh = (PostViewHolder) holder;

            // 基础绑定
            vh.tvTitle.setText(post.getTitle());
            vh.tvContent.setText(post.getContent());
            vh.tvUsername.setText(post.getUserName());
            vh.tvRewardTotal.setText("Total Reward: " + post.getRewardTotal());

            // 点赞数显示
            vh.btnLike.setText("👍 " + post.getLikeCount());

            // 点赞点击
            vh.btnLike.setOnClickListener(v -> {

                if (post.getIsLiked()) {
                    int like = post.getLikeCount() - 1;
                    post.setLikeCount(like);
                } else {
                    int like = post.getLikeCount() + 1;
                    post.setLikeCount(like);
                }

                post.setIsLiked(!post.getIsLiked());

                notifyItemChanged(position);
            });

            // 打赏
            vh.btnReward.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRewardClick(post, position);
                }
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

}
