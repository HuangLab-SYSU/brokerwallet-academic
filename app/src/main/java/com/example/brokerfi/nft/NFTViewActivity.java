package com.example.brokerfi.nft;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.brokerfi.R;
import com.example.brokerfi.core.config.ApiConfig;
import com.example.brokerfi.nft.adapter.NFTViewAdapter;
import com.example.brokerfi.main.menu.NavigationHelper;

import java.util.ArrayList;
import java.util.List;
import com.example.brokerfi.nft.model.NFT;


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

        // Simulate loading NFT data
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate network latency

                runOnUiThread(() -> {
                    loadingText.setVisibility(View.GONE);
                    // Add some mock data
                    nftList.add(new NFTItem("NFT #1", "描述1", ApiConfig.NFT_PLACEHOLDER_IMAGE_URL + "+1"));
                    nftList.add(new NFTItem("NFT #2", "描述2", ApiConfig.NFT_PLACEHOLDER_IMAGE_URL + "+2"));
                    nftList.add(new NFTItem("NFT #3", "描述3", ApiConfig.NFT_PLACEHOLDER_IMAGE_URL + "+3"));

                    adapter.notifyDataSetChanged();
                    recyclerView.setVisibility(View.VISIBLE);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loadingText.setVisibility(View.GONE);
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText(R.string.activity_nft_view_load_failed);
                });
            }
        }).start();
    }

    public static class NFTItem {
        private String name;
        private String description;
        private String imageUrl;
        private String attributes;        // NFT properties (JSON format)
        private String mintTime;          // NFT minting time
        private String minter;            // Founder's address
        private String uploadTime;        // Material upload time
        private String ownerAddress;      // holder address
        private String ownerDisplayName;  // holder's name

        public NFTItem(String name, String description, String imageUrl) {
            this.name = name;
            this.description = description;
            this.imageUrl = imageUrl;
            this.attributes = "";
            this.mintTime = "";
            this.minter = "";
            this.uploadTime = "";
            this.ownerAddress = "";
            this.ownerDisplayName = "";
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getAttributes() { return attributes; }
        public void setAttributes(String attributes) { this.attributes = attributes; }
        public String getMintTime() { return mintTime; }
        public void setMintTime(String mintTime) { this.mintTime = mintTime; }
        public String getMinter() { return minter; }
        public void setMinter(String minter) { this.minter = minter; }
        public String getUploadTime() { return uploadTime; }
        public void setUploadTime(String uploadTime) { this.uploadTime = uploadTime; }
        public String getOwnerAddress() { return ownerAddress; }
        public void setOwnerAddress(String ownerAddress) { this.ownerAddress = ownerAddress; }
        public String getOwnerDisplayName() { return ownerDisplayName; }
        public void setOwnerDisplayName(String ownerDisplayName) { this.ownerDisplayName = ownerDisplayName; }
    }
    @Override
    public void onBackPressed() {
        if (navigationHelper != null && navigationHelper.isPopupVisible()) {
            navigationHelper.hidePopup();
        } else {
            super.onBackPressed();
        }
    }

}
