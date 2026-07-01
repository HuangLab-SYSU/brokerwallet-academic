package com.example.brokerfi.medal;

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
import com.example.brokerfi.medal.adapter.MedalRankingAdapter;
import com.example.brokerfi.main.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.example.brokerfi.main.MainActivity;
import com.example.brokerfi.proof.ProofAndNFTActivity;
import com.example.brokerfi.send.SendActivity;
import com.example.brokerfi.xc.MyCenterActivity;


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

    // Pull down to refresh related
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private boolean isRefreshing = false;

    // Static cache, used to restore data when the Activity is rebuilt.
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
        // Restore leaderboard cache
        restoreRankingCache();

        // If there is cache, do not reload; if there is no cache, load.
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
     * Set pull-down refresh / 设置下拉刷新
     */
    private void setupPullToRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                Log.d("MedalRanking", "用户下拉刷新排行榜");
                isRefreshing = true;
                loadMedalRanking();
            });

            // Set refresh animation color
            swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            );
        }
    }

    private void loadMedalRanking() {
        // If it is not a pull-down refresh, the loading status is displayed.
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
     * Restore leaderboard cache / 恢复排行榜缓存
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
     * Save leaderboard cache / 保存排行榜缓存
     */
    private void saveRankingCache() {
        if (rankingList != null && !rankingList.isEmpty()) {
            cachedRankingList = new ArrayList<>(rankingList);
            Log.d("MedalRanking", "Saved ranking cache, total: " + cachedRankingList.size());
        }
    }

    /**
     * Stop pull-down refresh animation / 停止下拉刷新动画
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
                // Sort by total score (the backend should already sort it, but just to be safe)
                Collections.sort(rankingList, (a, b) -> Integer.compare(b.getTotalMedalScore(), a.getTotalMedalScore()));

                // Update ranking
                for (int i = 0; i < rankingList.size(); i++) {
                    rankingList.get(i).setRank(i + 1);
                }

                adapter.notifyDataSetChanged();
                showRankingList();

                // Save leaderboard cache
                saveRankingCache();
            }
        } catch (JSONException e) {
            Log.e("MedalRanking", "解析数据失败", e);
            showEmptyState();
        }
    }

    private void showGlobalStatsMenu() {
        // Jump directly to the global statistics interface
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

        // Format wallet address display
        public String getFormattedAddress() {
            if (walletAddress == null || walletAddress.length() < 10) {
                return walletAddress;
            }
            return walletAddress.substring(0, 6) + "..." + walletAddress.substring(walletAddress.length() - 4);
        }
    }

    /**
     * Display the calculation method description dialog box / 显示计算方法说明对话框
     */
    private void showCalculationHelpDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.medal_ranking_title_calculation);
        builder.setMessage(getString(R.string.medal_ranking_message_score_title) + "\n" +
                getString(R.string.medal_ranking_message_score_formula) + "\n\n" +
                getString(R.string.medal_ranking_message_gold_points) + "\n" +
                getString(R.string.medal_ranking_message_silver_points) + "\n" +
                getString(R.string.medal_ranking_message_bronze_point) + "\n\n" +
                getString(R.string.medal_ranking_message_ranking_rules) + "\n" +
                getString(R.string.medal_ranking_message_sort_rule) + "\n" +
                getString(R.string.medal_ranking_message_tie_gold) + "\n" +
                getString(R.string.medal_ranking_message_tie_silver) + "\n" +
                getString(R.string.medal_ranking_message_tie_bronze) + "\n\n" +
                getString(R.string.medal_ranking_message_profile_display) + "\n" +
                getString(R.string.medal_ranking_message_display_work) + "\n" +
                getString(R.string.medal_ranking_message_work_approval) + "\n\n" +
                "━━━━━━━━━━━━━━━━━━━\n" +
                getString(R.string.medal_ranking_message_earn_rewards) + "\n\n" +
                getString(R.string.medal_ranking_message_proof_rewards) + "\n" +
                getString(R.string.medal_ranking_message_medal_reward) + "\n" +
                getString(R.string.medal_ranking_message_nft_reward) + "\n" +
                getString(R.string.medal_ranking_message_bkc_rewards) + "\n\n" +
                getString(R.string.medal_ranking_message_submit_now));

        builder.setPositiveButton(R.string.medal_ranking_button_submit_now, (dialog, which) -> {
            dialog.dismiss();
            // Jump to proof submission page
            Intent intent = new Intent(this, ProofAndNFTActivity.class);
            startActivity(intent);
        });

        builder.setNegativeButton(R.string.medal_ranking_button_later, (dialog, which) -> {
            dialog.dismiss();
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
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
