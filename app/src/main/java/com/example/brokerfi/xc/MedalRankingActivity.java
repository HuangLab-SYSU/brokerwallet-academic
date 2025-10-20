package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.adapter.MedalRankingAdapter;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MedalRankingActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    private RecyclerView recyclerView;
    private MedalRankingAdapter adapter;
    private List<MedalRankingItem> rankingList;
    private TextView loadingText;
    private TextView errorText;
    private LinearLayout emptyStateLayout;
    private TextView proofAndNftButton;
    private TextView myCenterButton;
    private TextView globalStatsButton;
    private TextView helpButton;
    
    // 下拉刷新相关
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private boolean isRefreshing = false;
    
    // 静态缓存，用于Activity重建时恢复数据
    private static List<MedalRankingItem> cachedRankingList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medal_ranking);

        intView();
        intEvent();
        setupPullToRefresh();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MedalRankingActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        // 恢复排行榜缓存
        restoreRankingCache();
        
        // 如果有缓存，不重新加载；如果没有缓存，才加载
        if (rankingList.isEmpty()) {
            loadMedalRanking();
        } else {
            Log.d("MedalRanking", "Using cached ranking data, total: " + rankingList.size());
            recyclerView.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        loadingText = findViewById(R.id.loadingText);
        errorText = findViewById(R.id.errorText);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        proofAndNftButton = findViewById(R.id.proofAndNftButton);
        myCenterButton = findViewById(R.id.myCenterButton);
        globalStatsButton = findViewById(R.id.globalStatsButton);
        helpButton = findViewById(R.id.helpButton);
        
        rankingList = new ArrayList<>();
        adapter = new MedalRankingAdapter(rankingList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);
        
        proofAndNftButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProofAndNFTActivity.class);
            startActivity(intent);
        });
        
        myCenterButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyCenterActivity.class);
            startActivity(intent);
        });

        globalStatsButton.setOnClickListener(v -> {
            showGlobalStatsMenu();
        });
        
        helpButton.setOnClickListener(v -> {
            showCalculationHelpDialog();
        });
    }

    /**
     * 设置下拉刷新
     */
    private void setupPullToRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                Log.d("MedalRanking", "用户下拉刷新排行榜");
                isRefreshing = true;
                loadMedalRanking();
            });
            
            // 设置刷新动画颜色
            swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            );
        }
    }
    
    private void loadMedalRanking() {
        // 如果不是下拉刷新，显示加载状态
        if (!isRefreshing) {
            showLoading();
        }

        new Thread(() -> {
            try {
                Log.d("MedalRanking", "开始加载排行榜数据");
                String response = MedalApiUtil.getMedalRanking();
                
                runOnUiThread(() -> {
                    hideLoading();
                    stopRefreshing();
                    if (response != null && !response.trim().isEmpty()) {
                        Log.d("MedalRanking", "收到排行榜数据: " + response);
                        parseRankingData(response);
                    } else {
                        Log.d("MedalRanking", "排行榜数据为空，显示空状态");
                        showEmptyState();
                    }
                });
            } catch (Exception e) {
                Log.e("MedalRanking", "加载排行榜失败", e);
                runOnUiThread(() -> {
                    hideLoading();
                    stopRefreshing();
                    showEmptyState();
                });
            }
        }).start();
    }
    
    /**
     * 恢复排行榜缓存
     */
    private void restoreRankingCache() {
        if (cachedRankingList != null && !cachedRankingList.isEmpty()) {
            rankingList.clear();
            rankingList.addAll(cachedRankingList);
            Log.d("MedalRanking", "Restored cached ranking data, total: " + rankingList.size());
        } else {
            Log.d("MedalRanking", "No cached ranking data available");
        }
    }
    
    /**
     * 保存排行榜缓存
     */
    private void saveRankingCache() {
        if (rankingList != null && !rankingList.isEmpty()) {
            cachedRankingList = new ArrayList<>(rankingList);
            Log.d("MedalRanking", "Saved ranking cache, total: " + cachedRankingList.size());
        }
    }
    
    /**
     * 停止下拉刷新动画
     */
    private void stopRefreshing() {
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        isRefreshing = false;
    }

    private void showLoading() {
        loadingText.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingText.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        errorText.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        rankingList.clear();
        adapter.notifyDataSetChanged();
    }

    private void showRankingList() {
        errorText.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void parseRankingData(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            
            if (!jsonResponse.getBoolean("success")) {
                Log.w("MedalRanking", "API返回失败: " + jsonResponse.optString("message"));
                showEmptyState();
                return;
            }
            
            JSONArray data = jsonResponse.getJSONArray("data");
            
            rankingList.clear();
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.getJSONObject(i);
                MedalRankingItem rankingItem = new MedalRankingItem();
                rankingItem.setRank(i + 1);
                rankingItem.setWalletAddress(item.getString("walletAddress"));
                rankingItem.setDisplayName(item.optString("displayName", "Anonymous"));
                rankingItem.setGoldMedals(item.getInt("goldMedals"));
                rankingItem.setSilverMedals(item.getInt("silverMedals"));
                rankingItem.setBronzeMedals(item.getInt("bronzeMedals"));
                rankingItem.setTotalMedalScore(item.getInt("totalMedalScore"));
                rankingItem.setRepresentativeWork(item.optString("representativeWork", ""));
                rankingItem.setShowRepresentativeWork(item.optBoolean("showRepresentativeWork", false));
                rankingList.add(rankingItem);
            }
            
            if (rankingList.isEmpty()) {
                showEmptyState();
            } else {
                // 按总分排序（后端应该已经排序了，但保险起见）
                Collections.sort(rankingList, (a, b) -> Integer.compare(b.getTotalMedalScore(), a.getTotalMedalScore()));
                
                // 更新排名
                for (int i = 0; i < rankingList.size(); i++) {
                    rankingList.get(i).setRank(i + 1);
                }
                
                adapter.notifyDataSetChanged();
                showRankingList();
                
                // 保存排行榜缓存
                saveRankingCache();
            }
        } catch (JSONException e) {
            Log.e("MedalRanking", "解析数据失败", e);
            showEmptyState();
        }
    }

    private void showGlobalStatsMenu() {
        // 直接跳转到全局统计界面
        Intent intent = new Intent(this, GlobalStatsActivity.class);
        startActivity(intent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(
                requestCode, resultCode, data
        );
        if (intentResult.getContents() != null) {
            String scannedData = intentResult.getContents();
            Intent intent = new Intent(this, SendActivity.class);
            intent.putExtra("scannedData", scannedData);
            startActivity(intent);
        }
    }

    public static class MedalRankingItem {
        private int rank;
        private String walletAddress;
        private String displayName;
        private int goldMedals;
        private int silverMedals;
        private int bronzeMedals;
        private int totalMedalScore;
        private String representativeWork;
        private boolean showRepresentativeWork;

        // Getters and Setters
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        
        public String getWalletAddress() { return walletAddress; }
        public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public int getGoldMedals() { return goldMedals; }
        public void setGoldMedals(int goldMedals) { this.goldMedals = goldMedals; }
        
        public int getSilverMedals() { return silverMedals; }
        public void setSilverMedals(int silverMedals) { this.silverMedals = silverMedals; }
        
        public int getBronzeMedals() { return bronzeMedals; }
        public void setBronzeMedals(int bronzeMedals) { this.bronzeMedals = bronzeMedals; }
        
        public int getTotalMedalScore() { return totalMedalScore; }
        public void setTotalMedalScore(int totalMedalScore) { this.totalMedalScore = totalMedalScore; }
        
        public String getRepresentativeWork() { return representativeWork; }
        public void setRepresentativeWork(String representativeWork) { this.representativeWork = representativeWork; }
        
        public boolean isShowRepresentativeWork() { return showRepresentativeWork; }
        public void setShowRepresentativeWork(boolean showRepresentativeWork) { this.showRepresentativeWork = showRepresentativeWork; }

        // 格式化钱包地址显示
        public String getFormattedAddress() {
            if (walletAddress == null || walletAddress.length() < 10) {
                return walletAddress;
            }
            return walletAddress.substring(0, 6) + "..." + walletAddress.substring(walletAddress.length() - 4);
        }
    }
    
    /**
     * 显示计算方法说明对话框
     */
    private void showCalculationHelpDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("🏆 Medal Ranking Calculation");
        builder.setMessage("📊 Score Calculation Formula:\n" +
                "Total Score = Gold × 3 + Silver × 2 + Bronze × 1\n\n" +
                "🥇 Gold Medal = 3 points\n" +
                "🥈 Silver Medal = 2 points\n" +
                "🥉 Bronze Medal = 1 point\n\n" +
                "📈 Ranking Rules:\n" +
                "1. Sorted by total score (descending)\n" +
                "2. If tied, sorted by gold medals\n" +
                "3. If tied, sorted by silver medals\n" +
                "4. If tied, sorted by bronze medals\n\n" +
                "🎨 User Profile Display:\n" +
                "Choose to display your representative work & nickname\n" +
                "Representative work requires admin approval\n\n" +
                "━━━━━━━━━━━━━━━━━━━\n" +
                "🎁 Take Action, Earn Rewards!\n\n" +
                "Submit your proof to receive:\n" +
                "🏅 Honor Medals - Showcase your contribution\n" +
                "🖼️ Exclusive NFT - Permanently stored on blockchain\n" +
                "💰 BKC Tokens - Real rewards\n\n" +
                "What are you waiting for? Submit now!");
        
        builder.setPositiveButton("Submit Now", (dialog, which) -> {
            dialog.dismiss();
            // Jump to proof submission page
            Intent intent = new Intent(this, ProofAndNFTActivity.class);
            startActivity(intent);
        });
        
        builder.setNegativeButton("Later", (dialog, which) -> {
            dialog.dismiss();
        });
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
}
