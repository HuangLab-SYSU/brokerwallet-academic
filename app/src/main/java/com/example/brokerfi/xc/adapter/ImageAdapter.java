package com.example.brokerfi.xc.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.brokerfi.R;
import com.example.brokerfi.xc.ImagePreviewActivity;

import java.util.ArrayList;
import java.util.List;
import com.example.brokerfi.common.ui.Holder;


public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private final Context context;
    private final List<String> imageUrlList;

    public ImageAdapter(Context context, List<String> imageUrlList) {
        this.context = context;
        this.imageUrlList = imageUrlList;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_img_display, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String url = imageUrlList.get(position);
        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.placeholder_image)
                .into(holder.ivImage);
        // 点击查看大图
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ImagePreviewActivity.class);
            intent.putStringArrayListExtra("imageList", (ArrayList<String>) imageUrlList);
            intent.putExtra("position", position);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return imageUrlList == null ? 0 : imageUrlList.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
        }
    }
}
