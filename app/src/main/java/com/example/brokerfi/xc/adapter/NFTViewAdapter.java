package com.example.brokerfi.xc.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.brokerfi.R;
import com.example.brokerfi.xc.NFTViewActivity.NFTItem;
import java.util.List;

public class NFTViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int TYPE_NFT = 0;
    private static final int TYPE_FOOTER = 1;
    
    private List<NFTItem> nftList;
    private boolean hasMore = true;
    private boolean isLoading = false;
    private OnLoadMoreListener loadMoreListener;
    private OnItemClickListener itemClickListener;
    
    public interface OnLoadMoreListener {
        void onLoadMore();
    }
    
    public interface OnItemClickListener {
        void onItemClick(NFTItem item, int position);
    }
    
    public NFTViewAdapter(List<NFTItem> nftList) {
        this.nftList = nftList;
        android.util.Log.d("NFTAdapter", "NFTViewAdapter构造函数调用，列表大小: " + (nftList != null ? nftList.size() : "null"));
    }
    
    public void updateData(List<NFTItem> newNftList) {
        this.nftList = newNftList;
        android.util.Log.d("NFTAdapter", "updateData调用，新列表大小: " + (newNftList != null ? newNftList.size() : "null"));
        notifyDataSetChanged();
    }
    
    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
        notifyDataSetChanged();
    }
    
    public void setLoading(boolean loading) {
        this.isLoading = loading;
        notifyDataSetChanged();
    }
    
    public void setLoadMoreListener(OnLoadMoreListener listener) {
        this.loadMoreListener = listener;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }
    
    @Override
    public int getItemViewType(int position) {
        if (position == nftList.size()) {
            return TYPE_FOOTER;
        }
        return TYPE_NFT;
    }
    
    @Override
    public int getItemCount() {
        return nftList.size() + 1; // +1 for footer
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        android.util.Log.d("NFTAdapter", "onCreateViewHolder调用: viewType=" + viewType);
        if (viewType == TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_nft_footer, parent, false);
            return new FooterViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_nft_view, parent, false);
            return new ViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        android.util.Log.d("NFTAdapter", "onBindViewHolder调用: position=" + position + ", 总数=" + getItemCount());
        
        if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind(hasMore, isLoading);
            return;
        }
        
        NFTItem item = nftList.get(position);
        ViewHolder nftHolder = (ViewHolder) holder;
        
        android.util.Log.d("NFTAdapter", "绑定NFT: " + item.getName() + ", 描述: " + item.getDescription());
        nftHolder.nameText.setText(item.getName());
        nftHolder.descriptionText.setText(item.getDescription());
        
        // 设置点击监听器
        nftHolder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(item, position);
            }
        });
        
        // 使用Glide加载图片，优化大图片处理
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            // 有图片URL，使用Glide加载
            android.util.Log.d("NFTAdapter", "加载图片: " + item.getName() + ", URL长度: " + item.getImageUrl().length());
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .override(300, 300) // 限制图片大小
                    .centerCrop()
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // 缓存图片
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            android.util.Log.e("NFTAdapter", "图片加载失败: " + item.getName(), e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            android.util.Log.d("NFTAdapter", "图片加载成功: " + item.getName());
                            return false;
                        }
                    })
                    .into(nftHolder.imageView);
        } else {
            // 没有图片URL，显示占位符
            android.util.Log.d("NFTAdapter", "使用占位符: " + item.getName());
            nftHolder.imageView.setImageResource(R.drawable.placeholder_image);
        }
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameText;
        TextView descriptionText;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            nameText = itemView.findViewById(R.id.nameText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
        }
    }
    
    public static class FooterViewHolder extends RecyclerView.ViewHolder {
        TextView footerText;
        android.widget.ProgressBar footerProgress;
        
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
            footerText = itemView.findViewById(R.id.footerText);
            footerProgress = itemView.findViewById(R.id.footerProgress);
        }
        
        public void bind(boolean hasMore, boolean isLoading) {
            if (isLoading) {
                footerText.setText("正在加载...");
                footerProgress.setVisibility(View.VISIBLE);
            } else if (hasMore) {
                footerText.setText("下拉刷新更多");
                footerProgress.setVisibility(View.GONE);
            } else {
                footerText.setText("已经到底~请提交材料获得更多NFT");
                footerProgress.setVisibility(View.GONE);
            }
        }
    }
}