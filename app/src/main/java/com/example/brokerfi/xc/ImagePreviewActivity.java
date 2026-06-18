package com.example.brokerfi.xc;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.brokerfi.R;

import java.util.ArrayList;
import java.util.List;
import com.example.brokerfi.common.ui.Holder;


public class ImagePreviewActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ArrayList<String> imageList;
    private int currentPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        // 全屏黑色背景
        getWindow().setBackgroundDrawableResource(android.R.color.black);

        viewPager = findViewById(R.id.view_pager);
        imageList = getIntent().getStringArrayListExtra("imageList");
        currentPosition = getIntent().getIntExtra("position", 0);

        ImagePagerAdapter adapter = new ImagePagerAdapter(imageList);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);

        // 点击屏幕关闭
        viewPager.setOnClickListener(v -> finish());
    }

    private class ImagePagerAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {
        private final List<String> imageList;

        public ImagePagerAdapter(List<String> imageList) {
            this.imageList = imageList;
        }

        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(ImagePreviewActivity.this);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
            Glide.with(ImagePreviewActivity.this)
                    .load(imageList.get(position))
                    .into((ImageView) holder.itemView);
        }

        @Override
        public int getItemCount() {
            return imageList.size();
        }

        class ImageViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            public ImageViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
