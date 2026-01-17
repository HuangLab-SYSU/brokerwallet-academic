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
        if(item.getImageUrl().startsWith("http://localhost:5000/")){
            item.setImageUrl(item.getImageUrl().replace("http://localhost:5000/","http://dash.broker-chain.com.com:5000/"));
        }
        if(item.getImageUrl().startsWith("/uploads")){
            item.setImageUrl("http://dash.broker-chain.com:5000"+item.getImageUrl());
        }
        ViewHolder nftHolder = (ViewHolder) holder;
        
        android.util.Log.d("NFTAdapter", "绑定NFT: " + item.getName());
        
        // 解析并显示时间信息
        displayTimeInfo(nftHolder, item);
        
        // 设置点击监听器：显示NFT详情对话框
        nftHolder.itemView.setOnClickListener(v -> {
            showNftDetailDialog(v.getContext(), item);
        });
        
        // 使用Glide加载图片，优化大图片处理
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            android.util.Log.d("NFTAdapter", "加载图片: " + item.getName() + ", URL长度: " + item.getImageUrl().length());
            
            // 普通图片URL，使用Glide加载
            // Glide支持Base64格式（data:image/jpeg;base64,xxx）和HTTP URL
            // 列表卡片使用压缩图片以优化性能和内存
        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                    .override(300, 450) // 列表中压缩图片，优化内存和性能
                    .fitCenter() // 使用fitCenter而不是centerCrop，避免裁剪
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
    
    /**
     * 显示时间信息（材料上传时间、NFT铸造时间、持有者地址）
     */
    private void displayTimeInfo(ViewHolder holder, NFTItem item) {
        String uploadTime = item.getUploadTime();
        String mintTime = item.getMintTime();
        String ownerAddress = item.getOwnerAddress();
        String ownerDisplayName = item.getOwnerDisplayName();
        
        int visibleCount = 0;
        
        // Display material upload time
        if (uploadTime != null && !uploadTime.isEmpty()) {
            holder.uploadTimeText.setText("Material Upload: " + uploadTime);
            holder.uploadTimeText.setVisibility(View.VISIBLE);
            visibleCount++;
        } else {
            holder.uploadTimeText.setVisibility(View.GONE);
        }
        
        // Display NFT minting time
        if (mintTime != null && !mintTime.isEmpty()) {
            holder.mintTimeText.setText("NFT Minted: " + mintTime);
            holder.mintTimeText.setVisibility(View.VISIBLE);
            visibleCount++;
        } else {
            holder.mintTimeText.setVisibility(View.GONE);
        }
        
        // Display owner address (shortened)
        if (ownerAddress != null && !ownerAddress.isEmpty()) {
            String shortAddress = shortenAddress(ownerAddress);
            holder.ownerAddressText.setText("Owner Address: " + shortAddress);
            holder.ownerAddressText.setVisibility(View.VISIBLE);
            visibleCount++;
        } else {
            holder.ownerAddressText.setVisibility(View.GONE);
        }
        
        // Display owner nickname
        if (ownerDisplayName != null && !ownerDisplayName.isEmpty() && !ownerDisplayName.equals("Anonymous")) {
            holder.ownerDisplayNameText.setText("Owner Nickname: " + ownerDisplayName);
            holder.ownerDisplayNameText.setVisibility(View.VISIBLE);
            visibleCount++;
        } else {
            holder.ownerDisplayNameText.setVisibility(View.GONE);
        }
        
        // 如果有时间信息，显示时间区域
        holder.attributesLayout.setVisibility(visibleCount > 0 ? View.VISIBLE : View.GONE);
    }
    
    /**
     * 缩短地址显示（显示前6位+后4位）
     */
    private String shortenAddress(String address) {
        if (address == null || address.length() < 10) {
            return address;
        }
        return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }
    
    /**
     * 显示NFT详情对话框（图片 + 2个时间属性）
     */
    private void showNftDetailDialog(android.content.Context context, NFTItem item) {
        // 创建对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        android.view.View dialogView = android.view.LayoutInflater.from(context)
                .inflate(R.layout.dialog_nft_detail, null);
        builder.setView(dialogView);
        
        // 获取控件
        android.widget.ImageView nftImageView = dialogView.findViewById(R.id.nftImageView);
        android.widget.LinearLayout attributesContainer = dialogView.findViewById(R.id.attributesContainer);
        
        // 加载NFT图片（Glide支持data URL和HTTP URL）
        // 详情对话框中使用原始分辨率，保证画质
        if (nftImageView != null && item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL) // 使用原图尺寸
                    .fitCenter() // 完整显示图片，不裁剪
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // 缓存原图
                    .into(nftImageView);
            android.util.Log.d("NFTAdapter", "详情对话框加载高清原图: " + item.getImageUrl());
        }
        
        // 显示时间属性
        if (attributesContainer != null) {
            attributesContainer.removeAllViews();
            
            // Material upload time
            if (item.getUploadTime() != null && !item.getUploadTime().isEmpty()) {
                addAttributeRow(context, attributesContainer, "Material Upload", item.getUploadTime());
            }
            
            // NFT minting time
            if (item.getMintTime() != null && !item.getMintTime().isEmpty()) {
                addAttributeRow(context, attributesContainer, "NFT Minted", item.getMintTime());
            }
        }
        
        // 创建对话框
        android.app.AlertDialog dialog = builder.create();
        
        dialog.show();
    }
    
    /**
     * 添加属性行到容器
     */
    private void addAttributeRow(android.content.Context context, android.widget.LinearLayout container, 
                                  String label, String value) {
        // 创建属性行
        android.widget.LinearLayout row = new android.widget.LinearLayout(context);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setPadding(0, 16, 0, 16);
        
        // 标签
        android.widget.TextView labelView = new android.widget.TextView(context);
        labelView.setText(label + ": ");
        labelView.setTextSize(16);
        labelView.setTextColor(0xFF666666);
        android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        labelView.setLayoutParams(labelParams);
        
        // 值
        android.widget.TextView valueView = new android.widget.TextView(context);
        valueView.setText(value);
        valueView.setTextSize(16);
        valueView.setTextColor(0xFF333333);
        valueView.setTypeface(null, android.graphics.Typeface.BOLD);
        android.widget.LinearLayout.LayoutParams valueParams = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 2);
        valueView.setLayoutParams(valueParams);
        
        row.addView(labelView);
        row.addView(valueView);
        container.addView(row);
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ViewGroup attributesLayout;
        TextView uploadTimeText;
        TextView mintTimeText;
        TextView ownerAddressText;
        TextView ownerDisplayNameText;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            attributesLayout = itemView.findViewById(R.id.attributesLayout);
            uploadTimeText = itemView.findViewById(R.id.uploadTimeText);
            mintTimeText = itemView.findViewById(R.id.mintTimeText);
            ownerAddressText = itemView.findViewById(R.id.ownerAddressText);
            ownerDisplayNameText = itemView.findViewById(R.id.ownerDisplayNameText);
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
                footerText.setText("Loading...");
                footerProgress.setVisibility(View.VISIBLE);
            } else if (hasMore) {
                footerText.setText("Pull up to load more");
                footerProgress.setVisibility(View.GONE);
            } else {
                footerText.setText("End of list ~ Submit materials to get more NFTs");
                footerProgress.setVisibility(View.GONE);
            }
        }
    }
}