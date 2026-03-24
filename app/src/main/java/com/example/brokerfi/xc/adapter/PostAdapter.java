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

import java.util.List;

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

        if (post.getContent() != null && post.getContent().length() > 50) {
            holder.tvContent.setText(post.getContent().substring(0, 50) + "...");
        } else {
            holder.tvContent.setText(post.getContent());
        }

        holder.tvLike.setText("👍 " + post.getLikeCount());
        holder.tvComment.setText("💬 " + post.getCommentCount());

        // 图片
        if (post.getFirstImageUrl() != null && !post.getFirstImageUrl().isEmpty()) {
            holder.ivImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getFirstImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setVisibility(View.GONE);
        }

        // 点击事件（重点）
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

    // 推荐：提供数据刷新方法（比直接操作 list 更规范）
    public void updateData(List<PostDTO> newList) {
        this.postList.clear();
        this.postList.addAll(newList);
        notifyDataSetChanged();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {

        ImageView ivAvatar, ivImage;
        TextView tvUsername, tvTitle, tvContent;
        TextView tvLike, tvComment, tvReward;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            ivImage = itemView.findViewById(R.id.iv_image);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvLike = itemView.findViewById(R.id.btn_like);
            tvComment = itemView.findViewById(R.id.btn_comment);
            tvReward = itemView.findViewById(R.id.btn_reward);
        }
    }
}