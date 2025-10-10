package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.adapter.NFTViewAdapter;
import com.example.brokerfi.xc.adapter.SubmissionHistoryAdapter;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.example.brokerfi.xc.model.SubmissionRecord;
import com.example.brokerfi.xc.net.ABIUtils;
import com.example.brokerfi.xc.StorageUtil;
import com.example.brokerfi.xc.SecurityUtil;
import org.json.JSONException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 我的界面 - 个人中心
 * 显示用户的勋章、提交历史、NFT等信息
 */
public class MyCenterActivity extends AppCompatActivity {
    
    private static final String TAG = "MyCenter";
    
    // UI组件
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
    
    // 数据
    private List<SubmissionRecord> submissionsList = new ArrayList<>();
    private List<NFTViewActivity.NFTItem> nftList = new ArrayList<>();
    private SubmissionHistoryAdapter submissionsAdapter;
    private NFTViewAdapter nftAdapter;
    
    // 静态缓存，用于Activity重建时恢复数据
    private static List<NFTViewActivity.NFTItem> cachedNftList = null;
    private static boolean cachedNftHasMore = true;  // 缓存分页状态
    private static int cachedTotalNftCount = 0;  // 缓存NFT总数
    private static String cachedWalletAddress = null;  // 缓存的钱包地址
    
    // NFT分页加载相关
    private int nftCurrentPage = 0;
    private int nftPageSize = 5; // 每页加载5个NFT
    private boolean nftLoadingMore = false;
    private boolean nftHasMore = true;
    private int totalNftCount = 0; // NFT总数
    
