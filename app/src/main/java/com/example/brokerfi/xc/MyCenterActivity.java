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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MyCenterActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    
    // 勋章概览
    private TextView goldMedalCount;
    private TextView silverMedalCount;
    private TextView bronzeMedalCount;
    private TextView medalLoadingText;
    private TextView medalErrorText;
    
    // 标签页
    private TextView tabSubmissions;
    private TextView tabNfts;
    private LinearLayout submissionsContent;
    private LinearLayout nftsContent;
    
    // 提交历史
    private RecyclerView submissionsRecyclerView;
    private SubmissionHistoryAdapter submissionsAdapter;
    private List<SubmissionRecord> submissionsList;
    private TextView submissionsLoadingText;
    private TextView submissionsErrorText;
    private LinearLayout submissionsEmptyStateLayout;
    
    // NFT显示
    private RecyclerView nftRecyclerView;
    private NFTViewAdapter nftAdapter;
    private List<NFTViewActivity.NFTItem> nftList;
    private TextView nftLoadingText;
    private TextView nftErrorText;
    private LinearLayout nftEmptyStateLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_center);

        initViews();
        initEvents();
        loadUserData();
    }

    private void initViews() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        
        // 勋章概览
        goldMedalCount = findViewById(R.id.goldMedalCount);
        silverMedalCount = findViewById(R.id.silverMedalCount);
        bronzeMedalCount = findViewById(R.id.bronzeMedalCount);
        medalLoadingText = findViewById(R.id.medalLoadingText);
        medalErrorText = findViewById(R.id.medalErrorText);
        
        // 标签页
        tabSubmissions = findViewById(R.id.tabSubmissions);
        tabNfts = findViewById(R.id.tabNfts);
        submissionsContent = findViewById(R.id.submissionsContent);
        nftsContent = findViewById(R.id.nftsContent);
        
        // 提交历史
        submissionsRecyclerView = findViewById(R.id.submissionsRecyclerView);
        submissionsLoadingText = findViewById(R.id.submissionsLoadingText);
        submissionsErrorText = findViewById(R.id.submissionsErrorText);
        submissionsEmptyStateLayout = findViewById(R.id.submissionsEmptyStateLayout);
        
        // NFT显示
        nftRecyclerView = findViewById(R.id.nftRecyclerView);
        nftLoadingText = findViewById(R.id.nftLoadingText);
        nftErrorText = findViewById(R.id.nftErrorText);
        nftEmptyStateLayout = findViewById(R.id.nftEmptyStateLayout);
        
        // 初始化列表
        submissionsList = new ArrayList<>();
        submissionsAdapter = new SubmissionHistoryAdapter(this, submissionsList);
        submissionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        submissionsRecyclerView.setAdapter(submissionsAdapter);
        
        nftList = new ArrayList<>();
        nftAdapter = new NFTViewAdapter(nftList);
        nftRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        nftRecyclerView.setAdapter(nftAdapter);
        
        // 默认显示提交历史
        switchToSubmissionsTab();
    }

    private void initEvents() {
        navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);
        
        tabSubmissions.setOnClickListener(v -> switchToSubmissionsTab());
        tabNfts.setOnClickListener(v -> switchToNftsTab());
    }

    private void switchToSubmissionsTab() {
        tabSubmissions.setBackgroundResource(R.drawable.custom_green_background);
        tabSubmissions.setTextColor(getResources().getColor(R.color.white));
        tabNfts.setBackgroundResource(R.drawable.custom_edittext_border);
        tabNfts.setTextColor(getResources().getColor(R.color.black));
        
        submissionsContent.setVisibility(View.VISIBLE);
        nftsContent.setVisibility(View.GONE);
        
        loadSubmissions();
    }

    private void switchToNftsTab() {
        tabNfts.setBackgroundResource(R.drawable.custom_green_background);
        tabNfts.setTextColor(getResources().getColor(R.color.white));
        tabSubmissions.setBackgroundResource(R.drawable.custom_edittext_border);
        tabSubmissions.setTextColor(getResources().getColor(R.color.black));
        
        nftsContent.setVisibility(View.VISIBLE);
        submissionsContent.setVisibility(View.GONE);
        
        loadMyNfts();
    }

    private void loadUserData() {
        loadMyMedals();
    }

    private void loadMyMedals() {
        showMedalLoading();
        
        // 获取当前用户地址和私钥
        String myAddress = getMyAddress();
        String myPrivateKey = getMyPrivateKey();
        
        Log.d("MyCenter", "查询勋章数据，地址: " + myAddress);
        
        // 使用MedalQueryUtil查询真实勋章数据
        MedalQueryUtil.queryUserMedals(myAddress, myPrivateKey, new MedalQueryUtil.MedalQueryCallback() {
            @Override
            public void onSuccess(ABIUtils.MedalQueryResult result) {
                runOnUiThread(() -> {
                    hideMedalLoading();
                    updateMedalDisplay(result.goldMedals, result.silverMedals, result.bronzeMedals);
                    Log.d("MyCenter", "勋章数据: 金" + result.goldMedals + " 银" + result.silverMedals + " 铜" + result.bronzeMedals);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideMedalLoading();
                    showMedalError();
                    Log.e("MyCenter", "查询勋章失败: " + error);
                });
            }
        });
    }

    private void loadSubmissions() {
        showSubmissionsLoading();
        
        // 获取当前用户地址
        String myAddress = getMyAddress();
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
                                
                                // 解析每个提交记录
                                for (int i = 0; i < dataArray.length(); i++) {
                                    JSONObject submission = dataArray.getJSONObject(i);
                                    SubmissionRecord record = new SubmissionRecord();
                                    
                                    record.setId(submission.optLong("id", 0));
                                    record.setFileName(submission.optString("fileName", ""));
                                    record.setFileType(submission.optString("fileType", ""));
                                    record.setFileSize(submission.optLong("fileSize", 0));
                                    record.setUploadTime(submission.optString("uploadTime", ""));
                                    record.setAuditStatus(submission.optString("auditStatus", "PENDING"));
                                    record.setAuditStatusDesc(submission.optString("auditStatusDesc", "待审核"));
                                    record.setMedalAwarded(submission.optString("medalAwarded", "NONE"));
                                    record.setMedalAwardedDesc(submission.optString("medalAwardedDesc", "无勋章"));
                                    
                                    // 检查是否有NFT图片
                                    if (submission.has("nftImage")) {
                                        JSONObject nftImage = submission.optJSONObject("nftImage");
                                        if (nftImage != null) {
                                            record.setHasNftImage(true);
                                            SubmissionRecord.NftImageInfo nftInfo = new SubmissionRecord.NftImageInfo();
                                            nftInfo.setId(nftImage.optLong("id", 0));
                                            nftInfo.setOriginalName(nftImage.optString("originalName", ""));
                                            nftInfo.setMintStatus(nftImage.optString("mintStatus", "NOT_STARTED"));
                                            nftInfo.setMintStatusDesc(nftImage.optString("mintStatusDesc", "未开始"));
                                            nftInfo.setTokenId(nftImage.optString("tokenId", ""));
                                            nftInfo.setTransactionHash(nftImage.optString("transactionHash", ""));
                                            record.setNftImage(nftInfo);
                                        }
                                    }
                                    
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
                            String message = jsonResponse.optString("message", "查询失败");
                            showSubmissionsError();
                            Log.e("MyCenter", "提交历史查询失败: " + message);
                        }
                    } catch (Exception e) {
                        showSubmissionsError();
                        Log.e("MyCenter", "解析提交历史响应失败", e);
                    }
                    Log.d("MyCenter", "提交历史响应: " + response);
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
        showNftLoading();
        
        // 获取当前用户地址和私钥
        String myAddress = getMyAddress();
        String myPrivateKey = getMyPrivateKey();
        
        Log.d("MyCenter", "查询NFT数据，地址: " + myAddress);
        
        // 使用NFTQueryUtil查询真实NFT数据
        NFTQueryUtil.queryUserNfts(myAddress, myPrivateKey, new NFTQueryUtil.NFTQueryCallback() {
            @Override
            public void onSuccess(ABIUtils.UserNftsResult result) {
                runOnUiThread(() -> {
                    hideNftLoading();
                    nftList.clear();
                    
                    // 添加查询到的NFT数据
                    for (int i = 0; i < result.tokenIds.length; i++) {
                        String name = i < result.names.length ? result.names[i] : "NFT #" + result.tokenIds[i];
                        String description = i < result.descriptions.length ? result.descriptions[i] : "暂无描述";
                        String imageUrl = i < result.imageUrls.length ? result.imageUrls[i] : "https://via.placeholder.com/300x300?text=NFT";
                        
                        nftList.add(new NFTViewActivity.NFTItem(name, description, imageUrl));
                    }
                    
                    nftAdapter.notifyDataSetChanged();
                    
                    if (nftList.isEmpty()) {
                        showNftEmptyState();
                    } else {
                        nftRecyclerView.setVisibility(View.VISIBLE);
                    }
                    
                    Log.d("MyCenter", "NFT数量: " + nftList.size());
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideNftLoading();
                    showNftError();
                    Log.e("MyCenter", "查询NFT失败: " + error);
                });
            }
        });
    }

    private void updateMedalDisplay(int gold, int silver, int bronze) {
        goldMedalCount.setText(String.valueOf(gold));
        silverMedalCount.setText(String.valueOf(silver));
        bronzeMedalCount.setText(String.valueOf(bronze));
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
        submissionsErrorText.setVisibility(View.GONE);
        submissionsEmptyStateLayout.setVisibility(View.VISIBLE);
        submissionsRecyclerView.setVisibility(View.GONE);
    }
    
    private void hideSubmissionsEmptyState() {
        submissionsErrorText.setVisibility(View.GONE);
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
        nftErrorText.setVisibility(View.GONE);
        nftEmptyStateLayout.setVisibility(View.VISIBLE);
        nftRecyclerView.setVisibility(View.GONE);
    }
    
    /**
     * 获取当前用户的以太坊地址
     */
    private String getMyAddress() {
        try {
            String privateKey = StorageUtil.getCurrentPrivatekey(this);
            Log.d("MyCenter", "从存储获取的私钥: " + (privateKey != null ? "有私钥" : "无私钥"));
            
            if (privateKey != null) {
                String address = SecurityUtil.GetAddress(privateKey);
                // 去掉0x前缀，因为后端数据库中的地址没有0x前缀
                if (address.startsWith("0x")) {
                    address = address.substring(2);
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
}