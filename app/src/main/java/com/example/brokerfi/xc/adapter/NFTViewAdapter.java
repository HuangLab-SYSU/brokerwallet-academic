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

public class NFTViewAdapter extends RecyclerView.Adapter<NFTViewAdapter.ViewHolder> {
    
    private List<NFTItem> nftList;
    
    public NFTViewAdapter(List<NFTItem> nftList) {
        this.nftList = nftList;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nft_view, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NFTItem item = nftList.get(position);
        
        holder.nameText.setText(item.getName());
        holder.descriptionText.setText(item.getDescription());
        
        // 使用Glide加载图片
        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(holder.imageView);
    }
    
    @Override
    public int getItemCount() {
        return nftList != null ? nftList.size() : 0;
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
}