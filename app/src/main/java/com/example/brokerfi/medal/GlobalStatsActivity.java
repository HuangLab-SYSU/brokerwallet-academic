package com.example.brokerfi.medal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.brokerfi.R;
import com.example.brokerfi.core.config.ApiConfig;
import com.example.brokerfi.nft.adapter.NFTViewAdapter;
import com.example.brokerfi.main.menu.NavigationHelper;
import com.example.brokerfi.core.network.ABIUtils;
import com.example.brokerfi.core.storage.StorageUtil;
import com.example.brokerfi.core.security.SecurityUtil;
import org.json.JSONException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import com.example.brokerfi.main.MainActivity;
import com.example.brokerfi.nft.adapter.NFTAdapter;
import com.example.brokerfi.nft.model.NFT;
import com.example.brokerfi.nft.NFTViewActivity;


/**
 * Global statistics interface / 全局统计界面
 * Show global medal statistics and all NFTs / 显示全局勋章统计和所有NFT
 */
public class GlobalStatsActivity extends AppCompatActivity {

    private static final String TAG = "GlobalStats";

    // UI components
    private TextView titleText;
    private LinearLayout globalMedalStatsLayout;
    private TextView totalUsersText;
    private TextView totalGoldMedalsText;
    private TextView totalSilverMedalsText;
    private TextView totalBronzeMedalsText;
    private TextView highestScoreText;
    private TextView topUserText;
    private TextView medalLoadingText;
    private TextView medalErrorText;

    private RecyclerView nftRecyclerView;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout nftSwipeRefreshLayout;
    private NFTViewAdapter nftAdapter;
    private List<NFTViewActivity.NFTItem> nftList;
    private TextView nftTotalCountText;
    private TextView nftLoadingText;
    private TextView nftErrorText;
    private LinearLayout nftEmptyStateLayout;

    // Static cache, used to restore data when the Activity is rebuilt.
    private static List<NFTViewActivity.NFTItem> cachedNftList = null;
    private static boolean cachedNftHasMore = true;  // Cache paging status
    private static int cachedTotalNftCount = 0;  // Total number of cached NFTs

    private int totalNftCount = 0;  // Total number of global NFTs

    // NFT paging related
    private int nftCurrentPage = 0;
    private int nftPageSize = 5;
    private boolean nftHasMore = true;
    private boolean nftLoadingMore = false;

    // Pull down to refresh related variables
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
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(GlobalStatsActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        // Restore NFT cache
        restoreNftCache();

        loadGlobalMedalStats();

        // If there is cache, do not reload; if there is no cache, load.
        if (nftList.isEmpty()) {
            loadAllNfts();
        } else {
            Log.d("GlobalStats", "Using cached NFT data, total: " + nftList.size() + ", count: " + totalNftCount);
            nftRecyclerView.setVisibility(View.VISIBLE);
            nftAdapter.notifyDataSetChanged();
            nftTotalCountText.setText(nftTotalCountText.getContext().getString(R.string.dialog_confirm_transaction_dialog_total) + " " + totalNftCount);  // ✅ Use cached totalNftCount
            nftTotalCountText.setVisibility(View.VISIBLE);
            // Update the paging status of the Adapter
            nftAdapter.setHasMore(nftHasMore);
            nftAdapter.setLoading(false);
        }
    }

