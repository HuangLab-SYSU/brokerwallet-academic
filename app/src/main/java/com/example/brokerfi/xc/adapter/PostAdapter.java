package com.example.brokerfi.xc.adapter;

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

    public PostAdapter(Context context, List<PostDTO> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        PostDTO post = postList.get(position);

        holder.tvUsername.setText(post.username);
        holder.tvTitle.setText(post.title);
        // 截取正文 50 字作为首页摘要
        if (post.content.length() > 50) {
            holder.tvContent.setText(post.content.substring(0, 50) + "...");
        } else {
            holder.tvContent.setText(post.content);
        }

        holder.tvLike.setText("👍 " + post.likeCount);
        holder.tvComment.setText("💬 " + post.commentCount);
        holder.tvReward.setText(post.isRewarded ? "💰 Rewarded" : "💰 Reward");

        // 加载头像(找个placeholder_image？
        Glide.with(context)
                .load(post.avatarUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(holder.ivAvatar);

        // 加载首张图片(找个placeholder_image？
        if (post.firstImageUrl != null && !post.firstImageUrl.isEmpty()) {
            holder.ivImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.firstImageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
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
