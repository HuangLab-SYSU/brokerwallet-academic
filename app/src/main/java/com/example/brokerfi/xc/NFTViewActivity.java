package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.adapter.NFTViewAdapter;
import com.example.brokerfi.xc.menu.NavigationHelper;

import java.util.ArrayList;
import java.util.List;

public class NFTViewActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    private RecyclerView recyclerView;
    private NFTViewAdapter adapter;
    private List<NFTItem> nftList;
    private TextView loadingText;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nft_view);

        intView();
        intEvent();
        loadNFTs();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        recyclerView = findViewById(R.id.recyclerView);
        loadingText = findViewById(R.id.loadingText);
        errorText = findViewById(R.id.errorText);
        
        nftList = new ArrayList<>();
        adapter = new NFTViewAdapter(nftList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);
    }

    private void loadNFTs() {
        loadingText.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        // 模拟加载NFT数据
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 模拟网络延迟
                
                runOnUiThread(() -> {
                    loadingText.setVisibility(View.GONE);
                    // 添加一些模拟数据
                    nftList.add(new NFTItem("NFT #1", "描述1", "https://example.com/image1.jpg"));
                    nftList.add(new NFTItem("NFT #2", "描述2", "https://example.com/image2.jpg"));
                    nftList.add(new NFTItem("NFT #3", "描述3", "https://example.com/image3.jpg"));
                    
                    adapter.notifyDataSetChanged();
                    recyclerView.setVisibility(View.VISIBLE);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loadingText.setVisibility(View.GONE);
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText("加载失败");
                });
            }
        }).start();
    }

    public static class NFTItem {
        private String name;
        private String description;
        private String imageUrl;

        public NFTItem(String name, String description, String imageUrl) {
            this.name = name;
            this.description = description;
            this.imageUrl = imageUrl;
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }
}