package com.example.brokerfi.nft.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.brokerfi.R;
import com.example.brokerfi.core.config.ApiConfig;
import com.example.brokerfi.nft.NFTViewActivity.NFTItem;
import java.util.List;
import com.example.brokerfi.nft.model.NFT;
import com.example.brokerfi.nft.NFTViewActivity;


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
        if (item.getImageUrl() != null) {
            item.setImageUrl(ApiConfig.resolveNftAssetUrl(item.getImageUrl()));
        }
        ViewHolder nftHolder = (ViewHolder) holder;

        android.util.Log.d("NFTAdapter", "绑定NFT: " + item.getName());

        // Parse and display time information
        displayTimeInfo(nftHolder, item);

        // Set click listener: Show NFT details dialog
        nftHolder.itemView.setOnClickListener(v -> {
            showNftDetailDialog(v.getContext(), item);
        });

        // Use Glide to load images and optimize large image processing.
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            android.util.Log.d("NFTAdapter", "加载图片: " + item.getName() + ", URL长度: " + item.getImageUrl().length());

            // Ordinary image URL, loaded using Glide
            // Glide supports Base64 format (data:image/jpeg;base64,xxx) and HTTP URL.
            // List cards use compressed images to optimize performance and memory.
        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                    .override(300, 450) // Compress images in lists to optimize memory and performance.
                    .fitCenter() // Use fitCenter instead of centerCrop to avoid cropping.
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // Caching images
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
            // No image URL, placeholder shown
            android.util.Log.d("NFTAdapter", "使用占位符: " + item.getName());
            nftHolder.imageView.setImageResource(R.drawable.placeholder_image);
        }
    }

    /**
     * Display time information (material upload time, NFT minting time, holder address) / ??????????????NFT???????????
     */
    private void displayTimeInfo(ViewHolder holder, NFTItem item) {
        String uploadTime = item.getUploadTime();
        String mintTime = item.getMintTime();
        String ownerAddress = item.getOwnerAddress();
        String ownerDisplayName = item.getOwnerDisplayName();

        int visibleCount = 0;

        // Display material upload time
        if (uploadTime != null && !uploadTime.isEmpty()) {
            holder.uploadTimeText.setText(holder.uploadTimeText.getContext().getString(R.string.nft_view_adapter_material_upload) + " " + uploadTime);
            holder.uploadTimeText.setVisibility(View.VISIBLE);
            visibleCount++;
        } else {
            holder.uploadTimeText.setVisibility(View.GONE);
        }

        // Display NFT minting time
        if (mintTime != null && !mintTime.isEmpty()) {
            holder.mintTimeText.setText(holder.mintTimeText.getContext().getString(R.string.nft_view_adapter_nft_minted) + " " + mintTime);
            holder.mintTimeText.setVisibility(View.VISIBLE);
            visibleCount++;
        } else {
            holder.mintTimeText.setVisibility(View.GONE);
        }

        // Display owner address (shortened)
        if (ownerAddress != null && !ownerAddress.isEmpty()) {
            String shortAddress = shortenAddress(ownerAddress);
            holder.ownerAddressText.setText(holder.ownerAddressText.getContext().getString(R.string.nft_view_adapter_owner_address) + " " + shortAddress);
            holder.ownerAddressText.setVisibility(View.VISIBLE);
            visibleCount++;
        } else {
            holder.ownerAddressText.setVisibility(View.GONE);
        }

        // Display owner nickname
        if (ownerDisplayName != null && !ownerDisplayName.isEmpty() && !ownerDisplayName.equals("Anonymous")) {
            holder.ownerDisplayNameText.setText(holder.ownerDisplayNameText.getContext().getString(R.string.nft_view_adapter_owner_nickname) + " " + ownerDisplayName);
            holder.ownerDisplayNameText.setVisibility(View.VISIBLE);
            visibleCount++;
        } else {
            holder.ownerDisplayNameText.setVisibility(View.GONE);
        }

        // If there is time information, display the time area.
        holder.attributesLayout.setVisibility(visibleCount > 0 ? View.VISIBLE : View.GONE);
    }

    /**
     * Shorten address display (display first 6 digits + last 4 digits) / 缩短地址显示（显示前6位+后4位）
     */
    private String shortenAddress(String address) {
        if (address == null || address.length() < 10) {
            return address;
        }
        return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }

    /**
     * Show the NFT detail dialog (image plus two time attributes) / 显示NFT详情对话框（图片 + 2个时间属性）
     */
    private void showNftDetailDialog(android.content.Context context, NFTItem item) {
        // Create dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        android.view.View dialogView = android.view.LayoutInflater.from(context)
                .inflate(R.layout.dialog_nft_detail, null);
        builder.setView(dialogView);

        // Bind views
        android.widget.ImageView nftImageView = dialogView.findViewById(R.id.nftImageView);
        android.widget.LinearLayout attributesContainer = dialogView.findViewById(R.id.attributesContainer);

        // Load NFT images (Glide supports data URL and HTTP URL)
        // Use the original resolution in the details dialog box to ensure image quality.
        if (nftImageView != null && item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL) // Use original image size
                    .fitCenter() // Display the image completely without cropping
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // Cache original image
                    .into(nftImageView);
            android.util.Log.d("NFTAdapter", "详情对话框加载高清原图: " + item.getImageUrl());
        }

        // Show time attributes
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

        // Create dialog
        android.app.AlertDialog dialog = builder.create();

        dialog.show();
    }

    /**
     * Add attribute row to container / 添加属性行到容器
     */
    private void addAttributeRow(android.content.Context context, android.widget.LinearLayout container,
                                  String label, String value) {
        // Create attribute row
        android.widget.LinearLayout row = new android.widget.LinearLayout(context);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setPadding(0, 16, 0, 16);

        // Label
        android.widget.TextView labelView = new android.widget.TextView(context);
        labelView.setText(label + ": ");
        labelView.setTextSize(16);
        labelView.setTextColor(0xFF666666);
        android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        labelView.setLayoutParams(labelParams);

        // Value
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
                footerText.setText(R.string.loading);
                footerProgress.setVisibility(View.VISIBLE);
            } else if (hasMore) {
                footerText.setText(R.string.nft_view_adapter_pull_up_to_load_more);
                footerProgress.setVisibility(View.GONE);
            } else {
                footerText.setText(R.string.nft_view_adapter_end_of_list);
                footerProgress.setVisibility(View.GONE);
            }
        }
    }
}
