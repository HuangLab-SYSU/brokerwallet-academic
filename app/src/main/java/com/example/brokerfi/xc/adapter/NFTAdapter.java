package com.example.brokerfi.xc.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.model.NFT;

import java.util.List;

public class NFTAdapter extends RecyclerView.Adapter<NFTAdapter.ViewHolder> {
    private List<NFT> nftData;
    private int lastSelectedPosition = -1;
    private final boolean fromBuyNFTsActivity;

    public NFTAdapter(List<NFT> localData, boolean fromBuyNFTsActivity) {
        this.nftData = localData;
        this.fromBuyNFTsActivity = fromBuyNFTsActivity;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvId,tvName, tvShares,tvPrice, tvSaleStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.textView_id);
            ivImage = itemView.findViewById(R.id.iv_thumbnail);
            tvName = itemView.findViewById(R.id.tv_name);
            tvShares = itemView.findViewById(R.id.tv_shares);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvSaleStatus = itemView.findViewById(R.id.tv_sale_status);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.nft_holder, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NFT item = nftData.get(position);
        holder.tvId.setText(String.valueOf(position + 1));

        if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
            try {
                byte[] imageBytes = Base64.decode(item.getImageBase64(), Base64.NO_WRAP);
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                holder.ivImage.setImageBitmap(bitmap);
            } catch (IllegalArgumentException e) {
                Log.e("BASE64_ERROR", "Base64格式错误: " + e.getMessage());
            }
        }
        holder.tvName.setText(item.getName());
        holder.tvShares.setText(item.getShares().toString());
        holder.tvPrice.setText(item.isListed() ? String.format("%s BKC", item.getPrice().toString()) : "      ——");

        if (fromBuyNFTsActivity) {
            String account = item.getAccountNumber() != null ? item.getAccountNumber() : "";
            int length = account.length();

            String compactDisplay = (length >= 6) 
                ? account.substring(0, 3) + "…" + account.substring(length - 3)
                : account;

            holder.tvSaleStatus.setText(compactDisplay);
        } else {
            holder.tvSaleStatus.setText(item.isListed() ? "Listed" : "Unlisted");
            holder.tvSaleStatus.setTextColor(item.isListed() ? 0xFF4CAF50 : 0xFFF44336);
        }

        holder.itemView.setSelected(position == lastSelectedPosition);
        holder.itemView.setBackgroundResource(
                position == lastSelectedPosition ? R.drawable.custom_nft_selected_bg
                        : R.drawable.custom_nft_background
        );

        holder.itemView.setOnClickListener(v -> {
            int prevSelected = lastSelectedPosition;
            int currentPosition = holder.getAdapterPosition();
            
            if (currentPosition == RecyclerView.NO_POSITION) return;

            if (lastSelectedPosition == currentPosition) {
                lastSelectedPosition = -1;
            } else {
                lastSelectedPosition = currentPosition;
            }

            if (prevSelected != -1) {
                notifyItemChanged(prevSelected);
            }
            if (lastSelectedPosition != -1) {
                notifyItemChanged(lastSelectedPosition);
            }
        });
    }

    public NFT getSelectedItem() {
        return lastSelectedPosition != -1 ? nftData.get(lastSelectedPosition) : null;
    }

    public int getSelectedPosition() {
        return lastSelectedPosition;
    }

    public void clearSelection() {
        if (lastSelectedPosition != -1) {
            int prev = lastSelectedPosition;
            lastSelectedPosition = -1;
            notifyItemChanged(prev);
        }
    }

    @Override
    public int getItemCount() {
        return nftData.size();
    }


}
