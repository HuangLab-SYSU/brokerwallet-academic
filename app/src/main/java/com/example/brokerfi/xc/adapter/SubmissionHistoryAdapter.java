package com.example.brokerfi.xc.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.SubmissionHistoryActivity;
import com.example.brokerfi.xc.model.SubmissionRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 提交历史适配器
 */
public class SubmissionHistoryAdapter extends RecyclerView.Adapter<SubmissionHistoryAdapter.ViewHolder> {
    
    private Context context;
    private List<SubmissionRecord> submissionList;
    private SimpleDateFormat dateFormat;
    
    public SubmissionHistoryAdapter(Context context, List<SubmissionRecord> submissionList) {
        this.context = context;
        this.submissionList = submissionList;
        this.dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_submission_history, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubmissionRecord record = submissionList.get(position);
        
        // File name (if batch submission, show batch info)
        String fileName = record.getFileName();
        if (record.getFileCount() > 1) {
            fileName = fileName + " and " + record.getFileCount() + " more file(s)";
        }
        holder.fileNameText.setText(fileName);
        
        // File size and type
        String fileInfo = record.getFormattedFileSize();
        if (record.getFileType() != null && !record.getFileType().isEmpty()) {
            fileInfo += " • " + getSimpleFileType(record.getFileType());
        }
        if (record.getFileCount() > 1) {
            fileInfo = record.getFileCount() + " file(s) • " + fileInfo;
        }
        holder.fileInfoText.setText(fileInfo);
        
        // 上传时间
        holder.uploadTimeText.setText(formatUploadTime(record.getUploadTime()));
        
        // 审核状态
        holder.statusText.setText(record.getAuditStatusDesc());
        holder.statusText.setTextColor(context.getResources().getColor(record.getStatusColor()));
        
        // 勋章信息（显示数量）
        String medalInfo = buildMedalInfo(record);
        holder.medalText.setText(medalInfo);
        
        // NFT状态（隐藏，因为进度条已经能清楚表示状态）
        holder.nftStatusText.setVisibility(View.GONE);
        
        // BKC token reward display
        if (record.getTokenReward() != null && !record.getTokenReward().isEmpty() && 
            !record.getTokenReward().equals("0") && !record.getTokenReward().equals("0.0")) {
            holder.tokenRewardText.setText("💰 BKC Reward: " + record.getTokenReward() + " BKC");
            holder.tokenRewardText.setVisibility(View.VISIBLE);
        } else {
            holder.tokenRewardText.setVisibility(View.GONE);
        }
        
        // 进度显示（根据审核状态动态更新）
        updateProgress(holder, record);
        
        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (context instanceof SubmissionHistoryActivity) {
                ((SubmissionHistoryActivity) context).openSubmissionDetail(record);
            }
        });
    }
    
    /**
     * 构建勋章信息显示
     */
    private String buildMedalInfo(SubmissionRecord record) {
        String medalAwarded = record.getMedalAwarded();
        
        // Check if medal has been awarded
        if (medalAwarded == null || "NONE".equals(medalAwarded)) {
            return "⚪ No Medal Awarded";
        } else {
            return "🏅 Medal Awarded";
        }
    }
    
    /**
     * 更新进度显示
     */
    private void updateProgress(ViewHolder holder, SubmissionRecord record) {
        int progress = 1; // Default: uploaded
        String progressStr = "1/3 Uploaded";
        
        // Determine progress based on audit status
        if ("APPROVED".equals(record.getAuditStatus())) {
            // Audit approved
            progress = 2;
            progressStr = "2/3 Approved";
            
            // If has medal or NFT, then complete
            String medalAwarded = record.getMedalAwarded();
            boolean hasMedal = medalAwarded != null && !"NONE".equals(medalAwarded);
            
            if (hasMedal || record.isHasNftImage()) {
                progress = 3;
                if (record.isHasNftImage()) {
                    progressStr = "3/3 NFT Minted";
                } else {
                    progressStr = "3/3 Medal Awarded";
                }
            }
        } else if ("REJECTED".equals(record.getAuditStatus())) {
            progress = 2;
            progressStr = "Audit Rejected";
        }
        
        // 更新进度条颜色
        int activeColor = context.getResources().getColor(R.color.colorPrimary);
        int inactiveColor = context.getResources().getColor(R.color.grey);
        
        holder.progressBar1.setBackgroundColor(progress >= 1 ? activeColor : inactiveColor);
        holder.progressBar2.setBackgroundColor(progress >= 2 ? activeColor : inactiveColor);
        holder.progressBar3.setBackgroundColor(progress >= 3 ? activeColor : inactiveColor);
        
        holder.progressText.setText(progressStr);
    }
    
    @Override
    public int getItemCount() {
        return submissionList.size();
    }
    
    /**
     * 格式化上传时间
     */
    private String formatUploadTime(String uploadTime) {
        try {
            // 假设后端返回的是ISO格式的时间字符串
            // 这里简化处理，只取前面的日期和时间部分
            if (uploadTime != null && uploadTime.length() >= 16) {
                String dateTimeStr = uploadTime.substring(0, 16).replace('T', ' ');
                return dateTimeStr;
            }
            return uploadTime;
        } catch (Exception e) {
            return uploadTime != null ? uploadTime : "";
        }
    }
    
    /**
     * 获取简化的文件类型
     */
    private String getSimpleFileType(String mimeType) {
        if (mimeType == null) {
            return "File";
        }
        
        if (mimeType.contains("pdf")) {
            return "PDF";
        } else if (mimeType.contains("word") || mimeType.contains("msword")) {
            return "Word";
        } else if (mimeType.contains("excel") || mimeType.contains("spreadsheet")) {
            return "Excel";
        } else if (mimeType.contains("powerpoint") || mimeType.contains("presentation")) {
            return "PPT";
        } else if (mimeType.contains("image")) {
            return "Image";
        } else if (mimeType.contains("text")) {
            return "Text";
        } else {
            return "File";
        }
    }
    
    /**
     * ViewHolder类
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameText;
        TextView fileInfoText;
        TextView uploadTimeText;
        TextView statusText;
        TextView medalText;
        TextView nftStatusText;
        TextView tokenRewardText;
        View progressBar1, progressBar2, progressBar3;
        TextView progressText;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            fileNameText = itemView.findViewById(R.id.fileNameText);
            fileInfoText = itemView.findViewById(R.id.fileInfoText);
            uploadTimeText = itemView.findViewById(R.id.uploadTimeText);
            statusText = itemView.findViewById(R.id.statusText);
            medalText = itemView.findViewById(R.id.medalText);
            nftStatusText = itemView.findViewById(R.id.nftStatusText);
            tokenRewardText = itemView.findViewById(R.id.tokenRewardText);
            progressBar1 = itemView.findViewById(R.id.progressBar1);
            progressBar2 = itemView.findViewById(R.id.progressBar2);
            progressBar3 = itemView.findViewById(R.id.progressBar3);
            progressText = itemView.findViewById(R.id.progressText);
        }
    }
}

