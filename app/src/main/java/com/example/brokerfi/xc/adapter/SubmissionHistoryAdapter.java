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
        
        // 文件名
        holder.fileNameText.setText(record.getFileName());
        
        // 文件大小和类型
        String fileInfo = record.getFormattedFileSize();
        if (record.getFileType() != null && !record.getFileType().isEmpty()) {
            fileInfo += " • " + getSimpleFileType(record.getFileType());
        }
        holder.fileInfoText.setText(fileInfo);
        
        // 上传时间
        holder.uploadTimeText.setText(formatUploadTime(record.getUploadTime()));
        
        // 审核状态
        holder.statusText.setText(record.getAuditStatusDesc());
        holder.statusText.setTextColor(context.getResources().getColor(record.getStatusColor()));
        
        // 勋章信息
        String medalInfo = record.getMedalIcon() + " " + record.getMedalAwardedDesc();
        holder.medalText.setText(medalInfo);
        
        // NFT状态
        if (record.isHasNftImage() && record.getNftImage() != null) {
            holder.nftStatusText.setText("🖼️ " + record.getNftImage().getMintStatusDesc());
            holder.nftStatusText.setVisibility(View.VISIBLE);
        } else {
            holder.nftStatusText.setVisibility(View.GONE);
        }
        
        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (context instanceof SubmissionHistoryActivity) {
                ((SubmissionHistoryActivity) context).openSubmissionDetail(record);
            }
        });
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
            return "文件";
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
            return "图片";
        } else if (mimeType.contains("text")) {
            return "文本";
        } else {
            return "文件";
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
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            fileNameText = itemView.findViewById(R.id.fileNameText);
            fileInfoText = itemView.findViewById(R.id.fileInfoText);
            uploadTimeText = itemView.findViewById(R.id.uploadTimeText);
            statusText = itemView.findViewById(R.id.statusText);
            medalText = itemView.findViewById(R.id.medalText);
            nftStatusText = itemView.findViewById(R.id.nftStatusText);
        }
    }
}

