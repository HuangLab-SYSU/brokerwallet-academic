package com.example.brokerfi.xc.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.brokerfi.R;
import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.xc.dto.ProfileHeaderDTO;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import com.example.brokerfi.common.ui.Holder;


public class ProfileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_POST = 1;

    private final Context context;
    private final List<Object> dataList;

    public interface OnPostClickListener {
        void onPostClick(PostDTO post);
    }

    private OnPostClickListener onPostClickListener;

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.onPostClickListener = listener;
    }

    public ProfileAdapter(Context context, List<Object> dataList) {
        this.context = context;
        this.dataList = dataList;
    }

    public interface OnEditClickListener {
        void onEditClick();
    }

    private OnEditClickListener onEditClickListener;

    public void setOnEditClickListener(OnEditClickListener listener) {
        this.onEditClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Object obj = dataList.get(position);
        if (obj instanceof ProfileHeaderDTO) {
            return TYPE_HEADER;
        } else if (obj instanceof PostDTO) {
            return TYPE_POST;
        } else {
            throw new IllegalStateException("Unknown type");
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_profile_header, parent, false);
            return new HeaderHolder(view);
        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_post, parent, false);
            return new PostHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof HeaderHolder) {
            ProfileHeaderDTO data = (ProfileHeaderDTO) dataList.get(position);
            ((HeaderHolder) holder).bind(data);
        } else {
            PostDTO post = (PostDTO) dataList.get(position);
            PostHolder postHolder = (PostHolder) holder;
            postHolder.bind(post);

            postHolder.itemView.setOnClickListener(v -> {
                if (onPostClickListener != null) {
                    onPostClickListener.onPostClick(post);
                }
            });
        }
    }

    class HeaderHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar;
        TextView tvUsername, tvPostCount, tvReward;
        View btnEdit;

        public HeaderHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvPostCount = itemView.findViewById(R.id.tv_post_count);
            tvReward = itemView.findViewById(R.id.tv_reward_total);
            btnEdit = itemView.findViewById(R.id.btn_edit);
        }

        @SuppressLint("SetTextI18n")
        public void bind(ProfileHeaderDTO data) {
            tvUsername.setText(data.getUsername());
            tvPostCount.setText(tvPostCount.getContext().getString(R.string.profile_adapter_posts) + " " + data.getPostCount());
            tvReward.setText(tvReward.getContext().getString(R.string.profile_adapter_earned) + " " + data.getRewardTotal());

            if (data.getAvatar() != null && !data.getAvatar().isEmpty()) {
                Glide.with(context)
                        .load(data.getAvatar())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(7)))
                        .into(ivAvatar);
            }

            btnEdit.setOnClickListener(v -> {
                if (onEditClickListener != null) {
                    onEditClickListener.onEditClick();
                }
            });
        }
    }

    static class PostHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvContent, tvUsername, tvLike, tvPostTime;

        public PostHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvLike = itemView.findViewById(R.id.btn_like);
            tvPostTime = itemView.findViewById(R.id.tv_post_time);
        }

        @SuppressLint("SetTextI18n")
        public void bind(PostDTO post) {
            tvTitle.setText(post.getTitle());
            tvContent.setText(post.getContent());
            tvUsername.setText(post.getUserName());
            tvLike.setText(tvLike.getContext().getString(R.string.post_adapter_like_count, post.getLikeCount()));
            // Format time
            try {
                tvPostTime.setText(
                        new SimpleDateFormat("MM-dd", Locale.getDefault()).format(
                                Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(post.getCreateTime()))
                        )
                );
            } catch (Exception e) {
                tvPostTime.setText(post.getCreateTime());
            }

        }
    }
}
