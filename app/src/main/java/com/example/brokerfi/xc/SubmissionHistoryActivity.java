package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
// import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
import com.example.brokerfi.xc.adapter.SubmissionHistoryAdapter;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.example.brokerfi.xc.model.SubmissionRecord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 提交历史Activity
 * 显示用户的所有提交记录和状态
 */
public class SubmissionHistoryActivity extends AppCompatActivity {
    
    private static final String TAG = "SubmissionHistory";
    
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    
    // private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private SubmissionHistoryAdapter adapter;
    private List<SubmissionRecord> submissionList;
    
    private TextView loadingText;
    private TextView errorText;
    private LinearLayout emptyStateLayout;
    private TextView retryButton;
    
    private int currentPage = 0;
    private final int pageSize = 10;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission_history);
        
        initializeViews();
        setupUI();
        loadSubmissionHistory();
    }
    
    /**
     * 初始化视图
     */
    private void initializeViews() {
        // 导航相关
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);
        
        // 列表相关
        // swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        loadingText = findViewById(R.id.loadingText);
        errorText = findViewById(R.id.errorText);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        retryButton = findViewById(R.id.retryButton);
        
        // 初始化数据
        submissionList = new ArrayList<>();
        adapter = new SubmissionHistoryAdapter(this, submissionList);
    }
    
    /**
     * 设置UI
     */
    private void setupUI() {
        // 设置导航（简化版本）
        menu.setOnClickListener(v -> {
            // 可以在这里添加菜单逻辑，暂时留空
        });
        notificationBtn.setOnClickListener(v -> {
            // 可以在这里添加通知逻辑，暂时留空  
        });
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        // 设置下拉刷新（暂时注释掉）
        // swipeRefreshLayout.setOnRefreshListener(this::refreshData);
        // swipeRefreshLayout.setColorSchemeResources(
        //     android.R.color.holo_blue_bright,
        //     android.R.color.holo_green_light,
        //     android.R.color.holo_orange_light,
        //     android.R.color.holo_red_light
        // );
        
        // 设置重试按钮
        retryButton.setOnClickListener(v -> {
            hideError();
            loadSubmissionHistory();
        });
        
        // 设置滚动监听（用于分页加载）
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && hasMoreData) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        loadMoreData();
                    }
                }
            }
        });
    }
    
    /**
     * 加载提交历史
     */
    private void loadSubmissionHistory() {
        if (isLoading) {
            return;
        }
        
        String walletAddress = getCurrentWalletAddress();
        if (walletAddress == null) {
            showError("无法获取钱包地址，请检查账户设置");
            return;
        }
        
        showLoading();
        isLoading = true;
        currentPage = 0;
        
        SubmissionHistoryUtil.getUserSubmissions(walletAddress, currentPage, pageSize, 
            new SubmissionHistoryUtil.SubmissionHistoryCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        isLoading = false;
                        hideLoading();
                        // swipeRefreshLayout.setRefreshing(false);
                        handleSubmissionHistorySuccess(response);
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        isLoading = false;
                        hideLoading();
                        // swipeRefreshLayout.setRefreshing(false);
                        handleSubmissionHistoryError(error);
                    });
                }
            });
    }
    
    /**
     * 加载更多数据
     */
    private void loadMoreData() {
        if (isLoading || !hasMoreData) {
            return;
        }
        
        String walletAddress = getCurrentWalletAddress();
        if (walletAddress == null) {
            return;
        }
        
        isLoading = true;
        currentPage++;
        
        SubmissionHistoryUtil.getUserSubmissions(walletAddress, currentPage, pageSize,
            new SubmissionHistoryUtil.SubmissionHistoryCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        isLoading = false;
                        handleLoadMoreSuccess(response);
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        isLoading = false;
                        currentPage--; // 回退页码
                        Toast.makeText(SubmissionHistoryActivity.this, "加载更多失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }
    
    /**
     * 刷新数据
     */
    private void refreshData() {
        currentPage = 0;
        hasMoreData = true;
        loadSubmissionHistory();
    }
    
    /**
     * 处理提交历史成功响应
     */
    private void handleSubmissionHistorySuccess(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            
            if (jsonResponse.getBoolean("success")) {
                JSONArray dataArray = jsonResponse.getJSONArray("data");
                
                // 清空现有数据
                submissionList.clear();
                
                // 解析数据
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    SubmissionRecord record = parseSubmissionRecord(item);
                    if (record != null) {
                        submissionList.add(record);
                    }
                }
                
                // 检查分页信息
                if (jsonResponse.has("pagination")) {
                    JSONObject pagination = jsonResponse.getJSONObject("pagination");
                    int currentPageNum = pagination.getInt("currentPage");
                    int totalPages = pagination.getInt("totalPages");
                    hasMoreData = currentPageNum < totalPages - 1;
                }
                
                // 更新UI
                adapter.notifyDataSetChanged();
                
                if (submissionList.isEmpty()) {
                    showEmptyState();
                } else {
                    hideEmptyState();
                }
                
            } else {
                String message = jsonResponse.optString("message", "获取提交历史失败");
                showError(message);
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "解析提交历史响应失败", e);
            showError("数据解析失败");
        }
    }
    
    /**
     * 处理加载更多成功响应
     */
    private void handleLoadMoreSuccess(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            
            if (jsonResponse.getBoolean("success")) {
                JSONArray dataArray = jsonResponse.getJSONArray("data");
                
                // 添加新数据
                int startPosition = submissionList.size();
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    SubmissionRecord record = parseSubmissionRecord(item);
                    if (record != null) {
                        submissionList.add(record);
                    }
                }
                
                // 检查是否还有更多数据
                if (jsonResponse.has("pagination")) {
                    JSONObject pagination = jsonResponse.getJSONObject("pagination");
                    int currentPageNum = pagination.getInt("currentPage");
                    int totalPages = pagination.getInt("totalPages");
                    hasMoreData = currentPageNum < totalPages - 1;
                }
                
                // 通知适配器数据变化
                adapter.notifyItemRangeInserted(startPosition, dataArray.length());
                
            } else {
                currentPage--; // 回退页码
                Toast.makeText(this, "加载更多失败", Toast.LENGTH_SHORT).show();
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "解析加载更多响应失败", e);
            currentPage--; // 回退页码
            Toast.makeText(this, "数据解析失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 解析提交记录
     */
    private SubmissionRecord parseSubmissionRecord(JSONObject item) {
        try {
            SubmissionRecord record = new SubmissionRecord();
            
            record.setSubmissionId(item.optString("submissionId"));
            record.setId(item.optLong("id"));
            record.setFileName(item.optString("fileName"));
            record.setFileSize(item.optLong("fileSize"));
            record.setFileType(item.optString("fileType"));
            record.setUploadTime(item.optString("uploadTime"));
            record.setAuditStatus(item.optString("auditStatus"));
            record.setAuditStatusDesc(item.optString("auditStatusDesc"));
            record.setAuditTime(item.optString("auditTime"));
            record.setMedalAwarded(item.optString("medalAwarded"));
            record.setMedalAwardedDesc(item.optString("medalAwardedDesc"));
            record.setMedalAwardTime(item.optString("medalAwardTime"));
            record.setMedalTransactionHash(item.optString("medalTransactionHash"));
            
            // 解析NFT图片信息
            if (item.has("nftImage")) {
                JSONObject nftImageJson = item.getJSONObject("nftImage");
                SubmissionRecord.NftImageInfo nftImage = new SubmissionRecord.NftImageInfo();
                nftImage.setId(nftImageJson.optLong("id"));
                nftImage.setOriginalName(nftImageJson.optString("originalName"));
                nftImage.setMintStatus(nftImageJson.optString("mintStatus"));
                nftImage.setMintStatusDesc(nftImageJson.optString("mintStatusDesc"));
                nftImage.setTokenId(nftImageJson.optString("tokenId"));
                nftImage.setTransactionHash(nftImageJson.optString("transactionHash"));
                record.setNftImage(nftImage);
                record.setHasNftImage(true);
            }
            
            return record;
            
        } catch (Exception e) {
            Log.e(TAG, "解析提交记录失败", e);
            return null;
        }
    }
    
    /**
     * 处理错误
     */
    private void handleSubmissionHistoryError(String error) {
        Log.e(TAG, "获取提交历史失败: " + error);
        showError("获取提交历史失败: " + error);
    }
    
    /**
     * 显示加载状态
     */
    private void showLoading() {
        loadingText.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }
    
    /**
     * 隐藏加载状态
     */
    private void hideLoading() {
        loadingText.setVisibility(View.GONE);
    }
    
    /**
     * 显示错误状态
     */
    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }
    
    /**
     * 隐藏错误状态
     */
    private void hideError() {
        errorText.setVisibility(View.GONE);
    }
    
    /**
     * 显示空状态
     */
    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
    
    /**
     * 隐藏空状态
     */
    private void hideEmptyState() {
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
    
    /**
     * 获取当前钱包地址
     */
    private String getCurrentWalletAddress() {
        try {
            String privateKey = StorageUtil.getCurrentPrivatekey(this);
            if (privateKey != null) {
                return SecurityUtil.GetAddress(privateKey);
            }
        } catch (Exception e) {
            Log.e(TAG, "获取钱包地址失败", e);
        }
        return null;
    }
    
    /**
     * 打开提交详情页面
     */
    public void openSubmissionDetail(SubmissionRecord record) {
        Intent intent = new Intent(this, SubmissionDetailActivity.class);
        intent.putExtra("submissionId", record.getId());
        intent.putExtra("fileName", record.getFileName());
        startActivity(intent);
    }
}