    private void initView() {
        titleText = findViewById(R.id.titleText);
        globalMedalStatsLayout = findViewById(R.id.globalMedalStatsLayout);
        totalUsersText = findViewById(R.id.totalUsersText);
        totalGoldMedalsText = findViewById(R.id.totalGoldMedalsText);
        totalSilverMedalsText = findViewById(R.id.totalSilverMedalsText);
        totalBronzeMedalsText = findViewById(R.id.totalBronzeMedalsText);
        highestScoreText = findViewById(R.id.highestScoreText);
        topUserText = findViewById(R.id.topUserText);
        medalLoadingText = findViewById(R.id.medalLoadingText);
        medalErrorText = findViewById(R.id.medalErrorText);

        nftRecyclerView = findViewById(R.id.nftRecyclerView);
        nftSwipeRefreshLayout = findViewById(R.id.nftSwipeRefreshLayout);
        nftTotalCountText = findViewById(R.id.nftTotalCountText);
        nftLoadingText = findViewById(R.id.nftLoadingText);
        nftErrorText = findViewById(R.id.nftErrorText);
        nftEmptyStateLayout = findViewById(R.id.nftEmptyStateLayout);

        nftList = new ArrayList<>();
        nftAdapter = new NFTViewAdapter(nftList);
        nftRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        nftRecyclerView.setAdapter(nftAdapter);

        // Set up NFT click monitoring
        nftAdapter.setOnItemClickListener((item, position) -> {
            Log.d("GlobalStats", "NFT被点击: " + item.getName());
            showNftDetailDialog(item);
        });

        // Set up more listeners
        nftAdapter.setLoadMoreListener(() -> {
            Log.d("GlobalStats", "触发加载更多NFT");
            loadMoreNfts();
        });

        // Set up RecyclerView scrolling listener to trigger loading more when scrolling to the bottom.
        nftRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && dy > 0) { // Scroll upward
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    // When scrolling to the second to last item, trigger loading more.
                    if (!nftLoadingMore && nftHasMore &&
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        Log.d("GlobalStats", "滚动到底部，触发加载更多");
                        loadMoreNfts();
                    }
                }
            }
        });

        // Set pull-down refresh function
        setupNftPullRefresh();
    }

    private void initEvent() {
        // Set up navigation
        ImageView menu = findViewById(R.id.menu);
        ImageView notificationBtn = findViewById(R.id.notificationBtn);
        RelativeLayout action_bar = findViewById(R.id.action_bar);
        NavigationHelper navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);
    }

    /**
     * Load global medal statistics / 加载全局勋章统计
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
     * Parse global statistics / 解析全局统计数据
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

            // Update UI (keep labels)
            totalUsersText.setText(totalUsersText.getContext().getString(R.string.global_stats_total_users) + " " + data.optInt("totalUsers", 0));
            totalGoldMedalsText.setText(String.valueOf(data.optInt("totalGoldMedals", 0)));
            totalSilverMedalsText.setText(String.valueOf(data.optInt("totalSilverMedals", 0)));
            totalBronzeMedalsText.setText(String.valueOf(data.optInt("totalBronzeMedals", 0)));
            highestScoreText.setText(highestScoreText.getContext().getString(R.string.global_stats_highest_score) + " " + data.optInt("highestScore", 0));

            String topUser = data.optString("topUserDisplayName", "None");
            if (topUser.equals("null") || topUser.isEmpty()) {
                topUser = "None";
            }
            topUserText.setText(topUserText.getContext().getString(R.string.global_stats_top_user) + " " + topUser);

            globalMedalStatsLayout.setVisibility(View.VISIBLE);
            Log.d("GlobalStats", "全局勋章统计加载完成");

        } catch (JSONException e) {
            Log.e("GlobalStats", "解析全局统计数据失败", e);
            showMedalError();
        }
    }

    /**
     * Load more NFTs / 加载更多NFT
     */
    private void loadMoreNfts() {
        if (nftLoadingMore || !nftHasMore) {
            Log.d("GlobalStats", "无法加载更多NFT - 正在加载: " + nftLoadingMore + ", 还有更多: " + nftHasMore);
            return;
        }

        nftLoadingMore = true;
        nftCurrentPage++;

        // Update the footer status to "Loading..."
        runOnUiThread(() -> {
            nftAdapter.setLoading(true);
        });

        Log.d("GlobalStats", "开始加载更多NFT，页码: " + nftCurrentPage);

        new Thread(() -> {
            try {
                String response = MedalApiUtil.getAllNfts(nftCurrentPage, nftPageSize);
                Log.d("GlobalStats", "加载更多NFT响应: " + (response != null ? response : "null"));

                // If MedalApiUtil returns null, try direct HTTP call.
                if (response == null || response.trim().isEmpty()) {
                    Log.d("GlobalStats", "MedalApiUtil返回空，尝试直接HTTP调用");
                    try {
                        String testUrl = ApiConfig.getAllNftsUrl(nftCurrentPage, nftPageSize);
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
                    nftAdapter.setLoading(false);
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
                    nftAdapter.setLoading(false);
                    completePullRefresh();
                });
            }
        }).start();
    }

    /**
     * Load all NFTs / 加载所有NFT
     */
    private void loadAllNfts() {
        showNftLoading();

        new Thread(() -> {
            try {
                Log.d("GlobalStats", "开始加载所有NFT");

                // Test API calls directly
                String testUrl = ApiConfig.getAllNftsUrl(nftCurrentPage, nftPageSize);
                Log.d("GlobalStats", "测试URL: " + testUrl);

                String response = MedalApiUtil.getAllNfts(nftCurrentPage, nftPageSize);
                Log.d("GlobalStats", "MedalApiUtil响应: " + (response != null ? response : "null"));
                Log.d("GlobalStats", "响应长度: " + (response != null ? response.length() : "null"));
                Log.d("GlobalStats", "响应是否为空: " + (response == null || response.trim().isEmpty()));

                // If MedalApiUtil returns null, try direct HTTP call.
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

                // Use final variables
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
                    // Complete pull-down refresh
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
     * Parse more NFT data (paginated loading) / 解析更多NFT数据（分页加载）
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

            // Add new NFT to existing list
            for (int i = 0; i < nfts.length(); i++) {
                try {
                    JSONObject nft = nfts.getJSONObject(i);
                    String name = nft.optString("name", "NFT #" + nft.optString("tokenId", ""));
                    String description = nft.optString("description", getString(R.string.global_stats_no_description));
                    String imageUrl = nft.optString("imageUrl", "");

                    // Check image URL format and process
                    if (imageUrl != null && imageUrl.startsWith("{")) {
                        // ✅ New format: JSON metadata (including backend path)
                        try {
                            JSONObject imageMetadata = new JSONObject(imageUrl);
                            String storageType = imageMetadata.optString("storageType", "");

                            if ("backend-server".equals(storageType)) {
                                String path = imageMetadata.optString("path", "");
                                String serverUrl = imageMetadata.optString("serverUrl", ApiConfig.NFT_DAO_URL);

                                if (!path.isEmpty()) {
                                    imageUrl = ApiConfig.resolveNftAssetUrl(serverUrl + path);
                                    Log.d("GlobalStats", "使用后端服务器图片: " + imageUrl);
                                } else {
                                    Log.w("GlobalStats", "图片路径为空");
                                    imageUrl = null;
                                }
                            } else {
                                Log.w("GlobalStats", "未知存储类型: " + storageType);
                                imageUrl = null;
                            }
                        } catch (JSONException e) {
                            Log.e("GlobalStats", "解析图片元数据失败: " + e.getMessage());
                            imageUrl = null;
                        }
                    } else if (imageUrl != null && !imageUrl.isEmpty()) {
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

                    // Get NFT minting time, holder address and display name.
                    String mintTime = nft.optString("mintTime", "");
                    String ownerAddress = nft.optString("ownerAddress", "");
                    String ownerDisplayName = nft.optString("ownerDisplayName", getString(R.string.global_stats_anonymous_user));

                    // Parse attributes to obtain material upload time
                    String uploadTime = "";
                    if (nft.has("attributes")) {
                        Object attrObj = nft.opt("attributes");
                        String attributesStr = "";
                        if (attrObj instanceof String) {
                            attributesStr = (String) attrObj;
                        } else if (attrObj instanceof JSONObject || attrObj instanceof org.json.JSONArray) {
                            attributesStr = attrObj.toString();
                        }

                        // Trying to extract timestamp from attributes JSON
                        if (!attributesStr.isEmpty()) {
                            try {
                                JSONObject attrJson = new JSONObject(attributesStr);
                                if (attrJson.has("timestamp")) {
                                    uploadTime = formatTimestamp(attrJson.optString("timestamp", ""));
                                }
                            } catch (JSONException e) {
                                Log.w("GlobalStats", "解析attributes中的timestamp失败: " + e.getMessage());
                            }
                        }
                    }

                    Log.d("GlobalStats", "添加更多NFT: " + name + ", 上传时间: " + uploadTime + ", 铸造时间: " + mintTime + ", 持有者: " + ownerAddress + " (" + ownerDisplayName + ")");
                    NFTViewActivity.NFTItem nftItem = new NFTViewActivity.NFTItem(name, description, imageUrl);
                    nftItem.setUploadTime(uploadTime);
                    nftItem.setMintTime(mintTime);
                    nftItem.setOwnerAddress(ownerAddress);
                    nftItem.setOwnerDisplayName(ownerDisplayName);
                    nftList.add(nftItem);
                } catch (JSONException e) {
                    Log.e("GlobalStats", "解析更多NFT数据失败: " + e.getMessage());
                }
            }

            Log.d("GlobalStats", "更多NFT数据解析完成，当前列表总数: " + nftList.size());

            // Get the total NFT quantity from the response.
            int totalFromResponse = jsonResponse.optInt("totalCount", 0);
            if (totalFromResponse > 0) {
                totalNftCount = totalFromResponse;
                Log.d("GlobalStats", "更新NFT总数: " + totalNftCount);
            }

            nftAdapter.notifyDataSetChanged();

            // Check if there is more data
            if (nftList.size() >= totalNftCount) {
                nftHasMore = false;
                nftAdapter.setHasMore(false);
                Log.d("GlobalStats", "已加载完所有NFT: " + nftList.size() + "/" + totalNftCount);
            } else {
                Log.d("GlobalStats", "还有更多NFT: " + nftList.size() + "/" + totalNftCount);
            }

            // Save NFT cache
            saveNftCache();

        } catch (JSONException e) {
            Log.e("GlobalStats", "解析更多NFT数据失败", e);
        }
    }

    /**
     * Parse NFT data / 解析NFT数据
     */
    private void parseNftData(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            Log.d("GlobalStats", "解析NFT数据: " + response);

            // Check if there is nfts field
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
                        String description = nft.optString("description", getString(R.string.global_stats_no_description));
                        String imageUrl = nft.optString("imageUrl", "");

                        // Check image URL format and process
                        if (imageUrl != null && imageUrl.startsWith("{")) {
                            // ✅ New format: JSON metadata (including backend path)
                            try {
                                JSONObject imageMetadata = new JSONObject(imageUrl);
                                String storageType = imageMetadata.optString("storageType", "");

                                if ("backend-server".equals(storageType)) {
                                    String path = imageMetadata.optString("path", "");
                                    String serverUrl = imageMetadata.optString("serverUrl", ApiConfig.NFT_DAO_URL);

                                    if (!path.isEmpty()) {
                                        imageUrl = ApiConfig.resolveNftAssetUrl(serverUrl + path);
                                        Log.d("GlobalStats", "使用后端服务器图片: " + imageUrl);
                                    } else {
                                        Log.w("GlobalStats", "图片路径为空");
                                        imageUrl = null;
                                    }
                                } else {
                                    Log.w("GlobalStats", "未知存储类型: " + storageType);
                                    imageUrl = null;
                                }
                            } catch (JSONException e) {
                                Log.e("GlobalStats", "解析图片元数据失败: " + e.getMessage());
                                imageUrl = null;
                            }
                        } else if (imageUrl != null && !imageUrl.isEmpty()) {
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

                        // Get NFT minting time, holder address and display name.
                        String mintTime = nft.optString("mintTime", "");
                        String ownerAddress = nft.optString("ownerAddress", "");
                        String ownerDisplayName = nft.optString("ownerDisplayName", getString(R.string.global_stats_anonymous_user));

                        // Parse attributes to obtain material upload time
                        String uploadTime = "";
                        if (nft.has("attributes")) {
                            Object attrObj = nft.opt("attributes");
                            String attributesStr = "";
                            if (attrObj instanceof String) {
                                attributesStr = (String) attrObj;
                            } else if (attrObj instanceof JSONObject || attrObj instanceof org.json.JSONArray) {
                                attributesStr = attrObj.toString();
                            }

                            // Trying to extract timestamp from attributes JSON
                            if (!attributesStr.isEmpty()) {
                                try {
                                    JSONObject attrJson = new JSONObject(attributesStr);
                                    if (attrJson.has("timestamp")) {
                                        uploadTime = formatTimestamp(attrJson.optString("timestamp", ""));
                                    }
                                } catch (JSONException e) {
                                    Log.w("GlobalStats", "解析attributes中的timestamp失败: " + e.getMessage());
                                }
                            }
                        }

                        Log.d("GlobalStats", "添加NFT: " + name + ", 上传时间: " + uploadTime + ", 铸造时间: " + mintTime + ", 持有者: " + ownerAddress + " (" + ownerDisplayName + ")");
                        NFTViewActivity.NFTItem nftItem = new NFTViewActivity.NFTItem(name, description, imageUrl);
                        nftItem.setUploadTime(uploadTime);
                        nftItem.setMintTime(mintTime);
                        nftItem.setOwnerAddress(ownerAddress);
                        nftItem.setOwnerDisplayName(ownerDisplayName);
                        nftList.add(nftItem);
                    } catch (JSONException e) {
                        Log.e("GlobalStats", "解析NFT数据失败: " + e.getMessage());
                    }
                }

                Log.d("GlobalStats", "NFT数据解析完成，当前列表数量: " + nftList.size());

                // Get the total NFT quantity from the response.
                int totalFromResponse = jsonResponse.optInt("totalCount", nftList.size());
                totalNftCount = totalFromResponse;
                Log.d("GlobalStats", "NFT总数: " + totalNftCount + ", 当前已加载: " + nftList.size());

                // Update NFT total count display
                nftTotalCountText.setText(nftTotalCountText.getContext().getString(R.string.dialog_confirm_transaction_dialog_total) + " " + totalNftCount);
                nftTotalCountText.setVisibility(View.VISIBLE);

                // Check if there is more data (first load)
                if (nftList.size() >= totalNftCount) {
                    // All data loaded
                    nftHasMore = false;
                    nftAdapter.setHasMore(false);
                    Log.d("GlobalStats", "首次加载完成，已加载所有NFT");
                } else {
                    // There is more data
                    nftHasMore = true;
                    nftAdapter.setHasMore(true);
                    Log.d("GlobalStats", "首次加载完成，还有更多NFT可加载");
                }

                nftAdapter.notifyDataSetChanged();
                nftRecyclerView.setVisibility(View.VISIBLE);
                nftEmptyStateLayout.setVisibility(View.GONE);

                // Save NFT cache
                saveNftCache();

                // Stop pull-down refresh animation
                stopNftRefreshing();
            } else {
                showNftEmptyState();
                stopNftRefreshing();
            }
        } catch (JSONException e) {
            Log.e("GlobalStats", "解析NFT数据失败", e);
            showNftError();
            stopNftRefreshing();
        }
    }

    /**
     * Optimize Base64 image data / 优化Base64图片数据
     */
    private String optimizeBase64Image(String base64Data) {
        try {
            // Check if it is valid base64 image data.
            if (base64Data == null || !base64Data.startsWith("data:image/")) {
                Log.d("GlobalStats", "不是有效的base64图片数据");
                return null;
            }

            // Extract base64 data part
            String base64String = base64Data.substring(base64Data.indexOf(",") + 1);

            // Decode base64 data
            byte[] imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);

            // Check image size and compress if too big.
            if (imageBytes.length > 1024 * 1024) { // Greater than 1MB
                Log.d("GlobalStats", "图片过大，进行压缩处理");
                // Here you can add image compression logic
                // Temporarily return directly to the original data
                return base64Data;
            }

            Log.d("GlobalStats", "Base64图片优化完成，大小: " + imageBytes.length + " bytes");
            return base64Data;
        } catch (Exception e) {
            Log.e("GlobalStats", "Base64图片优化失败: " + e.getMessage());
            return null; // Use placeholders when optimization fails
        }
    }

    /**
     * Show the NFT detail dialog (image plus two time attributes) / 显示NFT详情对话框（图片 + 2个时间属性）
     */
    private void showNftDetailDialog(NFTViewActivity.NFTItem nftItem) {
        // Create dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nft_detail, null);
        builder.setView(dialogView);

        // Bind views
        ImageView nftImageView = dialogView.findViewById(R.id.nftImageView);
        LinearLayout attributesContainer = dialogView.findViewById(R.id.attributesContainer);

        // Load NFT image
        // Use the original resolution in the details dialog box to ensure image quality.
        if (nftImageView != null && nftItem.getImageUrl() != null && !nftItem.getImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(nftItem.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL) // Use original image size
                    .fitCenter() // Display the image completely without cropping
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // Cache original image
                    .into(nftImageView);
            Log.d("GlobalStats", "详情对话框加载高清原图: " + nftItem.getImageUrl());
        }

        // Show time attributes
        if (attributesContainer != null) {
            attributesContainer.removeAllViews();

            // Material upload time
            if (nftItem.getUploadTime() != null && !nftItem.getUploadTime().isEmpty()) {
                addAttributeItem(attributesContainer, "Material Upload", nftItem.getUploadTime());
            }

            // NFT minting time
            if (nftItem.getMintTime() != null && !nftItem.getMintTime().isEmpty()) {
                addAttributeItem(attributesContainer, "NFT Minted", nftItem.getMintTime());
            }

            // Owner address
            if (nftItem.getOwnerAddress() != null && !nftItem.getOwnerAddress().isEmpty()) {
                addAttributeItem(attributesContainer, "Owner", nftItem.getOwnerAddress());
            }
        }

        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        dialog.show();
        Log.d("GlobalStats", "显示NFT详情对话框");
    }

    // UI methods related to medal statistics
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

    // NFT related UI methods
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
     * Set the NFT pull-down refresh function (pull-down refresh at the top + load more at the bottom) / 设置NFT下拉刷新功能（顶部下拉刷新 + 底部加载更多）
     */
    private void setupNftPullRefresh() {
        // 1. Set top pull-to-refresh (using SwipeRefreshLayout)
        if (nftSwipeRefreshLayout != null) {
            nftSwipeRefreshLayout.setOnRefreshListener(() -> {
                Log.d("GlobalStats", "用户顶部下拉刷新NFT");
                refreshNfts();
            });

            // Set refresh animation color
            nftSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            );
        }

        // 2. Keep the bottom loading more function (using Adapter's LoadMoreListener)
        nftAdapter.setLoadMoreListener(() -> {
            if (!nftLoadingMore && nftHasMore) {
                Log.d("GlobalStats", "用户触发底部加载更多");
                loadMoreNfts();
            }
        });
    }

    /**
     * Restore NFT cache / 恢复NFT缓存
     */
    private void restoreNftCache() {
        if (cachedNftList != null && !cachedNftList.isEmpty()) {
            nftList.clear();
            nftList.addAll(cachedNftList);
            nftHasMore = cachedNftHasMore;  // Restore paging state
            totalNftCount = cachedTotalNftCount;  // Restore total number of NFTs
            Log.d("GlobalStats", "从静态缓存恢复NFT数据，共" + nftList.size() + "个，总数=" + totalNftCount + "，hasMore=" + nftHasMore);
        } else {
            Log.d("GlobalStats", "没有NFT缓存数据");
        }
    }

    /**
     * Save NFT cache / 保存NFT缓存
     */
    private void saveNftCache() {
        if (nftList != null && !nftList.isEmpty()) {
            cachedNftList = new ArrayList<>(nftList);
            cachedNftHasMore = nftHasMore;  // Save pagination state
            cachedTotalNftCount = totalNftCount;  // Save the total number of NFTs
            Log.d("GlobalStats", "保存NFT缓存，共" + cachedNftList.size() + "个，总数=" + cachedTotalNftCount + "，hasMore=" + cachedNftHasMore);
        }
    }

    /**
     * Reset NFT pagination state / 重置NFT分页状态
     */
    private void resetNftPagination() {
        nftCurrentPage = 0;
        nftLoadingMore = false;
        nftHasMore = true;
        Log.d("GlobalStats", "重置NFT分页状态");
    }

    /**
     * Refresh NFT (reset pagination, reload) / 刷新NFT（重置分页，重新加载）
     */
    private void refreshNfts() {
        Log.d("GlobalStats", "刷新NFT列表");
        resetNftPagination();
        nftList.clear();
        nftAdapter.notifyDataSetChanged();
        loadAllNfts();
    }

    /**
     * Stop pull-down refresh animation / 停止下拉刷新动画
     */
    private void stopNftRefreshing() {
        if (nftSwipeRefreshLayout != null && nftSwipeRefreshLayout.isRefreshing()) {
            nftSwipeRefreshLayout.setRefreshing(false);
        }
    }

    /**
     * Handling pull-down gestures (finger sliding up) / 处理下拉手势（手指向上滑动）
     */
    private void handlePullGesture(float deltaY) {
        Log.d("GlobalStats", "处理下拉手势（向上滑动）: deltaY=" + deltaY);

        if (!nftHasMore) {
            // No more data, show the end prompt
            nftAdapter.setHasMore(false);
            nftAdapter.setLoading(false);
            return;
        }

        if (deltaY > 50) { // Pull up more than 50 pixels
            nftAdapter.setLoading(true);
            Log.d("GlobalStats", "显示松手刷新提示");
        } else {
            nftAdapter.setLoading(false);
            Log.d("GlobalStats", "显示下拉刷新提示");
        }
    }

    /**
     * Trigger pull-to-refresh / 触发下拉刷新
     */
    private void triggerPullRefresh() {
        if (isRefreshing || !nftHasMore) {
            return;
        }

        Log.d("GlobalStats", "触发下拉刷新");
        isRefreshing = true;
        nftAdapter.setLoading(true);

        // Load the next page of NFT data
        loadMoreNfts();
    }

    /**
     * Reset pull-to-refresh state / 重置下拉状态
     */
    private void resetPullState() {
        nftAdapter.setLoading(false);
        Log.d("GlobalStats", "重置下拉状态");
    }

    /**
     * Complete pull-down refresh / 完成下拉刷新
     */
    private void completePullRefresh() {
        isRefreshing = false;
        nftAdapter.setLoading(false);
        Log.d("GlobalStats", "完成下拉刷新");
    }

    /**
     * Check if footer is visible / 检查footer是否可见
     */
    private void checkFooterVisibility() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) nftRecyclerView.getLayoutManager();
        if (layoutManager != null) {
            int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
            int totalItemCount = nftAdapter.getItemCount();

            // If the last visible item is footer (that is, the last item), the footer is considered visible.
            isFooterVisible = (lastVisiblePosition == totalItemCount - 1);
            Log.d("GlobalStats", "Footer可见性检查: 最后可见位置=" + lastVisiblePosition +
                  ", 总项数=" + totalItemCount + ", footer可见=" + isFooterVisible);
        }
    }


    /**
     * Add a single attribute display item / 添加单个属性显示项
     */
    private void addAttributeItem(android.widget.LinearLayout container, String label, String value) {
        if (value == null || value.isEmpty() || "null".equals(value)) {
            return; // Skip null values
        }

        // Create attribute row
        android.widget.LinearLayout attributeRow = new android.widget.LinearLayout(this);
        attributeRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        android.widget.LinearLayout.LayoutParams rowParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, 8);
        attributeRow.setLayoutParams(rowParams);

        // Label
        android.widget.TextView labelView = new android.widget.TextView(this);
        labelView.setText(label + ": ");
        labelView.setTextSize(14);
        labelView.setTextColor(getResources().getColor(R.color.grey_60));
        android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelView.setLayoutParams(labelParams);

        // Value
        android.widget.TextView valueView = new android.widget.TextView(this);
        valueView.setText(value);
        valueView.setTextSize(14);
        valueView.setTextColor(getResources().getColor(R.color.black));
        valueView.setTypeface(null, android.graphics.Typeface.BOLD);
        android.widget.LinearLayout.LayoutParams valueParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        valueView.setLayoutParams(valueParams);

        attributeRow.addView(labelView);
        attributeRow.addView(valueView);
        container.addView(attributeRow);
    }

    /**
     * Format timestamp (convert ISO 8601 format to readable format) / 格式化时间戳（ISO 8601格式转为可读格式）
     */
    private String formatTimestamp(String timestamp) {
        try {
            // ISO 8601 format: 2025-10-07T18:48:12.345Z
            if (timestamp != null && timestamp.contains("T")) {
                String[] parts = timestamp.split("T");
                if (parts.length >= 2) {
                    String date = parts[0]; // 2025-10-07
                    String time = parts[1].split("\\.")[0]; // 18:48:12
                    return date + " " + time;
                }
            }
            return timestamp;
        } catch (Exception e) {
            Log.w("GlobalStats", "格式化时间戳失败: " + e.getMessage());
            return timestamp;
        }
    }

}
