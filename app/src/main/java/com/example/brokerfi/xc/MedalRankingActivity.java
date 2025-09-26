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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medal_ranking);

        intView();
        intEvent();
        loadMedalRanking();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
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

    private void loadMedalRanking() {
        showLoading();

        new Thread(() -> {
            try {
                Log.d("MedalRanking", "开始加载排行榜数据");
                String response = MedalApiUtil.getMedalRanking();
                
                runOnUiThread(() -> {
                    hideLoading();
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
                    showEmptyState();
                });
            }
        }).start();
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
                rankingItem.setDisplayName(item.optString("displayName", "匿名用户"));
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
        builder.setTitle("🏆 勋章排行榜计算方法");
        builder.setMessage("📊 总分计算公式：\n" +
                "总分 = 金牌数量 × 3 + 银牌数量 × 2 + 铜牌数量 × 1\n\n" +
                "🥇 金牌 = 3分\n" +
                "🥈 银牌 = 2分\n" +
                "🥉 铜牌 = 1分\n\n" +
                "📈 排序规则：\n" +
                "1. 按总分从高到低排序\n" +
                "2. 总分相同时，按金牌数量排序\n" +
                "3. 金牌相同时，按银牌数量排序\n" +
                "4. 银牌相同时，按铜牌数量排序\n\n" +
                "🎨 用户信息展示：\n" +
                "根据个人意愿决定是否展示您的代表作与昵称\n" +
                "代表作需要管理员审核后才能在排行榜上显示");
        
        builder.setPositiveButton("我知道了", (dialog, which) -> {
            dialog.dismiss();
        });
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
}
