package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.adapter.NFTViewAdapter;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.example.brokerfi.xc.net.ABIUtils;
import com.example.brokerfi.xc.StorageUtil;
import com.example.brokerfi.xc.SecurityUtil;
import org.json.JSONException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局统计界面
 * 显示全局勋章统计和所有NFT
 */
public class GlobalStatsActivity extends AppCompatActivity {
    
    private static final String TAG = "GlobalStats";
    
    // UI组件
    private TextView titleText;
    private LinearLayout globalMedalStatsLayout;
    private TextView totalUsersText;
    private TextView usersWithMedalsText;
    private TextView totalGoldMedalsText;
    private TextView totalSilverMedalsText;
    private TextView totalBronzeMedalsText;
    private TextView highestScoreText;
    private TextView topUserText;
    private TextView medalLoadingText;
    private TextView medalErrorText;
    
    private RecyclerView nftRecyclerView;
    private NFTViewAdapter nftAdapter;
    private List<NFTViewActivity.NFTItem> nftList;
    private TextView nftLoadingText;
    private TextView nftErrorText;
    private LinearLayout nftEmptyStateLayout;
    
    // NFT分页相关
    private int nftCurrentPage = 0;
    private int nftPageSize = 5;
    private boolean nftHasMore = true;
    private boolean nftLoadingMore = false;
    
