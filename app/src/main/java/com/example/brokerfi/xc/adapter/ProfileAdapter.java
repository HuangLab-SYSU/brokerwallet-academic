package com.example.brokerfi.xc.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.xc.dto.ProfileHeaderDTO;

import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_POST = 1;

    private Context context;
    private List<Object> dataList;

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

    @Override
    public int getItemViewType(int position) {
        if (dataList.get(position) instanceof ProfileHeaderDTO) {
            return TYPE_HEADER;
        } else {
            return TYPE_POST;
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

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
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

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

        TextView tvUsername, tvPostCount, tvReward;

        public HeaderHolder(View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvPostCount = itemView.findViewById(R.id.tv_post_count);
            tvReward = itemView.findViewById(R.id.tv_reward_total);
        }

        public void bind(ProfileHeaderDTO data) {
            tvUsername.setText(data.username);
            tvPostCount.setText("Posts: " + data.postCount);
            tvReward.setText("Earned: " + data.rewardTotal);
        }
    }

    class PostHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvContent, tvUsername, tvLike;

        public PostHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvLike = itemView.findViewById(R.id.btn_like);
        }

        public void bind(PostDTO post) {
            tvTitle.setText(post.title);
            tvContent.setText(post.content);
            tvUsername.setText(post.username);
            tvLike.setText("👍 " + post.likeCount);
        }
    }
}