    // 下拉刷新相关状态
    private boolean isDragging = false;
    private boolean isRefreshing = false;
    private boolean isFooterVisible = false; // footer是否可见
    private float startY = 0;
    private float currentY = 0;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_center);
        
        initViews();
        initEvents();
        
        // 检查地址是否变化，恢复或清空NFT缓存
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
        
        // 初始化适配器
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
        
        // 设置RecyclerView滚动监听，滚动到底部时触发加载更多
        nftRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && dy > 0) { // 向上滑动
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    // 当滚动到倒数第2个item时，触发加载更多
                    if (!nftLoadingMore && nftHasMore && 
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        Log.d("MyCenter", "滚动到底部，触发加载更多");
                        loadMoreNfts();
                    }
                }
            }
        });
        
        // 设置RecyclerView的下拉刷新监听
        setupNftPullRefresh();
        
        // 默认显示提交历史
        switchToSubmissionsTab();
    }
    
    private void initEvents() {
        // Tab切换
        submissionsTab.setOnClickListener(v -> switchToSubmissionsTab());
        nftsTab.setOnClickListener(v -> {
            Log.d("MyCenter", "用户点击了NFT Tab");
            switchToNftsTab();
        });
        
    }
    
    private void switchToSubmissionsTab() {
        submissionsTab.setSelected(true);
        nftsTab.setSelected(false);
        
        // 显示提交历史内容区域
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
        
        // 显示NFT内容区域
        findViewById(R.id.nftsContent).setVisibility(View.VISIBLE);
        findViewById(R.id.submissionsContent).setVisibility(View.GONE);
        
        nftRecyclerView.setVisibility(View.VISIBLE);
        submissionsRecyclerView.setVisibility(View.GONE);
        
        // 智能缓存：如果有缓存，直接显示；如果没有，首次加载
        if (nftList.isEmpty()) {
            // 首次加载
            resetNftPagination();
            Log.d("MyCenter", "首次加载NFT数据");
            loadMyNfts();
        } else {
            // 有缓存，直接显示
            Log.d("MyCenter", "Using cached NFT data, total: " + nftList.size() + ", count: " + totalNftCount + ", pull to refresh");
            nftRecyclerView.setVisibility(View.VISIBLE);
            // Update NFT total count display
            nftTotalCountText.setText("Total: " + totalNftCount);  // ✅ Use cached totalNftCount
            nftTotalCountText.setVisibility(View.VISIBLE);
            // 更新Adapter的分页状态
            nftAdapter.setHasMore(nftHasMore);
            nftAdapter.setLoading(false);
        }
    }
    
    /**
     * 检查地址变化并恢复NFT缓存
     */
    private void checkAndRestoreNftCache() {
        String currentAddress = getMyAddressForDatabase();
        
        // 检查缓存的地址是否与当前地址一致
        if (cachedWalletAddress != null && cachedWalletAddress.equals(currentAddress)) {
            // 地址未变化，恢复缓存
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
            // 地址发生变化，清空旧缓存
            if (cachedWalletAddress != null) {
                Log.d("MyCenter", "Address changed from " + cachedWalletAddress + " to " + currentAddress + ", clearing cache");
            } else {
                Log.d("MyCenter", "First time loading, address=" + currentAddress);
            }
            clearNftCache();
        }
    }
    
    /**
     * 清空NFT缓存
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
     * 保存NFT缓存
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
     * 重置NFT分页状态
     */
    private void resetNftPagination() {
        nftCurrentPage = 0;
        nftLoadingMore = false;
        nftHasMore = true;
        Log.d("MyCenter", "重置NFT分页状态");
    }
    
    /**
     * 手动加载更多NFT
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
        
        // 获取当前用户地址
        String myAddress = getMyAddress();
        
        Log.d("MyCenter", "查询勋章数据，地址: " + myAddress);
        
        // 通过后端API查询勋章数据
        new Thread(() -> {
            try {
                // 构建API请求URL
                String apiUrl = "http://127.0.0.1:5000/api/blockchain/medals/" + myAddress;
                
                // 发送HTTP GET请求
                java.net.URL url = new java.net.URL(apiUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // 读取响应
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // 解析JSON响应
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
                
                // 查询失败
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
        
        // 获取当前用户地址（提交历史需要不带0x前缀的地址）
        String myAddress = getMyAddressForDatabase();
        Log.d("MyCenter", "查询提交历史，地址: " + myAddress);
        
        // 使用SubmissionHistoryUtil查询真实提交历史
        SubmissionHistoryUtil.getUserSubmissions(myAddress, 0, 20, new SubmissionHistoryUtil.SubmissionHistoryCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    hideSubmissionsLoading();
                    try {
                        // 解析JSON响应
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.optBoolean("success", false);
                        
                        if (success) {
                            JSONArray dataArray = jsonResponse.optJSONArray("data");
                            if (dataArray != null && dataArray.length() > 0) {
                                submissionsList.clear();
                                
                                // 解析提交记录
                                for (int i = 0; i < dataArray.length(); i++) {
                                    JSONObject submission = dataArray.getJSONObject(i);
                                    
                                    SubmissionRecord record = new SubmissionRecord();
                                    
                                    // 基本信息
                                    record.setId(submission.optLong("id", 0L));
                                    record.setSubmissionId(submission.optString("submissionId", ""));
                                    record.setBatchId(submission.optString("batchId", null));
                                    record.setFileCount(submission.optInt("fileCount", 1));
                                    record.setFileName(submission.optString("fileName", ""));
                                    record.setFileSize(submission.optLong("fileSize", 0L));
                                    record.setFileType(submission.optString("fileType", ""));
                                    record.setUploadTime(submission.optString("uploadTime", ""));
                                    
                                    // 审核信息
                                    record.setAuditStatus(submission.optString("auditStatus", ""));
                                    record.setAuditStatusDesc(submission.optString("auditStatusDesc", "未知状态"));
                                    record.setAuditTime(submission.optString("auditTime", ""));
                                    
                                    // 勋章信息
                                    record.setMedalAwarded(submission.optString("medalAwarded", "NONE"));
                                    record.setMedalAwardedDesc(submission.optString("medalAwardedDesc", "无"));
                                    record.setMedalAwardTime(submission.optString("medalAwardTime", ""));
                                    record.setMedalTransactionHash(submission.optString("medalTransactionHash", ""));
                                    
                                    // NFT信息
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
                                    
                                    // 代币奖励信息
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
                                
                                // 更新UI
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
        // 防止重复加载
        if (nftLoadingMore) {
            Log.d("MyCenter", "NFT正在加载中，跳过重复请求");
            return;
        }
        
        if (nftCurrentPage == 0) {
            // 首次加载，显示加载状态
            showNftLoading();
            nftList.clear();
        } else {
            // 分页加载，显示加载更多状态
            nftLoadingMore = true;
            runOnUiThread(() -> {
                nftAdapter.setLoading(true);
            });
            Log.d("MyCenter", "开始分页加载NFT，页码: " + nftCurrentPage);
        }
        
        // 获取当前用户地址
        String myAddress = getMyAddress();
        
        Log.d("MyCenter", "查询NFT数据，地址: " + myAddress + ", 页码: " + nftCurrentPage);
        
        // 通过后端API查询NFT数据
        new Thread(() -> {
            try {
                // 构建API请求URL，添加分页参数
                String apiUrl = "http://127.0.0.1:5000/api/blockchain/nft/user/" + myAddress + 
                              "?page=" + nftCurrentPage + "&size=" + nftPageSize;
                
                // 发送HTTP GET请求，增加超时时间
                java.net.URL url = new java.net.URL(apiUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000); // 增加连接超时时间
                connection.setReadTimeout(30000); // 增加读取超时时间
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // 读取响应
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // 解析JSON响应
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    Log.d("MyCenter", "NFT API响应: " + response.toString());
                    
                    // 获取NFT总数
                    int totalFromResponse = jsonResponse.optInt("totalCount", 0);
                    totalNftCount = totalFromResponse;
                    Log.d("MyCenter", "NFT总数: " + totalNftCount);
                    
                    JSONArray nfts = jsonResponse.optJSONArray("nfts");
                    Log.d("MyCenter", "NFT数组: " + (nfts != null ? nfts.length() + "个NFT" : "null"));
                    
                    // 详细调试信息
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
                                // 首次加载，隐藏加载状态
                                hideNftLoading();
                            } else {
                                // 分页加载，隐藏加载更多状态
                                nftLoadingMore = false;
                            }
                            
                            if (nfts.length() > 0) {
                                // 添加查询到的NFT数据
                                int addedCount = 0;
                                for (int i = 0; i < nfts.length(); i++) {
                                    try {
                                        JSONObject nft = nfts.getJSONObject(i);
                                        String name = nft.optString("name", "NFT #" + nft.optString("tokenId", ""));
                                        String description = nft.optString("description", "暂无描述");
                                        String imageUrl = nft.optString("imageUrl", "");
                                        
                                        // 检查图片URL格式并处理
                                        if (imageUrl != null && imageUrl.startsWith("{")) {
                                            // ✅ 新格式：JSON元数据（包含后端路径）
                                            try {
                                                JSONObject imageMetadata = new JSONObject(imageUrl);
                                                String storageType = imageMetadata.optString("storageType", "");
                                                
                                                if ("backend-server".equals(storageType)) {
                                                    String path = imageMetadata.optString("path", "");
                                                    String serverUrl = imageMetadata.optString("serverUrl", "http://localhost:5000");
                                                    
                                                    if (!path.isEmpty()) {
                                                        // 拼接完整URL
                                                        imageUrl = serverUrl + path;
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
                                            // 旧格式：Base64数据
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
                                        
                                        // 获取NFT铸造时间（不获取持有者地址，因为是"我的"界面）
                                        String mintTime = nft.optString("mintTime", "");
                                        
                                        // 解析attributes获取材料上传时间
                                        String uploadTime = "";
                                        if (nft.has("attributes")) {
                                            Object attrObj = nft.opt("attributes");
                                            String attributesStr = "";
                                            if (attrObj instanceof String) {
                                                attributesStr = (String) attrObj;
                                            } else if (attrObj instanceof JSONObject || attrObj instanceof org.json.JSONArray) {
                                                attributesStr = attrObj.toString();
                                            }
                                            
                                            // 尝试从attributes JSON中提取timestamp
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
                                        // 不设置ownerAddress，因为是"我的"界面，持有者就是当前用户
                                        nftList.add(nftItem);
                                        addedCount++;
                                    } catch (JSONException e) {
                                        Log.e("MyCenter", "解析NFT数据失败: " + e.getMessage());
                                    }
                                }
                                
                                // 检查是否还有更多数据
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
                                
                                // 显示NFT总数
                                nftTotalCountText.setText("总数: " + totalNftCount);
                                nftTotalCountText.setVisibility(View.VISIBLE);
                                
                                // 更新适配器状态
                                nftAdapter.setHasMore(nftHasMore);
                                nftAdapter.setLoading(false);
                                
                                // 保存NFT缓存
                                saveNftCache();
                                
                                // 停止下拉刷新动画
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
                
                // 查询失败
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
     * 优化Base64图片数据，压缩到合适尺寸
     */
    private String optimizeBase64Image(String base64Data) {
        try {
            // 首先验证数据格式
            if (base64Data == null || base64Data.isEmpty()) {
                Log.w("MyCenter", "Base64数据为空");
                return null;
            }
            
            // 检查是否是有效的data URL格式
            if (!base64Data.startsWith("data:image/")) {
                Log.w("MyCenter", "Base64数据格式不正确，不是有效的图片数据URL");
                return null;
            }
            
            // 验证Base64数据是否有效
            if (!isValidBase64DataUrl(base64Data)) {
                Log.w("MyCenter", "Base64数据无效，无法解码");
                return null;
            }
            
            // 检查数据大小，如果小于100KB则直接返回
            if (base64Data.length() < 100000) {
                Log.d("MyCenter", "Base64图片数据较小，直接使用");
                return base64Data;
            }
            
            // 提取Base64数据部分
            String[] parts = base64Data.split(",");
            if (parts.length != 2) {
                Log.w("MyCenter", "Base64数据格式错误，使用占位符");
                return null;
            }
            
            String mimeType = parts[0];
            String base64String = parts[1];
            
            // 解码Base64数据
            byte[] imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
            
            // 使用BitmapFactory压缩图片
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
            
            // 计算压缩比例，目标尺寸300x300
            int targetSize = 300;
            int scale = Math.max(options.outWidth / targetSize, options.outHeight / targetSize);
            options.inJustDecodeBounds = false;
            options.inSampleSize = Math.max(1, scale);
            options.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565; // 减少内存使用
            
            // 解码压缩后的图片
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
            
            if (bitmap == null) {
                Log.w("MyCenter", "图片解码失败，使用占位符");
                return null;
            }
            
            // 进一步压缩到目标尺寸
            if (bitmap.getWidth() > targetSize || bitmap.getHeight() > targetSize) {
                android.graphics.Bitmap scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true);
                bitmap.recycle();
                bitmap = scaledBitmap;
            }
            
            // 转换为压缩的Base64
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            android.graphics.Bitmap.CompressFormat format = android.graphics.Bitmap.CompressFormat.JPEG;
            if (mimeType.contains("png")) {
                format = android.graphics.Bitmap.CompressFormat.PNG;
            }
            bitmap.compress(format, 80, baos); // 80%质量
            bitmap.recycle();
            
            byte[] compressedBytes = baos.toByteArray();
            String compressedBase64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.DEFAULT);
            
            Log.d("MyCenter", "Base64图片优化完成，原始大小: " + base64String.length() + 
                  ", 压缩后大小: " + compressedBase64.length());
            
            return mimeType + "," + compressedBase64;
            
        } catch (Exception e) {
            Log.e("MyCenter", "Base64图片优化失败: " + e.getMessage());
            return null; // 优化失败时使用占位符
        }
    }
    
    /**
     * 验证Base64数据URL是否有效
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
            // 尝试解码Base64
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
     * 获取当前用户地址（用于区块链API，带0x前缀）
     */
    private String getMyAddress() {
        try {
            String privateKey = StorageUtil.getCurrentPrivatekey(this);
            Log.d("MyCenter", "从存储获取的私钥: " + (privateKey != null ? "有私钥" : "无私钥"));
            
            if (privateKey != null) {
                String address = SecurityUtil.GetAddress(privateKey);
                // 确保地址有0x前缀，因为后端API期望带0x前缀的以太坊地址
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
     * 获取当前用户地址（用于数据库查询，不带0x前缀）
     */
    private String getMyAddressForDatabase() {
        try {
            String privateKey = StorageUtil.getCurrentPrivatekey(this);
            Log.d("MyCenter", "从存储获取的私钥: " + (privateKey != null ? "有私钥" : "无私钥"));
            
            if (privateKey != null) {
                String address = SecurityUtil.GetAddress(privateKey);
                // 去掉0x前缀，因为数据库中的地址没有0x前缀
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
     * 获取当前用户的私钥
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
     * 设置NFT下拉刷新功能（顶部下拉刷新 + 底部加载更多）
     */
    private void setupNftPullRefresh() {
        // 1. 设置顶部下拉刷新（使用SwipeRefreshLayout）
        if (nftSwipeRefreshLayout != null) {
            nftSwipeRefreshLayout.setOnRefreshListener(() -> {
                Log.d("MyCenter", "用户顶部下拉刷新NFT");
                refreshNfts();
            });
            
            // 设置刷新动画颜色
            nftSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            );
        }
        
        // 2. 保留底部加载更多功能（使用Adapter的LoadMoreListener）
        nftAdapter.setLoadMoreListener(() -> {
            if (!nftLoadingMore && nftHasMore) {
                Log.d("MyCenter", "用户触发底部加载更多");
                loadMoreNfts();
            }
        });
    }
    
    /**
     * 刷新NFT（重置分页，重新加载）
     */
    private void refreshNfts() {
        Log.d("MyCenter", "刷新NFT列表");
        resetNftPagination();
        nftList.clear();
        nftAdapter.notifyDataSetChanged();
        loadMyNfts();
    }
    
    /**
     * 停止下拉刷新动画
     */
    private void stopNftRefreshing() {
        if (nftSwipeRefreshLayout != null && nftSwipeRefreshLayout.isRefreshing()) {
            nftSwipeRefreshLayout.setRefreshing(false);
        }
    }
    
    /**
     * 处理下拉手势（手指向上滑动）
     */
    private void handlePullGesture(float deltaY) {
        Log.d("MyCenter", "处理下拉手势（向上滑动）: deltaY=" + deltaY);
        
        if (!nftHasMore) {
            // 没有更多数据，显示到底提示
            nftAdapter.setHasMore(false);
            nftAdapter.setLoading(false);
            return;
        }
        
        if (deltaY > 15) {
            // 向上拉动超过15像素，显示松手提示
            Log.d("MyCenter", "显示松手刷新~");
            nftAdapter.setHasMore(true);
            nftAdapter.setLoading(false);
        } else if (deltaY > 5) {
            // 向上拉动5-15像素，显示下拉提示
            Log.d("MyCenter", "显示下拉刷新更多");
            nftAdapter.setHasMore(true);
            nftAdapter.setLoading(false);
        } else {
            // 向上拉动不足5像素，显示默认提示
            nftAdapter.setHasMore(true);
            nftAdapter.setLoading(false);
        }
    }
    
    /**
     * 重置下拉状态
     */
    private void resetPullState() {
        Log.d("MyCenter", "重置下拉状态");
        if (!isRefreshing) {
            nftAdapter.setHasMore(nftHasMore);
            nftAdapter.setLoading(false);
        }
    }
    
    /**
     * 触发下拉刷新
     */
    private void triggerPullRefresh() {
        if (!isRefreshing && nftHasMore) {
            isRefreshing = true;
            Log.d("MyCenter", "触发下拉刷新");
            
            // 显示加载状态
            nftAdapter.setHasMore(true);
            nftAdapter.setLoading(true);
            
            // 触发加载更多
            loadMoreNfts();
        }
    }
    
    /**
     * 检查View是否完全可见
     */
    private boolean isViewFullyVisible(View view) {
        if (view == null) return false;
        
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        
        int viewTop = location[1];
        int viewBottom = viewTop + view.getHeight();
        
        // 获取RecyclerView的可见区域
        int[] recyclerViewLocation = new int[2];
        nftRecyclerView.getLocationOnScreen(recyclerViewLocation);
        int recyclerViewTop = recyclerViewLocation[1];
        int recyclerViewBottom = recyclerViewTop + nftRecyclerView.getHeight();
        
        // 检查footer item是否完全在RecyclerView的可见区域内
        boolean isFullyVisible = viewTop >= recyclerViewTop && viewBottom <= recyclerViewBottom;
        
        Log.d("MyCenter", "Footer可见性检查: viewTop=" + viewTop + ", viewBottom=" + viewBottom + 
              ", recyclerViewTop=" + recyclerViewTop + ", recyclerViewBottom=" + recyclerViewBottom + 
              ", isFullyVisible=" + isFullyVisible);
        
        return isFullyVisible;
    }
    
    /**
     * 检查footer是否可见
     */
    private void checkFooterVisibility() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) nftRecyclerView.getLayoutManager();
        if (layoutManager != null) {
            int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
            int totalItemCount = nftRecyclerView.getAdapter().getItemCount();
            
            // 检查是否滚动到最后一个item（footer）
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
     * 显示NFT详情对话框（图片 + 2个时间属性）
     */
    private void showNftDetailDialog(NFTViewActivity.NFTItem nftItem) {
        // 创建对话框
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nft_detail, null);
        builder.setView(dialogView);
        
        // 获取控件
        ImageView nftImageView = dialogView.findViewById(R.id.nftImageView);
        LinearLayout attributesContainer = dialogView.findViewById(R.id.attributesContainer);
        
        // 加载NFT图片
        // 详情对话框中使用原始分辨率，保证画质
        if (nftImageView != null && nftItem.getImageUrl() != null && !nftItem.getImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(nftItem.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL) // 使用原图尺寸
                    .fitCenter() // 完整显示图片，不裁剪
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // 缓存原图
                    .into(nftImageView);
            Log.d("MyCenter", "详情对话框加载高清原图: " + nftItem.getImageUrl());
        }
        
        // 显示时间属性
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
            
            // "我的"界面不显示持有者地址（持有者就是当前用户）
        }
        
        // 创建对话框
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        dialog.show();
        Log.d("MyCenter", "显示NFT详情对话框");
    }
    
    
    /**
     * 添加单个属性显示项
     */
    private void addAttributeItem(android.widget.LinearLayout container, String label, String value) {
        if (value == null || value.isEmpty() || "null".equals(value)) {
            return; // 跳过空值
        }
        
        // 创建属性行
        android.widget.LinearLayout attributeRow = new android.widget.LinearLayout(this);
        attributeRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        android.widget.LinearLayout.LayoutParams rowParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, 8);
        attributeRow.setLayoutParams(rowParams);
        
        // 标签
        android.widget.TextView labelView = new android.widget.TextView(this);
        labelView.setText(label + ": ");
        labelView.setTextSize(14);
        labelView.setTextColor(getResources().getColor(R.color.grey_60));
        android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelView.setLayoutParams(labelParams);
        
        // 值
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
     * 格式化时间戳（ISO 8601格式转为可读格式）
     */
    private String formatTimestamp(String timestamp) {
        try {
            // ISO 8601格式: 2025-10-07T18:48:12.345Z
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