    // 下拉刷新相关变量
    private boolean isDragging = false;
    private boolean isRefreshing = false;
    private boolean isFooterVisible = false;
    private float startY = 0;
    private float currentY = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_stats);
        
        initView();
        initEvent();
        loadGlobalMedalStats();
        loadAllNfts();
    }
    
    private void initView() {
        titleText = findViewById(R.id.titleText);
        globalMedalStatsLayout = findViewById(R.id.globalMedalStatsLayout);
        totalUsersText = findViewById(R.id.totalUsersText);
        usersWithMedalsText = findViewById(R.id.usersWithMedalsText);
        totalGoldMedalsText = findViewById(R.id.totalGoldMedalsText);
        totalSilverMedalsText = findViewById(R.id.totalSilverMedalsText);
        totalBronzeMedalsText = findViewById(R.id.totalBronzeMedalsText);
        highestScoreText = findViewById(R.id.highestScoreText);
        topUserText = findViewById(R.id.topUserText);
        medalLoadingText = findViewById(R.id.medalLoadingText);
        medalErrorText = findViewById(R.id.medalErrorText);
        
        nftRecyclerView = findViewById(R.id.nftRecyclerView);
        nftLoadingText = findViewById(R.id.nftLoadingText);
        nftErrorText = findViewById(R.id.nftErrorText);
        nftEmptyStateLayout = findViewById(R.id.nftEmptyStateLayout);
        
        nftList = new ArrayList<>();
        nftAdapter = new NFTViewAdapter(nftList);
        nftRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        nftRecyclerView.setAdapter(nftAdapter);
        
        // 设置NFT点击监听
        nftAdapter.setOnItemClickListener((item, position) -> {
            Log.d("GlobalStats", "NFT被点击: " + item.getName());
            showNftDetailDialog(item);
        });
        
        // 设置下拉刷新功能
        setupNftPullRefresh();
    }
    
    private void initEvent() {
        // 设置导航
        ImageView menu = findViewById(R.id.menu);
        ImageView notificationBtn = findViewById(R.id.notificationBtn);
        RelativeLayout action_bar = findViewById(R.id.action_bar);
        NavigationHelper navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);
    }
    
    /**
     * 加载全局勋章统计
     */
    private void loadGlobalMedalStats() {
        showMedalLoading();
        
        new Thread(() -> {
            try {
                Log.d("GlobalStats", "开始加载全局勋章统计");
                String response = MedalApiUtil.getGlobalStats();
                
                runOnUiThread(() -> {
                    hideMedalLoading();
                    if (response != null && !response.trim().isEmpty()) {
                        Log.d("GlobalStats", "收到全局统计数据: " + response);
                        parseGlobalStatsData(response);
                    } else {
                        Log.d("GlobalStats", "全局统计数据为空");
                        showMedalError();
                    }
                });
            } catch (Exception e) {
                Log.e("GlobalStats", "加载全局统计失败", e);
                runOnUiThread(() -> {
                    hideMedalLoading();
                    showMedalError();
                });
            }
        }).start();
    }
    
    /**
     * 解析全局统计数据
     */
    private void parseGlobalStatsData(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            
            if (!jsonResponse.getBoolean("success")) {
                Log.w("GlobalStats", "API返回失败: " + jsonResponse.optString("message"));
                showMedalError();
                return;
            }
            
            JSONObject data = jsonResponse.getJSONObject("data");
            
            // 更新UI
            totalUsersText.setText(String.valueOf(data.optInt("totalUsers", 0)));
            usersWithMedalsText.setText(String.valueOf(data.optInt("usersWithMedals", 0)));
            totalGoldMedalsText.setText(String.valueOf(data.optInt("totalGoldMedals", 0)));
            totalSilverMedalsText.setText(String.valueOf(data.optInt("totalSilverMedals", 0)));
            totalBronzeMedalsText.setText(String.valueOf(data.optInt("totalBronzeMedals", 0)));
            highestScoreText.setText(String.valueOf(data.optInt("highestScore", 0)));
            
            String topUser = data.optString("topUserDisplayName", "暂无");
            if (topUser.equals("null") || topUser.isEmpty()) {
                topUser = "暂无";
            }
            topUserText.setText(topUser);
            
            globalMedalStatsLayout.setVisibility(View.VISIBLE);
            Log.d("GlobalStats", "全局勋章统计加载完成");
            
        } catch (JSONException e) {
            Log.e("GlobalStats", "解析全局统计数据失败", e);
            showMedalError();
        }
    }
    
    /**
     * 加载更多NFT
     */
    private void loadMoreNfts() {
        if (nftLoadingMore || !nftHasMore) {
            Log.d("GlobalStats", "无法加载更多NFT - 正在加载: " + nftLoadingMore + ", 还有更多: " + nftHasMore);
            return;
        }
        
        nftLoadingMore = true;
        nftCurrentPage++;
        
        Log.d("GlobalStats", "开始加载更多NFT，页码: " + nftCurrentPage);
        
        new Thread(() -> {
            try {
                String response = MedalApiUtil.getAllNfts(nftCurrentPage, nftPageSize);
                Log.d("GlobalStats", "加载更多NFT响应: " + (response != null ? response : "null"));
                
                // 如果MedalApiUtil返回null，尝试直接HTTP调用
                if (response == null || response.trim().isEmpty()) {
                    Log.d("GlobalStats", "MedalApiUtil返回空，尝试直接HTTP调用");
                    try {
                        String testUrl = "http://127.0.0.1:5000/api/blockchain/nft/all?page=" + nftCurrentPage + "&size=" + nftPageSize;
                        java.net.URL url = new java.net.URL(testUrl);
                        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(15000);
                        connection.setReadTimeout(30000);
                        
                        int responseCode = connection.getResponseCode();
                        if (responseCode == 200) {
                            java.io.BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(connection.getInputStream()));
                            StringBuilder directResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                directResponse.append(line);
                            }
                            reader.close();
                            response = directResponse.toString();
                            Log.d("GlobalStats", "直接HTTP调用成功，响应: " + response);
                        }
                    } catch (Exception e) {
                        Log.e("GlobalStats", "直接HTTP调用异常: " + e.getMessage());
                    }
                }
                
                final String finalResponse = response;
                runOnUiThread(() -> {
                    nftLoadingMore = false;
                    if (finalResponse != null && !finalResponse.trim().isEmpty()) {
                        parseMoreNftData(finalResponse);
                    } else {
                        Log.d("GlobalStats", "没有更多NFT数据");
                        nftHasMore = false;
                        nftAdapter.setHasMore(false);
                    }
                    completePullRefresh();
                });
            } catch (Exception e) {
                Log.e("GlobalStats", "加载更多NFT失败", e);
                runOnUiThread(() -> {
                    nftLoadingMore = false;
                    completePullRefresh();
                });
            }
        }).start();
    }
    
    /**
     * 加载所有NFT
     */
    private void loadAllNfts() {
        showNftLoading();
        
        new Thread(() -> {
            try {
                Log.d("GlobalStats", "开始加载所有NFT");
                
                // 直接测试API调用
                String testUrl = "http://127.0.0.1:5000/api/blockchain/nft/all?page=" + nftCurrentPage + "&size=" + nftPageSize;
                Log.d("GlobalStats", "测试URL: " + testUrl);
                
                String response = MedalApiUtil.getAllNfts(nftCurrentPage, nftPageSize);
                Log.d("GlobalStats", "MedalApiUtil响应: " + (response != null ? response : "null"));
                Log.d("GlobalStats", "响应长度: " + (response != null ? response.length() : "null"));
                Log.d("GlobalStats", "响应是否为空: " + (response == null || response.trim().isEmpty()));
                
                // 如果MedalApiUtil返回null，尝试直接HTTP调用
                if (response == null || response.trim().isEmpty()) {
                    Log.d("GlobalStats", "MedalApiUtil返回空，尝试直接HTTP调用");
                    try {
                        java.net.URL url = new java.net.URL(testUrl);
                        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(15000);
                        connection.setReadTimeout(30000);
                        
                        int responseCode = connection.getResponseCode();
                        Log.d("GlobalStats", "直接HTTP调用响应码: " + responseCode);
                        
                        if (responseCode == 200) {
                            java.io.BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(connection.getInputStream()));
                            StringBuilder directResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                directResponse.append(line);
                            }
                            reader.close();
                            response = directResponse.toString();
                            Log.d("GlobalStats", "直接HTTP调用成功，响应: " + response);
                        } else {
                            Log.e("GlobalStats", "直接HTTP调用失败，响应码: " + responseCode);
                        }
                    } catch (Exception e) {
                        Log.e("GlobalStats", "直接HTTP调用异常: " + e.getMessage());
                    }
                }
                
                // 使用final变量
                final String finalResponse = response;
                runOnUiThread(() -> {
                    hideNftLoading();
                    if (finalResponse != null && !finalResponse.trim().isEmpty()) {
                        Log.d("GlobalStats", "收到NFT数据: " + finalResponse);
                        parseNftData(finalResponse);
                    } else {
                        Log.d("GlobalStats", "NFT数据为空，显示空状态");
                        showNftEmptyState();
                    }
                    // 完成下拉刷新
                    completePullRefresh();
                });
            } catch (Exception e) {
                Log.e("GlobalStats", "加载NFT失败", e);
                runOnUiThread(() -> {
                    hideNftLoading();
                    showNftError();
                });
            }
        }).start();
    }
    
    /**
     * 解析更多NFT数据（分页加载）
     */
    private void parseMoreNftData(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            Log.d("GlobalStats", "解析更多NFT数据: " + response);
            
            JSONArray nfts = jsonResponse.optJSONArray("nfts");
            if (nfts == null || nfts.length() == 0) {
                Log.d("GlobalStats", "没有更多NFT数据");
                nftHasMore = false;
                nftAdapter.setHasMore(false);
                return;
            }
            
            Log.d("GlobalStats", "找到更多NFT，数量: " + nfts.length());
            
            // 添加新的NFT到现有列表
            for (int i = 0; i < nfts.length(); i++) {
                try {
                    JSONObject nft = nfts.getJSONObject(i);
                    String name = nft.optString("name", "NFT #" + nft.optString("tokenId", ""));
                    String description = nft.optString("description", "暂无描述");
                    String imageUrl = nft.optString("imageUrl", "");
                    
                    // 检查图片URL是否为base64数据，如果是则进行优化处理
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        if (imageUrl.startsWith("data:image/")) {
                            Log.d("GlobalStats", "检测到base64图片数据，进行优化处理");
                            String optimizedUrl = optimizeBase64Image(imageUrl);
                            if (optimizedUrl != null) {
                                imageUrl = optimizedUrl;
                                Log.d("GlobalStats", "Base64图片优化成功，使用优化后的URL");
                            } else {
                                Log.d("GlobalStats", "Base64图片优化失败，使用占位符");
                                imageUrl = null;
                            }
                        } else {
                            Log.d("GlobalStats", "使用原始图片URL");
                        }
                    } else {
                        Log.d("GlobalStats", "图片URL为空，使用占位符");
                        imageUrl = null;
                    }
                    
                    Log.d("GlobalStats", "添加更多NFT: " + name + ", 图片URL: " + (imageUrl != null ? "有图片" : "无图片"));
                    nftList.add(new NFTViewActivity.NFTItem(name, description, imageUrl));
                } catch (JSONException e) {
                    Log.e("GlobalStats", "解析更多NFT数据失败: " + e.getMessage());
                }
            }
            
            Log.d("GlobalStats", "更多NFT数据解析完成，总数量: " + nftList.size());
            nftAdapter.notifyDataSetChanged();
            
            // 检查是否还有更多数据
            if (nfts.length() < nftPageSize) {
                nftHasMore = false;
                nftAdapter.setHasMore(false);
                Log.d("GlobalStats", "已加载完所有NFT");
            }
            
        } catch (JSONException e) {
            Log.e("GlobalStats", "解析更多NFT数据失败", e);
        }
    }
    
    /**
     * 解析NFT数据
     */
    private void parseNftData(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            Log.d("GlobalStats", "解析NFT数据: " + response);
            
            // 检查是否有nfts字段
            JSONArray nfts = jsonResponse.optJSONArray("nfts");
            if (nfts == null) {
                Log.w("GlobalStats", "响应中没有nfts字段");
                showNftEmptyState();
                return;
            }
            
            Log.d("GlobalStats", "找到NFT数组，长度: " + nfts.length());
            
            if (nfts.length() > 0) {
                nftList.clear();
                for (int i = 0; i < nfts.length(); i++) {
                    try {
                        JSONObject nft = nfts.getJSONObject(i);
                        String name = nft.optString("name", "NFT #" + nft.optString("tokenId", ""));
                        String description = nft.optString("description", "暂无描述");
                        String imageUrl = nft.optString("imageUrl", "");
                        
                        // 检查图片URL是否为base64数据，如果是则进行优化处理
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            if (imageUrl.startsWith("data:image/")) {
                                Log.d("GlobalStats", "检测到base64图片数据，进行优化处理");
                                String optimizedUrl = optimizeBase64Image(imageUrl);
                                if (optimizedUrl != null) {
                                    imageUrl = optimizedUrl;
                                    Log.d("GlobalStats", "Base64图片优化成功，使用优化后的URL");
                                } else {
                                    Log.d("GlobalStats", "Base64图片优化失败，使用占位符");
                                    imageUrl = null;
                                }
                            } else {
                                Log.d("GlobalStats", "使用原始图片URL");
                            }
                        } else {
                            Log.d("GlobalStats", "图片URL为空，使用占位符");
                            imageUrl = null;
                        }
                        
                        Log.d("GlobalStats", "添加NFT: " + name + ", 图片URL: " + (imageUrl != null ? "有图片" : "无图片"));
                        nftList.add(new NFTViewActivity.NFTItem(name, description, imageUrl));
                    } catch (JSONException e) {
                        Log.e("GlobalStats", "解析NFT数据失败: " + e.getMessage());
                    }
                }
                
                Log.d("GlobalStats", "NFT数据解析完成，总数量: " + nftList.size());
                nftAdapter.notifyDataSetChanged();
                nftRecyclerView.setVisibility(View.VISIBLE);
                nftEmptyStateLayout.setVisibility(View.GONE);
            } else {
                showNftEmptyState();
            }
        } catch (JSONException e) {
            Log.e("GlobalStats", "解析NFT数据失败", e);
            showNftError();
        }
    }
    
    /**
     * 优化Base64图片数据
     */
    private String optimizeBase64Image(String base64Data) {
        try {
            // 检查是否是有效的base64图片数据
            if (base64Data == null || !base64Data.startsWith("data:image/")) {
                Log.d("GlobalStats", "不是有效的base64图片数据");
                return null;
            }
            
            // 提取base64数据部分
            String base64String = base64Data.substring(base64Data.indexOf(",") + 1);
            
            // 解码base64数据
            byte[] imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
            
            // 检查图片大小，如果太大则进行压缩
            if (imageBytes.length > 1024 * 1024) { // 大于1MB
                Log.d("GlobalStats", "图片过大，进行压缩处理");
                // 这里可以添加图片压缩逻辑
                // 暂时直接返回原数据
                return base64Data;
            }
            
            Log.d("GlobalStats", "Base64图片优化完成，大小: " + imageBytes.length + " bytes");
            return base64Data;
        } catch (Exception e) {
            Log.e("GlobalStats", "Base64图片优化失败: " + e.getMessage());
            return null; // 优化失败时使用占位符
        }
    }
    
    /**
     * 显示NFT详情对话框
     */
    private void showNftDetailDialog(NFTViewActivity.NFTItem nftItem) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nft_detail, null);
        builder.setView(dialogView);

        ImageView closeButton = dialogView.findViewById(R.id.closeButton);
        ImageView detailImageView = dialogView.findViewById(R.id.detailImageView);
        TextView detailNameText = dialogView.findViewById(R.id.detailNameText);
        TextView detailDescriptionText = dialogView.findViewById(R.id.detailDescriptionText);
        TextView expandDescriptionButton = dialogView.findViewById(R.id.expandDescriptionButton);

        detailNameText.setText(nftItem.getName());
        detailDescriptionText.setText(nftItem.getDescription());

        String description = nftItem.getDescription();
        if (description != null && description.length() > 100) {
            expandDescriptionButton.setVisibility(View.VISIBLE);
            expandDescriptionButton.setOnClickListener(v -> {
                if (detailDescriptionText.getMaxLines() == 4) {
                    detailDescriptionText.setMaxLines(Integer.MAX_VALUE);
                    expandDescriptionButton.setText("收起");
                } else {
                    detailDescriptionText.setMaxLines(4);
                    expandDescriptionButton.setText("展开");
                }
            });
        } else {
            expandDescriptionButton.setVisibility(View.GONE);
        }

        if (nftItem.getImageUrl() != null && !nftItem.getImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(nftItem.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .fitCenter()
                    .into(detailImageView);
        } else {
            detailImageView.setImageResource(R.drawable.placeholder_image);
        }

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        closeButton.setOnClickListener(v -> {
            Log.d("GlobalStats", "用户点击了关闭按钮");
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.dialogBackground).setOnClickListener(v -> {
            Log.d("GlobalStats", "用户点击了背景");
            dialog.dismiss();
        });

        dialog.show();
        Log.d("GlobalStats", "显示NFT详情对话框: " + nftItem.getName());
    }
    
    // 勋章统计相关UI方法
    private void showMedalLoading() {
        medalLoadingText.setVisibility(View.VISIBLE);
        medalErrorText.setVisibility(View.GONE);
        globalMedalStatsLayout.setVisibility(View.GONE);
    }
    
    private void hideMedalLoading() {
        medalLoadingText.setVisibility(View.GONE);
    }
    
    private void showMedalError() {
        medalErrorText.setVisibility(View.VISIBLE);
        globalMedalStatsLayout.setVisibility(View.GONE);
    }
    
    // NFT相关UI方法
    private void showNftLoading() {
        nftLoadingText.setVisibility(View.VISIBLE);
        nftErrorText.setVisibility(View.GONE);
        nftEmptyStateLayout.setVisibility(View.GONE);
        nftRecyclerView.setVisibility(View.GONE);
    }
    
    private void hideNftLoading() {
        nftLoadingText.setVisibility(View.GONE);
    }
    
    private void showNftError() {
        nftErrorText.setVisibility(View.VISIBLE);
        nftEmptyStateLayout.setVisibility(View.GONE);
        nftRecyclerView.setVisibility(View.GONE);
    }
    
    private void showNftEmptyState() {
        nftErrorText.setVisibility(View.GONE);
        nftEmptyStateLayout.setVisibility(View.VISIBLE);
        nftRecyclerView.setVisibility(View.GONE);
    }
    
    /**
     * 设置NFT下拉刷新功能
     */
    private void setupNftPullRefresh() {
        nftRecyclerView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    startY = event.getY();
                    isDragging = true;
                    
                    // 检查footer是否可见
                    checkFooterVisibility();
                    Log.d("GlobalStats", "触摸开始: " + startY + ", footer可见: " + isFooterVisible);
                    break;
                    
                case android.view.MotionEvent.ACTION_MOVE:
                    if (isDragging && !isRefreshing && isFooterVisible) {
                        currentY = event.getY();
                        float deltaY = currentY - startY;
                        Log.d("GlobalStats", "触摸移动: deltaY=" + deltaY + ", footer可见: " + isFooterVisible);
                        
                        // 只有在footer可见且用户主动向上拉动时才处理
                        if (deltaY < 0) { // 手指向上移动
                            handlePullGesture(Math.abs(deltaY)); // 使用绝对值
                        } else {
                            resetPullState();
                        }
                    }
                    break;
                    
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    if (isDragging && !isRefreshing && isFooterVisible) {
                        currentY = event.getY();
                        float deltaY = currentY - startY;
                        Log.d("GlobalStats", "触摸结束: deltaY=" + deltaY + ", footer可见: " + isFooterVisible);
                        
                        if (deltaY < -15) { // 向上拉动超过15像素，触发刷新
                            triggerPullRefresh();
                        } else {
                            resetPullState();
                        }
                    }
                    isDragging = false;
                    isFooterVisible = false; // 重置footer可见状态
                    break;
            }
            return false; // 不消费事件，让RecyclerView正常滚动
        });
    }
    
    /**
     * 处理下拉手势（手指向上滑动）
     */
    private void handlePullGesture(float deltaY) {
        Log.d("GlobalStats", "处理下拉手势（向上滑动）: deltaY=" + deltaY);
        
        if (!nftHasMore) {
            // 没有更多数据，显示到底提示
            nftAdapter.setHasMore(false);
            nftAdapter.setLoading(false);
            return;
        }
        
        if (deltaY > 50) { // 向上拉动超过50像素
            nftAdapter.setLoading(true);
            Log.d("GlobalStats", "显示松手刷新提示");
        } else {
            nftAdapter.setLoading(false);
            Log.d("GlobalStats", "显示下拉刷新提示");
        }
    }
    
    /**
     * 触发下拉刷新
     */
    private void triggerPullRefresh() {
        if (isRefreshing || !nftHasMore) {
            return;
        }
        
        Log.d("GlobalStats", "触发下拉刷新");
        isRefreshing = true;
        nftAdapter.setLoading(true);
        
        // 加载下一页NFT数据
        loadMoreNfts();
    }
    
    /**
     * 重置下拉状态
     */
    private void resetPullState() {
        nftAdapter.setLoading(false);
        Log.d("GlobalStats", "重置下拉状态");
    }
    
    /**
     * 完成下拉刷新
     */
    private void completePullRefresh() {
        isRefreshing = false;
        nftAdapter.setLoading(false);
        Log.d("GlobalStats", "完成下拉刷新");
    }
    
    /**
     * 检查footer是否可见
     */
    private void checkFooterVisibility() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) nftRecyclerView.getLayoutManager();
        if (layoutManager != null) {
            int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
            int totalItemCount = nftAdapter.getItemCount();
            
            // 如果最后一个可见项是footer（即最后一个item），则认为footer可见
            isFooterVisible = (lastVisiblePosition == totalItemCount - 1);
            Log.d("GlobalStats", "Footer可见性检查: 最后可见位置=" + lastVisiblePosition + 
                  ", 总项数=" + totalItemCount + ", footer可见=" + isFooterVisible);
        }
    }
    
}
