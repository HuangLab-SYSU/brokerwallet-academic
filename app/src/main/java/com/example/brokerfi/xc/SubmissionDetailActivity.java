package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;
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
import com.example.brokerfi.xc.menu.NavigationHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 提交详情Activity
 * 显示单个提交的详细信息和处理进度
 */
public class SubmissionDetailActivity extends AppCompatActivity {
    
    private static final String TAG = "SubmissionDetail";
    
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    
    private TextView loadingText;
    private TextView errorText;
    private LinearLayout detailContainer;
    private TextView retryButton;
    
    private Long submissionId;
    private String fileName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission_detail);
        
        // 获取传递的参数
        Intent intent = getIntent();
        submissionId = intent.getLongExtra("submissionId", -1);
        fileName = intent.getStringExtra("fileName");
        
        if (submissionId == -1) {
            Toast.makeText(this, "无效的提交ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initializeViews();
        setupUI();
        loadSubmissionDetail();
    }
    
    /**
     * 初始化视图
     */
    private void initializeViews() {
        // 导航相关
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notification);
        action_bar = findViewById(R.id.action_bar);
        navigationHelper = new NavigationHelper(this);
        
        // 内容相关
        loadingText = findViewById(R.id.loadingText);
        errorText = findViewById(R.id.errorText);
        detailContainer = findViewById(R.id.detailContainer);
        retryButton = findViewById(R.id.retryButton);
    }
    
    /**
     * 设置UI
     */
    private void setupUI() {
        // 设置导航
        menu.setOnClickListener(v -> navigationHelper.openMenu());
        notificationBtn.setOnClickListener(v -> navigationHelper.openNotifications());
        
        // 设置重试按钮
        retryButton.setOnClickListener(v -> {
            hideError();
            loadSubmissionDetail();
        });
    }
    
    /**
     * 加载提交详情
     */
    private void loadSubmissionDetail() {
        showLoading();
        
        SubmissionHistoryUtil.getSubmissionDetail(submissionId,
            new SubmissionHistoryUtil.SubmissionDetailCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        hideLoading();
                        handleDetailSuccess(response);
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        hideLoading();
                        handleDetailError(error);
                    });
                }
            });
    }
    
    /**
     * 处理详情成功响应
     */
    private void handleDetailSuccess(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            
            if (jsonResponse.getBoolean("success")) {
                JSONObject data = jsonResponse.getJSONObject("data");
                displaySubmissionDetail(data);
            } else {
                String message = jsonResponse.optString("message", "获取详情失败");
                showError(message);
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "解析详情响应失败", e);
            showError("数据解析失败");
        }
    }
    
    /**
     * 显示提交详情
     */
    private void displaySubmissionDetail(JSONObject data) {
        try {
            detailContainer.removeAllViews();
            
            // 文件信息
            addDetailItem("📄 文件名", data.optString("fileName"));
            addDetailItem("📦 文件大小", formatFileSize(data.optLong("fileSize")));
            addDetailItem("📅 提交时间", formatDateTime(data.optString("uploadTime")));
            
            // 审核状态
            String auditStatus = data.optString("auditStatusDesc");
            String auditTime = data.optString("auditTime");
            if (auditTime != null && !auditTime.isEmpty()) {
                auditStatus += " (" + formatDateTime(auditTime) + ")";
            }
            addDetailItem("🔍 审核状态", auditStatus);
            
            // 勋章信息
            String medalDesc = data.optString("medalAwardedDesc");
            String medalTime = data.optString("medalAwardTime");
            if (medalTime != null && !medalTime.isEmpty()) {
                medalDesc += " (" + formatDateTime(medalTime) + ")";
            }
            addDetailItem("🏅 勋章奖励", medalDesc);
            
            // 处理进度
            if (data.has("processSteps")) {
                JSONArray steps = data.getJSONArray("processSteps");
                StringBuilder stepsText = new StringBuilder();
                for (int i = 0; i < steps.length(); i++) {
                    if (i > 0) stepsText.append("\n");
                    stepsText.append(steps.getString(i));
                }
                addDetailItem("📋 处理进度", stepsText.toString());
            }
            
            // 用户信息
            if (data.has("user")) {
                JSONObject user = data.getJSONObject("user");
                addDetailItem("👤 提交用户", user.optString("displayName", "未设置"));
                addDetailItem("🏆 总勋章数", String.valueOf(user.optInt("totalMedals")));
            }
            
            detailContainer.setVisibility(View.VISIBLE);
            
        } catch (JSONException e) {
            Log.e(TAG, "显示详情失败", e);
            showError("显示详情失败");
        }
    }
    
    /**
     * 添加详情项
     */
    private void addDetailItem(String label, String value) {
        View itemView = getLayoutInflater().inflate(R.layout.item_detail_info, detailContainer, false);
        
        TextView labelText = itemView.findViewById(R.id.labelText);
        TextView valueText = itemView.findViewById(R.id.valueText);
        
        labelText.setText(label);
        valueText.setText(value);
        
        detailContainer.addView(itemView);
    }
    
    /**
     * 处理错误
     */
    private void handleDetailError(String error) {
        Log.e(TAG, "获取提交详情失败: " + error);
        showError("获取详情失败: " + error);
    }
    
    /**
     * 显示加载状态
     */
    private void showLoading() {
        loadingText.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        detailContainer.setVisibility(View.GONE);
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
        detailContainer.setVisibility(View.GONE);
    }
    
    /**
     * 隐藏错误状态
     */
    private void hideError() {
        errorText.setVisibility(View.GONE);
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 格式化日期时间
     */
    private String formatDateTime(String dateTime) {
        try {
            if (dateTime != null && dateTime.length() >= 16) {
                return dateTime.substring(0, 16).replace('T', ' ');
            }
            return dateTime;
        } catch (Exception e) {
            return dateTime != null ? dateTime : "";
        }
    }
}
