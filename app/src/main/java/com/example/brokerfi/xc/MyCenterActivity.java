package com.example.brokerfi.xc;

import static com.example.brokerfi.core.config.ApiConfig.API_BLOCKCHAIN_MEDALS;
import static com.example.brokerfi.core.config.ApiConfig.API_BLOCKCHAIN_NFT_USER;

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
import com.example.brokerfi.nft.adapter.NFTViewAdapter;
import com.example.brokerfi.proof.adapter.SubmissionHistoryAdapter;
import com.example.brokerfi.main.menu.NavigationHelper;
import com.example.brokerfi.proof.model.SubmissionRecord;
import com.example.brokerfi.core.network.ABIUtils;
import com.example.brokerfi.core.storage.StorageUtil;
import com.example.brokerfi.core.security.SecurityUtil;
import org.json.JSONException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import com.example.brokerfi.core.config.ApiConfig;
import com.example.brokerfi.main.MainActivity;
import com.example.brokerfi.nft.adapter.NFTAdapter;
import com.example.brokerfi.nft.model.NFT;
import com.example.brokerfi.nft.NFTViewActivity;
import com.example.brokerfi.proof.SubmissionHistoryUtil;


/**
 * My page - personal center / 我的界面 - 个人中心
 * Display user’s medals, submission history, NFT and other information. / 显示用户的勋章、提交历史、NFT等信息
 */
public class MyCenterActivity extends AppCompatActivity {

    private static final String TAG = "MyCenter";

    // UI components
    private TextView titleText;
    private LinearLayout medalOverviewLayout;
    private TextView goldMedalCount;
    private TextView silverMedalCount;
    private TextView bronzeMedalCount;
    private TextView medalLoadingText;
    private TextView medalErrorText;

    private LinearLayout tabLayout;
    private TextView submissionsTab;
    private TextView nftsTab;

    private RecyclerView submissionsRecyclerView;
    private RecyclerView nftRecyclerView;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout nftSwipeRefreshLayout;
    private TextView nftTotalCountText;
    private TextView submissionsLoadingText;
    private TextView submissionsErrorText;
    private LinearLayout submissionsEmptyStateLayout;
    private TextView nftLoadingText;
    private TextView nftErrorText;
    private LinearLayout nftEmptyStateLayout;

    // Data
    private List<SubmissionRecord> submissionsList = new ArrayList<>();
    private List<NFTViewActivity.NFTItem> nftList = new ArrayList<>();
    private SubmissionHistoryAdapter submissionsAdapter;
    private NFTViewAdapter nftAdapter;

    // Static cache, used to restore data when the Activity is rebuilt.
    private static List<NFTViewActivity.NFTItem> cachedNftList = null;
    private static boolean cachedNftHasMore = true;  // Cache paging status
    private static int cachedTotalNftCount = 0;  // Total number of cached NFTs
    private static String cachedWalletAddress = null;  // Cached wallet address

    // NFT pagination loading state
    private int nftCurrentPage = 0;
    private int nftPageSize = 5; // Load 5 NFTs per page
    private boolean nftLoadingMore = false;
    private boolean nftHasMore = true;
    private int totalNftCount = 0; // Total number of NFTs

