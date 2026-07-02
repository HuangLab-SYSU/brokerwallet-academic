package com.example.brokerfi.xc.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.brokerfi.R;
import com.example.brokerfi.xc.dto.CommentDTO;
import com.example.brokerfi.xc.dto.PostDTO;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import com.example.brokerfi.common.ui.Holder;


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

            // basic binding
            vh.tvTitle.setText(post.getTitle());
            vh.tvContent.setText(post.getContent());
            vh.tvUsername.setText(post.getUserName());
            vh.btnComment.setText(context.getString(R.string.post_adapter_comment_count, post.getCommentCount()));
            vh.btnLike.setText(context.getString(R.string.post_adapter_like_count, post.getLikeCount()));
            // Format time
            try {
                vh.tvTime.setText(
                        new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(
                                Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(post.getCreateTime()))
                        )
                );
            } catch (Exception e) {
                vh.tvTime.setText(post.getCreateTime());
            }

            if (post.getAvatarUrl() != null && !post.getAvatarUrl().isEmpty()) {
                Glide.with(context)
                        .load(post.getAvatarUrl())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(7)))
                        .placeholder(R.drawable.placeholder_image) // placeholder image
                        .into(vh.ivAvatar);
            } else {
                vh.ivAvatar.setImageResource(R.drawable.placeholder_image);
            }

            String rewardStr = "0";
            if (post.getRewardAmount() != null) {
                rewardStr = post.getRewardAmount().stripTrailingZeros().toPlainString();
            }
            vh.tvRewardTotal.setText(vh.tvRewardTotal.getContext().getString(R.string.post_detail_adapter_total_reward_bkc) + " " + rewardStr);

            if (post.getUserId().equals(currentUserId)) {
                // Posts made by yourself: Hide the reward button.
                vh.btnReward.setVisibility(View.GONE);
            } else {
                // Other people’s posts: displayed normally
                vh.btnReward.setVisibility(View.VISIBLE);
            }

            // Like
            vh.btnLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(post, position);
                }
            });

            // reward
            vh.btnReward.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRewardClick(post, position);
                }
            });

            // Picture display
            List<String> imageList = post.getImages();
            if (imageList != null && !imageList.isEmpty()) {
                vh.rvImages.setVisibility(View.VISIBLE);

                // Nine-square grid 3 columns
                GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 3);
                vh.rvImages.setLayoutManager(gridLayoutManager);
                vh.rvImages.setNestedScrollingEnabled(false);  // Turn off nested scrolling
                vh.rvImages.setHasFixedSize(false);            // Allows for a high degree of adaptability
                ImageAdapter imageAdapter = new ImageAdapter(context, imageList);
                vh.rvImages.setAdapter(imageAdapter);

            } else {
                vh.rvImages.setVisibility(View.GONE);
            }

        } else if (holder instanceof CommentViewHolder) {
            CommentDTO comment = (CommentDTO) dataList.get(position);

            CommentViewHolder vh = (CommentViewHolder) holder;

            // Format time
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

            // avatar
            if (comment.getAvatarUrl() != null && !comment.getAvatarUrl().isEmpty()) {
                Glide.with(context)
                        .load(comment.getAvatarUrl())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(7)))
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

        ImageView ivAvatar;
        TextView tvTitle, tvContent, tvUsername, tvTime;
        TextView btnLike, btnComment, btnReward, tvRewardTotal;
        RecyclerView rvImages;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);

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
