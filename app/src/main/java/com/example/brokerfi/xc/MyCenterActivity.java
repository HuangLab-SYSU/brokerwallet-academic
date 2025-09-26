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
    
    // NFT分页加载相关
    private int nftCurrentPage = 0;
    private int nftPageSize = 5; // 每页加载5个NFT
    private boolean nftLoadingMore = false;
    private boolean nftHasMore = true;
    
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
        
        // 只有在NFT列表为空时才重新加载
        if (nftList.isEmpty()) {
            // 重置NFT分页状态
            resetNftPagination();
            Log.d("MyCenter", "Tab切换完成，开始加载NFT数据");
            loadMyNfts();
        } else {
            Log.d("MyCenter", "NFT数据已存在，直接显示");
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
                                    String fileName = submission.optString("fileName", "");
                                    String auditStatusDesc = submission.optString("auditStatusDesc", "未知状态");
                                    String medalAwardedDesc = submission.optString("medalAwardedDesc", "无");
                                    String nftImage = submission.optString("nftImage", "");
                                    
                                    SubmissionRecord record = new SubmissionRecord();
                                    record.setFileName(fileName);
                                    record.setAuditStatusDesc(auditStatusDesc);
                                    record.setMedalAwardedDesc(medalAwardedDesc);
                                    submissionsList.add(record);
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
                                        
                                        // 检查图片URL是否为base64数据，如果是则进行优化处理
                                        if (imageUrl.startsWith("data:image/")) {
                                            Log.d("MyCenter", "检测到base64图片数据，进行优化处理");
                                            String optimizedUrl = optimizeBase64Image(imageUrl);
                                            if (optimizedUrl != null) {
                                                imageUrl = optimizedUrl;
                                                Log.d("MyCenter", "Base64图片优化成功，使用优化后的URL");
                                            } else {
                                                Log.d("MyCenter", "Base64图片优化失败，使用占位符");
                                                imageUrl = null;
                                            }
                                        }
                                        
                                        Log.d("MyCenter", "添加NFT: " + name + ", 图片URL: " + (imageUrl != null ? "有图片" : "无图片"));
                                        nftList.add(new NFTViewActivity.NFTItem(name, description, imageUrl));
                                        addedCount++;
                                    } catch (JSONException e) {
                                        Log.e("MyCenter", "解析NFT数据失败: " + e.getMessage());
                                    }
                                }
                                
                                // 检查是否还有更多数据
                                if (nfts.length() < nftPageSize) {
                                    nftHasMore = false;
                                    Log.d("MyCenter", "没有更多NFT数据了");
                                } else {
                                    nftCurrentPage++;
                                    Log.d("MyCenter", "还有更多NFT数据，下一页: " + nftCurrentPage);
                                }
                                
                                Log.d("MyCenter", "添加NFT数据完成，本次添加: " + addedCount + ", 总数量: " + nftList.size());
                                nftAdapter.updateData(nftList);
                                Log.d("MyCenter", "通知适配器更新完成");
                                
                                nftRecyclerView.setVisibility(View.VISIBLE);
                                Log.d("MyCenter", "设置RecyclerView可见");
                                
                                // 更新适配器状态
                                nftAdapter.setHasMore(nftHasMore);
                                nftAdapter.setLoading(false);
                                
                                // 完成下拉刷新
                                if (isRefreshing) {
                                    isRefreshing = false;
                                    Log.d("MyCenter", "下拉刷新完成");
                                }
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
            // 检查数据大小，如果小于100KB则直接返回
            if (base64Data.length() < 100000) {
                Log.d("MyCenter", "Base64图片数据较小，直接使用");
                // 即使是小数据，也检查格式是否正确
                if (base64Data.startsWith("data:image/")) {
                    return base64Data;
                } else {
                    Log.w("MyCenter", "Base64数据格式不正确，使用占位符");
                    return null;
                }
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
                    Log.d("MyCenter", "触摸开始: " + startY + ", footer可见: " + isFooterVisible);
                    break;
                    
                case android.view.MotionEvent.ACTION_MOVE:
                    if (isDragging && !isRefreshing && isFooterVisible) {
                        currentY = event.getY();
                        float deltaY = currentY - startY;
                        Log.d("MyCenter", "触摸移动: deltaY=" + deltaY + ", footer可见: " + isFooterVisible);
                        
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
                        Log.d("MyCenter", "触摸结束: deltaY=" + deltaY + ", footer可见: " + isFooterVisible);
                        
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
     * 显示NFT详情对话框
     */
    private void showNftDetailDialog(NFTViewActivity.NFTItem nftItem) {
        // 创建对话框 - 使用AlertDialog.Builder避免黑色背景
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        // 创建自定义视图
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nft_detail, null);
        builder.setView(dialogView);
        
        // 获取视图组件
        ImageView closeButton = dialogView.findViewById(R.id.closeButton);
        ImageView detailImageView = dialogView.findViewById(R.id.detailImageView);
        TextView detailNameText = dialogView.findViewById(R.id.detailNameText);
        TextView detailDescriptionText = dialogView.findViewById(R.id.detailDescriptionText);
        TextView expandDescriptionButton = dialogView.findViewById(R.id.expandDescriptionButton);
        
        // 设置NFT信息
        detailNameText.setText(nftItem.getName());
        detailDescriptionText.setText(nftItem.getDescription());
        
        // 检查描述长度，决定是否显示展开按钮
        String description = nftItem.getDescription();
        if (description != null && description.length() > 100) {
            expandDescriptionButton.setVisibility(android.view.View.VISIBLE);
            
            // 设置展开/收起功能
            expandDescriptionButton.setOnClickListener(v -> {
                if (detailDescriptionText.getMaxLines() == 4) {
                    // 展开
                    detailDescriptionText.setMaxLines(Integer.MAX_VALUE);
                    expandDescriptionButton.setText("收起");
                } else {
                    // 收起
                    detailDescriptionText.setMaxLines(4);
                    expandDescriptionButton.setText("展开");
                }
            });
        } else {
            expandDescriptionButton.setVisibility(android.view.View.GONE);
        }
        
        // 优化图片加载 - 使用fitCenter显示完整图片
        if (nftItem.getImageUrl() != null && !nftItem.getImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(nftItem.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .fitCenter() // 使用fitCenter显示完整图片
                    .into(detailImageView);
        } else {
            detailImageView.setImageResource(R.drawable.placeholder_image);
        }
        
        // 创建对话框
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        // 设置关闭按钮点击事件
        closeButton.setOnClickListener(v -> {
            Log.d("MyCenter", "用户点击了关闭按钮");
            dialog.dismiss();
        });
        
        // 设置背景点击关闭
        dialogView.findViewById(R.id.dialogBackground).setOnClickListener(v -> {
            Log.d("MyCenter", "用户点击了背景");
            dialog.dismiss();
        });
        
        // 显示对话框
        dialog.show();
        Log.d("MyCenter", "显示NFT详情对话框: " + nftItem.getName());
    }
}