    // Pull-to-refresh state
    private boolean isDragging = false;
    private boolean isRefreshing = false;
    private boolean isFooterVisible = false; // Is the footer visible?
    private float startY = 0;
    private float currentY = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_center);

        initViews();
        initEvents();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MyCenterActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        // Check if the address has changed, restore or clear the NFT cache.
        checkAndRestoreNftCache();

        loadUserData();
    }

    private void initViews() {
        goldMedalCount = findViewById(R.id.goldMedalCount);
        silverMedalCount = findViewById(R.id.silverMedalCount);
        bronzeMedalCount = findViewById(R.id.bronzeMedalCount);
        medalLoadingText = findViewById(R.id.medalLoadingText);
        medalErrorText = findViewById(R.id.medalErrorText);

        submissionsTab = findViewById(R.id.tabSubmissions);
        nftsTab = findViewById(R.id.tabNfts);

        submissionsRecyclerView = findViewById(R.id.submissionsRecyclerView);
        nftRecyclerView = findViewById(R.id.nftRecyclerView);
        nftSwipeRefreshLayout = findViewById(R.id.nftSwipeRefreshLayout);
        nftTotalCountText = findViewById(R.id.nftTotalCountText);
        submissionsLoadingText = findViewById(R.id.submissionsLoadingText);
        submissionsErrorText = findViewById(R.id.submissionsErrorText);
        submissionsEmptyStateLayout = findViewById(R.id.submissionsEmptyStateLayout);
        nftLoadingText = findViewById(R.id.nftLoadingText);
        nftErrorText = findViewById(R.id.nftErrorText);
        nftEmptyStateLayout = findViewById(R.id.nftEmptyStateLayout);

        // Initialize adapter
        submissionsAdapter = new SubmissionHistoryAdapter(this, submissionsList);
        nftAdapter = new NFTViewAdapter(nftList);
        nftAdapter.setLoadMoreListener(() -> {
            Log.d("MyCenter", "适配器触发加载更多");
            loadMoreNfts();
        });

        nftAdapter.setOnItemClickListener((item, position) -> {
            Log.d("MyCenter", "用户点击了NFT: " + item.getName());
            showNftDetailDialog(item);
        });
        Log.d("MyCenter", "NFT适配器初始化完成，列表大小: " + nftList.size());

        submissionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        submissionsRecyclerView.setAdapter(submissionsAdapter);

        nftRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        nftRecyclerView.setAdapter(nftAdapter);

        // Set up RecyclerView scrolling listener to trigger loading more when scrolling to the bottom.
        nftRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && dy > 0) { // Scroll upward
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    // When scrolling to the second to last item, trigger loading more.
                    if (!nftLoadingMore && nftHasMore &&
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        Log.d("MyCenter", "滚动到底部，触发加载更多");
                        loadMoreNfts();
                    }
                }
            }
        });

        // Set the RecyclerView pull-to-refresh listener
        setupNftPullRefresh();

        // Show submission history by default
        switchToSubmissionsTab();
    }

    private void initEvents() {
        // Tab switching
        submissionsTab.setOnClickListener(v -> switchToSubmissionsTab());
        nftsTab.setOnClickListener(v -> {
            Log.d("MyCenter", "用户点击了NFT Tab");
            switchToNftsTab();
        });

    }

    private void switchToSubmissionsTab() {
        submissionsTab.setSelected(true);
        nftsTab.setSelected(false);

        // Show submission history content area
        findViewById(R.id.submissionsContent).setVisibility(View.VISIBLE);
        findViewById(R.id.nftsContent).setVisibility(View.GONE);

        submissionsRecyclerView.setVisibility(View.VISIBLE);
        nftRecyclerView.setVisibility(View.GONE);
        loadSubmissions();
    }

    private void switchToNftsTab() {
        Log.d("MyCenter", "切换到NFT Tab");
        nftsTab.setSelected(true);
        submissionsTab.setSelected(false);

        // Show NFT content area
        findViewById(R.id.nftsContent).setVisibility(View.VISIBLE);
        findViewById(R.id.submissionsContent).setVisibility(View.GONE);

        nftRecyclerView.setVisibility(View.VISIBLE);
        submissionsRecyclerView.setVisibility(View.GONE);

        // Smart caching: If there is cache, it will be displayed directly; if not, it will be loaded for the first time.
        if (nftList.isEmpty()) {
            // first load
            resetNftPagination();
            Log.d("MyCenter", "首次加载NFT数据");
            loadMyNfts();
        } else {
            // There is cache and can be displayed directly.
            Log.d("MyCenter", "Using cached NFT data, total: " + nftList.size() + ", count: " + totalNftCount + ", pull to refresh");
            nftRecyclerView.setVisibility(View.VISIBLE);
            // Update NFT total count display
            nftTotalCountText.setText(nftTotalCountText.getContext().getString(R.string.dialog_confirm_transaction_dialog_total) + " " + totalNftCount);  // ✅ Use cached totalNftCount
            nftTotalCountText.setVisibility(View.VISIBLE);
            // Update the paging status of the Adapter
            nftAdapter.setHasMore(nftHasMore);
            nftAdapter.setLoading(false);
        }
    }

    /**
     * Check address changes and restore NFT cache / 检查地址变化并恢复NFT缓存
     */
    private void checkAndRestoreNftCache() {
        String currentAddress = getMyAddressForDatabase();

        // Check whether the cached address is consistent with the current address.
        if (cachedWalletAddress != null && cachedWalletAddress.equals(currentAddress)) {
            // The address has not changed, restore the cache.
            if (cachedNftList != null && !cachedNftList.isEmpty()) {
                nftList.clear();
                nftList.addAll(cachedNftList);
                nftHasMore = cachedNftHasMore;
                totalNftCount = cachedTotalNftCount;
                Log.d("MyCenter", "Address unchanged, restored NFT cache: " + nftList.size() + " items, address=" + currentAddress);
            } else {
                Log.d("MyCenter", "Address unchanged but no cache data, address=" + currentAddress);
            }
        } else {
            // The address changes, clear the old cache
            if (cachedWalletAddress != null) {
                Log.d("MyCenter", "Address changed from " + cachedWalletAddress + " to " + currentAddress + ", clearing cache");
            } else {
                Log.d("MyCenter", "First time loading, address=" + currentAddress);
            }
            clearNftCache();
        }
    }

    /**
     * Clear NFT cache / 清空NFT缓存
     */
    private void clearNftCache() {
        cachedNftList = null;
        cachedNftHasMore = true;
        cachedTotalNftCount = 0;
        cachedWalletAddress = null;
        nftList.clear();
        nftHasMore = true;
        totalNftCount = 0;
        Log.d("MyCenter", "NFT cache cleared");
    }

    /**
     * Save NFT cache / 保存NFT缓存
     */
    private void saveNftCache() {
        if (nftList != null && !nftList.isEmpty()) {
            cachedNftList = new ArrayList<>(nftList);
            cachedNftHasMore = nftHasMore;  // Save pagination state
            cachedTotalNftCount = totalNftCount;  // Save NFT total count
            cachedWalletAddress = getMyAddressForDatabase();  // Save current wallet address
            Log.d("MyCenter", "Saved NFT cache, total: " + cachedNftList.size() + ", count=" + cachedTotalNftCount +
                  ", hasMore=" + cachedNftHasMore + ", address=" + cachedWalletAddress);
        }
    }

    /**
     * Reset NFT pagination state / 重置NFT分页状态
     */
    private void resetNftPagination() {
        nftCurrentPage = 0;
        nftLoadingMore = false;
        nftHasMore = true;
        Log.d("MyCenter", "重置NFT分页状态");
    }

    /**
     * Manually load more NFTs / 手动加载更多NFT
     */
    public void loadMoreNfts() {
        if (!nftLoadingMore && nftHasMore) {
            Log.d("MyCenter", "手动加载更多NFT，当前页码: " + nftCurrentPage);
            loadMyNfts();
        } else {
            Log.d("MyCenter", "无法加载更多NFT - 正在加载: " + nftLoadingMore + ", 还有更多: " + nftHasMore);
        }
    }

    private void loadUserData() {
        loadMyMedals();
    }

    private void loadMyMedals() {
        showMedalLoading();

        // Get current user address
        String myAddress = getMyAddress();

        Log.d("MyCenter", "查询勋章数据，地址: " + myAddress);

        // Query medal data through backend API
        new Thread(() -> {
            try {
                // Build API request URL
                String apiUrl = API_BLOCKCHAIN_MEDALS  + myAddress;

                // Send HTTP GET request
                java.net.URL url = new java.net.URL(apiUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // Read response
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse the JSON response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONObject medals = jsonResponse.optJSONObject("medals");

                    if (medals != null) {
                        int goldMedals = medals.optInt("gold", 0);
                        int silverMedals = medals.optInt("silver", 0);
                        int bronzeMedals = medals.optInt("bronze", 0);

                        runOnUiThread(() -> {
                            hideMedalLoading();
                            updateMedalDisplay(goldMedals, silverMedals, bronzeMedals);
                            Log.d("MyCenter", "勋章数据: 金" + goldMedals + " 银" + silverMedals + " 铜" + bronzeMedals);
                        });
                        return;
                    }
                }

                // Query failed
                runOnUiThread(() -> {
                    hideMedalLoading();
                    showMedalError();
                    Log.e("MyCenter", "查询勋章失败: HTTP " + responseCode);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideMedalLoading();
                    showMedalError();
                    Log.e("MyCenter", "查询勋章失败: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadSubmissions() {
        showSubmissionsLoading();

        // Get the current user address (submit history requires an address without 0x prefix)
        String myAddress = getMyAddressForDatabase();
        Log.d("MyCenter", "查询提交历史，地址: " + myAddress);

        // Use SubmissionHistoryUtil to query the real submission history.
        SubmissionHistoryUtil.getUserSubmissions(myAddress, 0, 20, new SubmissionHistoryUtil.SubmissionHistoryCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    hideSubmissionsLoading();
                    try {
                        // Parse the JSON response
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.optBoolean("success", false);

                        if (success) {
                            JSONArray dataArray = jsonResponse.optJSONArray("data");
                            if (dataArray != null && dataArray.length() > 0) {
                                submissionsList.clear();

                                // Parse commit records
                                for (int i = 0; i < dataArray.length(); i++) {
                                    JSONObject submission = dataArray.getJSONObject(i);

                                    SubmissionRecord record = new SubmissionRecord();

                                    // Basic information
                                    record.setId(submission.optLong("id", 0L));
                                    record.setSubmissionId(submission.optString("submissionId", ""));
                                    record.setBatchId(submission.optString("batchId", null));
                                    record.setFileCount(submission.optInt("fileCount", 1));
                                    record.setFileName(submission.optString("fileName", ""));
                                    record.setFileSize(submission.optLong("fileSize", 0L));
                                    record.setFileType(submission.optString("fileType", ""));
                                    record.setUploadTime(submission.optString("uploadTime", ""));

                                    // Review information
                                    record.setAuditStatus(submission.optString("auditStatus", ""));
                                    record.setAuditStatusDesc(submission.optString("auditStatusDesc", getString(R.string.my_center_unknown_status)));
                                    record.setAuditTime(submission.optString("auditTime", ""));

                                    // Medal information
                                    record.setMedalAwarded(submission.optString("medalAwarded", "NONE"));
                                    record.setMedalAwardedDesc(submission.optString("medalAwardedDesc", getString(R.string.my_center_none)));
                                    record.setMedalAwardTime(submission.optString("medalAwardTime", ""));
                                    record.setMedalTransactionHash(submission.optString("medalTransactionHash", ""));

                                    // NFT information
                                    if (submission.has("nftImage")) {
                                        JSONObject nftImageObj = submission.optJSONObject("nftImage");
                                        if (nftImageObj != null) {
                                            record.setHasNftImage(true);

                                            SubmissionRecord.NftImageInfo nftInfo = new SubmissionRecord.NftImageInfo();
                                            nftInfo.setId(nftImageObj.optLong("id", 0L));
                                            nftInfo.setOriginalName(nftImageObj.optString("originalName", ""));
                                            nftInfo.setMintStatus(nftImageObj.optString("mintStatus", ""));
                                            nftInfo.setMintStatusDesc(nftImageObj.optString("mintStatusDesc", ""));
                                            nftInfo.setTokenId(nftImageObj.optString("tokenId", ""));
                                            nftInfo.setTransactionHash(nftImageObj.optString("transactionHash", ""));

                                            record.setNftImage(nftInfo);
                                        } else {
                                            record.setHasNftImage(false);
                                        }
                                    } else {
                                        record.setHasNftImage(false);
                                    }

                                    // Token reward information
                                    if (submission.has("tokenReward") && !submission.isNull("tokenReward")) {
                                        record.setTokenReward(submission.optString("tokenReward", null));
                                        record.setTokenRewardTxHash(submission.optString("tokenRewardTxHash", null));
                                    }

                                    submissionsList.add(record);

                                    Log.d("MyCenter", "解析提交记录: " + record.getFileName() +
                                        ", 大小: " + record.getFormattedFileSize() +
                                        ", 状态: " + record.getAuditStatusDesc() +
                                        ", 勋章: " + record.getMedalAwardedDesc() +
                                        ", NFT: " + record.isHasNftImage() +
                                        ", BKC奖励: " + (record.getTokenReward() != null ? record.getTokenReward() + " BKC" : "无"));
                                }

                                // Update UI
                                submissionsAdapter.notifyDataSetChanged();
                                hideSubmissionsEmptyState();
                                Log.d("MyCenter", "成功加载 " + submissionsList.size() + " 条提交记录");
                            } else {
                                showSubmissionsEmptyState();
                                Log.d("MyCenter", "提交历史为空");
                            }
                        } else {
                            showSubmissionsError();
                            Log.e("MyCenter", "查询提交历史失败");
                        }
                    } catch (Exception e) {
                        showSubmissionsError();
                        Log.e("MyCenter", "解析提交历史失败: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideSubmissionsLoading();
                    showSubmissionsError();
                    Log.e("MyCenter", "查询提交历史失败: " + error);
                });
            }
        });
    }

    private void loadMyNfts() {
        // Prevent repeated loading
        if (nftLoadingMore) {
            Log.d("MyCenter", "NFT正在加载中，跳过重复请求");
            return;
        }

        if (nftCurrentPage == 0) {
            // Loading for the first time, showing loading status.
            showNftLoading();
            nftList.clear();
        } else {
            // Loading in pages, showing loading more status
            nftLoadingMore = true;
            runOnUiThread(() -> {
                nftAdapter.setLoading(true);
            });
            Log.d("MyCenter", "开始分页加载NFT，页码: " + nftCurrentPage);
        }

        // Get current user address
        String myAddress = getMyAddress();

        Log.d("MyCenter", "查询NFT数据，地址: " + myAddress + ", 页码: " + nftCurrentPage);

        // Query NFT data through backend API
        new Thread(() -> {
            try {
                // Build the API request URL and add pagination parameters.
                String apiUrl = API_BLOCKCHAIN_NFT_USER + myAddress + "?page=" + nftCurrentPage + "&size=" + nftPageSize;

                // Send HTTP GET request, increase timeout
                java.net.URL url = new java.net.URL(apiUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000); // Increase connection timeout
                connection.setReadTimeout(30000); // Increase read timeout

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // Read response
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse the JSON response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    Log.d("MyCenter", "NFT API响应: " + response.toString());

                    // Get the total number of NFTs
                    int totalFromResponse = jsonResponse.optInt("totalCount", 0);
                    totalNftCount = totalFromResponse;
                    Log.d("MyCenter", "NFT总数: " + totalNftCount);

                    JSONArray nfts = jsonResponse.optJSONArray("nfts");
                    Log.d("MyCenter", "NFT数组: " + (nfts != null ? nfts.length() + "个NFT" : "null"));

                    // Detailed debugging information
                    if (nfts != null) {
                        Log.d("MyCenter", "NFT数组长度: " + nfts.length());
                        for (int i = 0; i < nfts.length(); i++) {
                            try {
                                JSONObject nft = nfts.getJSONObject(i);
                                Log.d("MyCenter", "NFT " + i + ": " + nft.toString());
                            } catch (Exception e) {
                                Log.e("MyCenter", "解析NFT " + i + " 失败: " + e.getMessage());
                            }
                        }
                    }

                    if (nfts != null) {
                        runOnUiThread(() -> {
                            Log.d("MyCenter", "开始更新UI - NFT数量: " + nfts.length());

                            if (nftCurrentPage == 0) {
                                // First load, hide loading status
                                hideNftLoading();
                            } else {
                                // Load in pages, hide more loading status
                                nftLoadingMore = false;
                            }

                            if (nfts.length() > 0) {
                                // Add the queried NFT data
                                int addedCount = 0;
                                for (int i = 0; i < nfts.length(); i++) {
                                    try {
                                        JSONObject nft = nfts.getJSONObject(i);
                                        String name = nft.optString("name", "NFT #" + nft.optString("tokenId", ""));
                                        String description = nft.optString("description", getString(R.string.my_center_no_description));
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
                                                        // Splice complete URL
                                                        imageUrl = ApiConfig.resolveNftAssetUrl(serverUrl + path);
                                                        Log.d("MyCenter", "使用后端服务器图片: " + imageUrl);
                                                    } else {
                                                        Log.w("MyCenter", "图片路径为空");
                                                        imageUrl = null;
                                                    }
                                                } else {
                                                    Log.w("MyCenter", "未知存储类型: " + storageType);
                                                    imageUrl = null;
                                                }
                                            } catch (JSONException e) {
                                                Log.e("MyCenter", "解析图片元数据失败: " + e.getMessage());
                                                imageUrl = null;
                                            }
                                        } else if (imageUrl != null && imageUrl.startsWith("data:image/")) {
                                            // Old format: Base64 data
                                            Log.d("MyCenter", "检测到base64图片数据，进行优化处理");
                                            Log.d("MyCenter", "原始图片数据长度: " + imageUrl.length());
                                            String optimizedUrl = optimizeBase64Image(imageUrl);
                                            if (optimizedUrl != null) {
                                                imageUrl = optimizedUrl;
                                                Log.d("MyCenter", "Base64图片优化成功，使用优化后的URL");
                                            } else {
                                                Log.w("MyCenter", "Base64图片优化失败，使用占位符");
                                                imageUrl = null;
                                            }
                                        } else if (imageUrl != null && !imageUrl.isEmpty()) {
                                            Log.d("MyCenter", "使用原始图片URL: " + imageUrl.substring(0, Math.min(50, imageUrl.length())) + "...");
                                        } else {
                                            Log.w("MyCenter", "图片URL为空或无效");
                                            imageUrl = null;
                                        }

                                        // Get the NFT minting time; the My page does not need the holder address.
                                        String mintTime = nft.optString("mintTime", "");

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
                                                    Log.w("MyCenter", "解析attributes中的timestamp失败: " + e.getMessage());
                                                }
                                            }
                                        }

                                        Log.d("MyCenter", "添加NFT: " + name + ", 上传时间: " + uploadTime + ", 铸造时间: " + mintTime);
                                        NFTViewActivity.NFTItem nftItem = new NFTViewActivity.NFTItem(name, description, imageUrl);
                                        nftItem.setUploadTime(uploadTime);
                                        nftItem.setMintTime(mintTime);
                                        // The ownerAddress is not set because it is "my" interface and the holder is the current user.
                                        nftList.add(nftItem);
                                        addedCount++;
                                    } catch (JSONException e) {
                                        Log.e("MyCenter", "解析NFT数据失败: " + e.getMessage());
                                    }
                                }

                                // Check if there is more data
                                if (nftList.size() >= totalNftCount) {
                                    nftHasMore = false;
                                    Log.d("MyCenter", "已加载所有NFT: " + nftList.size() + "/" + totalNftCount);
                                } else {
                                    nftHasMore = true;
                                    nftCurrentPage++;
                                    Log.d("MyCenter", "还有更多NFT: " + nftList.size() + "/" + totalNftCount + ", 下一页: " + nftCurrentPage);
                                }

                                Log.d("MyCenter", "添加NFT数据完成，本次添加: " + addedCount + ", 总数量: " + nftList.size());
                                nftAdapter.updateData(nftList);
                                Log.d("MyCenter", "通知适配器更新完成");

                                nftRecyclerView.setVisibility(View.VISIBLE);
                                Log.d("MyCenter", "设置RecyclerView可见");

                                // Show total number of NFTs
                                nftTotalCountText.setText(nftTotalCountText.getContext().getString(R.string.my_center_total_count) + " " + totalNftCount);
                                nftTotalCountText.setVisibility(View.VISIBLE);

                                // Update adapter status
                                nftAdapter.setHasMore(nftHasMore);
                                nftAdapter.setLoading(false);

                                // Save NFT cache
                                saveNftCache();

                                // Stop pull-down refresh animation
                                stopNftRefreshing();
                            } else {
                                if (nftCurrentPage == 0) {
                                    showNftEmptyState();
                                    Log.d("MyCenter", "NFT数量: 0");
                                } else {
                                    nftHasMore = false;
                                    Log.d("MyCenter", "没有更多NFT数据了");
                                }
                            }
                        });
                        return;
                    }
                }

                // Query failed
                runOnUiThread(() -> {
                    stopNftRefreshing();
                    if (nftCurrentPage == 0) {
                        hideNftLoading();
                        showNftError();
                    } else {
                        nftLoadingMore = false;
                    }
                    Log.e("MyCenter", "查询NFT失败: HTTP " + responseCode);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    stopNftRefreshing();
                    if (nftCurrentPage == 0) {
                        hideNftLoading();
                        showNftError();
                    } else {
                        nftLoadingMore = false;
                    }
                    Log.e("MyCenter", "查询NFT失败: " + e.getMessage());
                });
            }
        }).start();
    }

    private void updateMedalDisplay(int gold, int silver, int bronze) {
        goldMedalCount.setText(String.valueOf(gold));
        silverMedalCount.setText(String.valueOf(silver));
        bronzeMedalCount.setText(String.valueOf(bronze));
    }

    /**
     * Optimize Base64 image data and compress it to appropriate size. / 优化Base64图片数据，压缩到合适尺寸
     */
    private String optimizeBase64Image(String base64Data) {
        try {
            // First verify the data format
            if (base64Data == null || base64Data.isEmpty()) {
                Log.w("MyCenter", "Base64数据为空");
                return null;
            }

            // Check if it is a valid data URL format.
            if (!base64Data.startsWith("data:image/")) {
                Log.w("MyCenter", "Base64数据格式不正确，不是有效的图片数据URL");
                return null;
            }

            // Verify that Base64 data is valid
            if (!isValidBase64DataUrl(base64Data)) {
                Log.w("MyCenter", "Base64数据无效，无法解码");
                return null;
            }

            // Check the data size and return directly if it is less than 100KB.
            if (base64Data.length() < 100000) {
                Log.d("MyCenter", "Base64图片数据较小，直接使用");
                return base64Data;
            }

            // Extract Base64 data part
            String[] parts = base64Data.split(",");
            if (parts.length != 2) {
                Log.w("MyCenter", "Base64数据格式错误，使用占位符");
                return null;
            }

            String mimeType = parts[0];
            String base64String = parts[1];

            // Decode Base64 data
            byte[] imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);

            // Compress images using BitmapFactory
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

            // Calculate compression ratio, target size 300x300
            int targetSize = 300;
            int scale = Math.max(options.outWidth / targetSize, options.outHeight / targetSize);
            options.inJustDecodeBounds = false;
            options.inSampleSize = Math.max(1, scale);
            options.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565; // Reduce memory usage

            // Decode compressed images
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

            if (bitmap == null) {
                Log.w("MyCenter", "图片解码失败，使用占位符");
                return null;
            }

            // further compress to target size
            if (bitmap.getWidth() > targetSize || bitmap.getHeight() > targetSize) {
                android.graphics.Bitmap scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true);
                bitmap.recycle();
                bitmap = scaledBitmap;
            }

            // Convert to compressed Base64
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            android.graphics.Bitmap.CompressFormat format = android.graphics.Bitmap.CompressFormat.JPEG;
            if (mimeType.contains("png")) {
                format = android.graphics.Bitmap.CompressFormat.PNG;
            }
            bitmap.compress(format, 80, baos); // 80% quality
            bitmap.recycle();

            byte[] compressedBytes = baos.toByteArray();
            String compressedBase64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.DEFAULT);

            Log.d("MyCenter", "Base64图片优化完成，原始大小: " + base64String.length() +
                  ", 压缩后大小: " + compressedBase64.length());

            return mimeType + "," + compressedBase64;

        } catch (Exception e) {
            Log.e("MyCenter", "Base64图片优化失败: " + e.getMessage());
            return null; // Use placeholders when optimization fails
        }
    }

    /**
     * Verify that the Base64 data URL is valid. / 验证Base64数据URL是否有效
     */
    private boolean isValidBase64DataUrl(String dataUrl) {
        try {
            if (!dataUrl.contains(",")) {
                return false;
            }
            String[] parts = dataUrl.split(",", 2);
            if (parts.length != 2) {
                return false;
            }
            String base64Part = parts[1];
            // Try decoding Base64
            android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT);
            return true;
        } catch (Exception e) {
            Log.w("MyCenter", "Base64数据验证失败: " + e.getMessage());
            return false;
        }
    }

    private void showMedalLoading() {
        medalLoadingText.setVisibility(View.VISIBLE);
        medalErrorText.setVisibility(View.GONE);
    }

    private void hideMedalLoading() {
        medalLoadingText.setVisibility(View.GONE);
    }

    private void showMedalError() {
        medalErrorText.setVisibility(View.VISIBLE);
    }

    private void showSubmissionsLoading() {
        submissionsLoadingText.setVisibility(View.VISIBLE);
        submissionsErrorText.setVisibility(View.GONE);
        submissionsEmptyStateLayout.setVisibility(View.GONE);
        submissionsRecyclerView.setVisibility(View.GONE);
    }

    private void hideSubmissionsLoading() {
        submissionsLoadingText.setVisibility(View.GONE);
    }

    private void showSubmissionsError() {
        submissionsErrorText.setVisibility(View.VISIBLE);
        submissionsEmptyStateLayout.setVisibility(View.GONE);
        submissionsRecyclerView.setVisibility(View.GONE);
    }

    private void showSubmissionsEmptyState() {
        submissionsEmptyStateLayout.setVisibility(View.VISIBLE);
        submissionsRecyclerView.setVisibility(View.GONE);
    }

    private void hideSubmissionsEmptyState() {
        submissionsEmptyStateLayout.setVisibility(View.GONE);
        submissionsRecyclerView.setVisibility(View.VISIBLE);
    }

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
        nftEmptyStateLayout.setVisibility(View.VISIBLE);
        nftRecyclerView.setVisibility(View.GONE);
    }

    /**
     * Get the current user address (for blockchain API, with 0x prefix) / 获取当前用户地址（用于区块链API，带0x前缀）
     */
    private String getMyAddress() {
        try {
            String privateKey = StorageUtil.getCurrentPrivatekey(this);
            Log.d("MyCenter", "从存储获取的私钥: " + (privateKey != null ? "有私钥" : "无私钥"));

            if (privateKey != null) {
                String address = SecurityUtil.GetAddress(privateKey);
                // Make sure the address has a 0x prefix as the backend API expects Ethereum addresses with a 0x prefix.
                if (!address.startsWith("0x")) {
                    address = "0x" + address;
                }
                Log.d("MyCenter", "计算出的地址: " + address);
                return address;
            } else {
                Log.e("MyCenter", "无法获取私钥，使用默认地址");
                return "0000000000000000000000000000000000000000";
            }
        } catch (Exception e) {
            Log.e("MyCenter", "获取地址失败", e);
            return "0000000000000000000000000000000000000000";
        }
    }

    /**
     * Get the current user address (for database query, without 0x prefix) / 获取当前用户地址（用于数据库查询，不带0x前缀）
     */
    private String getMyAddressForDatabase() {
        try {
            String privateKey = StorageUtil.getCurrentPrivatekey(this);
            Log.d("MyCenter", "从存储获取的私钥: " + (privateKey != null ? "有私钥" : "无私钥"));

            if (privateKey != null) {
                String address = SecurityUtil.GetAddress(privateKey);
                // Remove the 0x prefix because the addresses in the database do not have a 0x prefix.
                if (address.startsWith("0x")) {
                    address = address.substring(2);
                }
                Log.d("MyCenter", "计算出的地址（数据库格式）: " + address);
                return address;
            } else {
                Log.e("MyCenter", "无法获取私钥，使用默认地址");
                return "0000000000000000000000000000000000000000";
            }
        } catch (Exception e) {
            Log.e("MyCenter", "获取地址失败", e);
            return "0000000000000000000000000000000000000000";
        }
    }

    /**
     * Get the current user's private key / 获取当前用户的私钥
     */
    private String getMyPrivateKey() {
        try {
            String privateKey = StorageUtil.getCurrentPrivatekey(this);
            Log.d("MyCenter", "从存储获取的私钥: " + (privateKey != null ? "有私钥" : "无私钥"));

            if (privateKey != null) {
                return privateKey;
            } else {
                Log.e("MyCenter", "无法获取私钥，使用默认私钥");
                return "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
            }
        } catch (Exception e) {
            Log.e("MyCenter", "获取私钥失败", e);
            return "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        }
    }

    /**
     * Set the NFT pull-down refresh function (pull-down refresh at the top + load more at the bottom) / 设置NFT下拉刷新功能（顶部下拉刷新 + 底部加载更多）
     */
    private void setupNftPullRefresh() {
        // 1. Set top pull-to-refresh (using SwipeRefreshLayout)
        if (nftSwipeRefreshLayout != null) {
            nftSwipeRefreshLayout.setOnRefreshListener(() -> {
                Log.d("MyCenter", "用户顶部下拉刷新NFT");
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
                Log.d("MyCenter", "用户触发底部加载更多");
                loadMoreNfts();
            }
        });
    }

    /**
     * Refresh NFT (reset pagination, reload) / 刷新NFT（重置分页，重新加载）
     */
    private void refreshNfts() {
        Log.d("MyCenter", "刷新NFT列表");
        resetNftPagination();
        nftList.clear();
        nftAdapter.notifyDataSetChanged();
        loadMyNfts();
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
        Log.d("MyCenter", "处理下拉手势（向上滑动）: deltaY=" + deltaY);

        if (!nftHasMore) {
            // No more data, show the end prompt
            nftAdapter.setHasMore(false);
            nftAdapter.setLoading(false);
            return;
        }

        if (deltaY > 15) {
            // Pull up more than 15 pixels to display a release prompt.
            Log.d("MyCenter", "显示松手刷新~");
            nftAdapter.setHasMore(true);
            nftAdapter.setLoading(false);
        } else if (deltaY > 5) {
            // Pull up 5-15 pixels to display the pull-to-refresh prompt.
            Log.d("MyCenter", "显示下拉刷新更多");
            nftAdapter.setHasMore(true);
            nftAdapter.setLoading(false);
        } else {
            // Pull up less than 5 pixels to display the default prompt.
            nftAdapter.setHasMore(true);
            nftAdapter.setLoading(false);
        }
    }

    /**
     * Reset pull-to-refresh state / 重置下拉状态
     */
    private void resetPullState() {
        Log.d("MyCenter", "重置下拉状态");
        if (!isRefreshing) {
            nftAdapter.setHasMore(nftHasMore);
            nftAdapter.setLoading(false);
        }
    }

    /**
     * Trigger pull-to-refresh / 触发下拉刷新
     */
    private void triggerPullRefresh() {
        if (!isRefreshing && nftHasMore) {
            isRefreshing = true;
            Log.d("MyCenter", "触发下拉刷新");

            // Show the loading state
            nftAdapter.setHasMore(true);
            nftAdapter.setLoading(true);

            // Trigger load-more
            loadMoreNfts();
        }
    }

    /**
     * Check if the View is fully visible / 检查View是否完全可见
     */
    private boolean isViewFullyVisible(View view) {
        if (view == null) return false;

        int[] location = new int[2];
        view.getLocationOnScreen(location);

        int viewTop = location[1];
        int viewBottom = viewTop + view.getHeight();

        // Get the visible area of ​​RecyclerView
        int[] recyclerViewLocation = new int[2];
        nftRecyclerView.getLocationOnScreen(recyclerViewLocation);
        int recyclerViewTop = recyclerViewLocation[1];
        int recyclerViewBottom = recyclerViewTop + nftRecyclerView.getHeight();

        // Check whether the footer item is completely within the visible area of ​​RecyclerView.
        boolean isFullyVisible = viewTop >= recyclerViewTop && viewBottom <= recyclerViewBottom;

        Log.d("MyCenter", "Footer可见性检查: viewTop=" + viewTop + ", viewBottom=" + viewBottom +
              ", recyclerViewTop=" + recyclerViewTop + ", recyclerViewBottom=" + recyclerViewBottom +
              ", isFullyVisible=" + isFullyVisible);

        return isFullyVisible;
    }

    /**
     * Check if footer is visible / 检查footer是否可见
     */
    private void checkFooterVisibility() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) nftRecyclerView.getLayoutManager();
        if (layoutManager != null) {
            int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
            int totalItemCount = nftRecyclerView.getAdapter().getItemCount();

            // Check whether scrolling to the last item (footer)
            if (lastVisibleItemPosition == totalItemCount - 1) {
                View lastView = layoutManager.findViewByPosition(lastVisibleItemPosition);
                isFooterVisible = (lastView != null && isViewFullyVisible(lastView));
                Log.d("MyCenter", "Footer可见性检查: 最后位置=" + lastVisibleItemPosition +
                      ", 总数=" + totalItemCount + ", footer可见=" + isFooterVisible);
            } else {
                isFooterVisible = false;
                Log.d("MyCenter", "Footer不可见: 最后位置=" + lastVisibleItemPosition + ", 总数=" + totalItemCount);
            }
        } else {
            isFooterVisible = false;
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
            Log.d("MyCenter", "详情对话框加载高清原图: " + nftItem.getImageUrl());
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

            // The My page does not show the holder address because the holder is the current user.
        }

        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        dialog.show();
        Log.d("MyCenter", "显示NFT详情对话框");
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
            Log.w("MyCenter", "格式化时间戳失败: " + e.getMessage());
            return timestamp;
        }
    }
}
