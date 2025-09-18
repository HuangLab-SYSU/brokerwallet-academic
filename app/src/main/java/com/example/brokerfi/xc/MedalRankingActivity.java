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
    private TextView proofAndNftButton;
    private TextView nftViewButton;

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
        proofAndNftButton = findViewById(R.id.proofAndNftButton);
        nftViewButton = findViewById(R.id.nftViewButton);
        
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
        
        nftViewButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NFTViewActivity.class);
            startActivity(intent);
        });
    }

    private void loadMedalRanking() {
        loadingText.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                // 模拟从服务器获取排行榜数据
                // 实际实现中应该调用contract项目的API
                String response = MedalApiUtil.getMedalRanking();
                
                runOnUiThread(() -> {
                    loadingText.setVisibility(View.GONE);
                    if (response != null) {
                        parseRankingData(response);
                    } else {
                        // 显示空的排行榜样式，而不是错误信息
                        rankingList.clear();
                        adapter.notifyDataSetChanged();
                    }
                    recyclerView.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                Log.e("MedalRanking", "加载排行榜失败", e);
                runOnUiThread(() -> {
                    loadingText.setVisibility(View.GONE);
                    // 显示空的排行榜样式，而不是错误信息
                    rankingList.clear();
                    adapter.notifyDataSetChanged();
                    recyclerView.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void parseRankingData(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray data = jsonResponse.getJSONArray("data");
            
            rankingList.clear();
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.getJSONObject(i);
                MedalRankingItem rankingItem = new MedalRankingItem();
                rankingItem.setRank(i + 1);
                rankingItem.setAddress(item.getString("address"));
                rankingItem.setGold(item.getInt("gold"));
                rankingItem.setSilver(item.getInt("silver"));
                rankingItem.setBronze(item.getInt("bronze"));
                rankingItem.setTotal(item.getInt("total"));
                rankingList.add(rankingItem);
            }
            
            // 按总数排序
            Collections.sort(rankingList, (a, b) -> Integer.compare(b.getTotal(), a.getTotal()));
            
            // 更新排名
            for (int i = 0; i < rankingList.size(); i++) {
                rankingList.get(i).setRank(i + 1);
            }
            
            adapter.notifyDataSetChanged();
        } catch (JSONException e) {
            Log.e("MedalRanking", "解析数据失败", e);
            errorText.setVisibility(View.VISIBLE);
            errorText.setText("数据解析失败");
        }
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
        private String address;
        private int gold;
        private int silver;
        private int bronze;
        private int total;

        // Getters and Setters
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public int getGold() { return gold; }
        public void setGold(int gold) { this.gold = gold; }
        public int getSilver() { return silver; }
        public void setSilver(int silver) { this.silver = silver; }
        public int getBronze() { return bronze; }
        public void setBronze(int bronze) { this.bronze = bronze; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
    }
}
