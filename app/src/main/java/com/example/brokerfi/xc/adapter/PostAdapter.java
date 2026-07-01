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

import com.bumptech.glide.Glide;
import com.example.brokerfi.R;
import com.example.brokerfi.xc.dto.PostDTO;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import com.example.brokerfi.common.ui.Holder;


public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<PostDTO> postList;

    public interface OnItemClickListener {
        void onItemClick(PostDTO post);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public PostAdapter(Context context, List<PostDTO> postList) {
        this.context = context;
        this.postList = postList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<PostDTO> list) {
        this.postList.clear();
        this.postList.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        PostDTO post = postList.get(position);

        holder.tvUsername.setText(post.getUserName());
        holder.tvTitle.setText(post.getTitle());
        // Format time
        try {
            holder.tv_post_time.setText(
                    new SimpleDateFormat("MM-dd", Locale.getDefault()).format(
                            Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(post.getCreateTime()))
                    )
            );
        } catch (Exception e) {
            holder.tv_post_time.setText(post.getCreateTime());
        }

        if (post.getContent() != null && post.getContent().length() > 50) {
            holder.tvContent.setText(context.getString(R.string.post_adapter_content_truncated, post.getContent().substring(0, 50)));
        } else {
            holder.tvContent.setText(post.getContent());
        }

        holder.tvLike.setText(context.getString(R.string.post_adapter_like_count, post.getLikeCount()));
        holder.tvComment.setText(context.getString(R.string.post_adapter_comment_count, post.getCommentCount()));

        // picture
        if (post.getFirstImageUrl() != null && !post.getFirstImageUrl().isEmpty()) {
            holder.ivImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getFirstImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setVisibility(View.GONE);
        }

        // Click event (key point)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList == null ? 0 : postList.size();
    }

    // Recommendation: Provide data refresh method (more standardized than directly operating list)
    public void updateData(List<PostDTO> newList) {
        this.postList.clear();
        this.postList.addAll(newList);
        notifyDataSetChanged();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar, ivImage;
        TextView tvUsername, tvTitle, tvContent, tv_post_time;
        TextView tvLike, tvComment, tvReward;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            ivImage = itemView.findViewById(R.id.iv_image);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tv_post_time = itemView.findViewById(R.id.tv_post_time);
            tvLike = itemView.findViewById(R.id.btn_like);
            tvComment = itemView.findViewById(R.id.btn_comment);
            tvReward = itemView.findViewById(R.id.btn_reward);
        }
    }
}
