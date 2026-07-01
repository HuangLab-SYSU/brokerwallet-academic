package com.example.brokerfi.proof.model;

/**
 * Submit record data model / 提交记录数据模型
 */
public class SubmissionRecord {
    private String submissionId;
    private Long id;
    private String batchId;
    private int fileCount = 1;  // Number of files in the batch, default is 1.
    private String fileName;
    private long fileSize;
    private String fileType;
    private String uploadTime;
    private String auditStatus;
    private String auditStatusDesc;
    private String auditTime;
    private String medalAwarded;
    private String medalAwardedDesc;
    private String medalAwardTime;
    private String medalTransactionHash;
    private boolean hasNftImage;
    private NftImageInfo nftImage;
    private String tokenReward;
    private String tokenRewardTxHash;

    // Constructor
    public SubmissionRecord() {}

    // Getter and Setter methods
    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus) {
        this.auditStatus = auditStatus;
    }

    public String getAuditStatusDesc() {
        return auditStatusDesc;
    }

    public void setAuditStatusDesc(String auditStatusDesc) {
        this.auditStatusDesc = auditStatusDesc;
    }

    public String getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(String auditTime) {
        this.auditTime = auditTime;
    }

    public String getMedalAwarded() {
        return medalAwarded;
    }

    public void setMedalAwarded(String medalAwarded) {
        this.medalAwarded = medalAwarded;
    }

    public String getMedalAwardedDesc() {
        return medalAwardedDesc;
    }

    public void setMedalAwardedDesc(String medalAwardedDesc) {
        this.medalAwardedDesc = medalAwardedDesc;
    }

    public String getMedalAwardTime() {
        return medalAwardTime;
    }

    public void setMedalAwardTime(String medalAwardTime) {
        this.medalAwardTime = medalAwardTime;
    }

    public String getMedalTransactionHash() {
        return medalTransactionHash;
    }

    public void setMedalTransactionHash(String medalTransactionHash) {
        this.medalTransactionHash = medalTransactionHash;
    }

    public boolean isHasNftImage() {
        return hasNftImage;
    }

    public void setHasNftImage(boolean hasNftImage) {
        this.hasNftImage = hasNftImage;
    }

    public NftImageInfo getNftImage() {
        return nftImage;
    }

    public void setNftImage(NftImageInfo nftImage) {
        this.nftImage = nftImage;
    }

    public String getTokenReward() {
        return tokenReward;
    }

    public void setTokenReward(String tokenReward) {
        this.tokenReward = tokenReward;
    }

    public String getTokenRewardTxHash() {
        return tokenRewardTxHash;
    }

    public void setTokenRewardTxHash(String tokenRewardTxHash) {
        this.tokenRewardTxHash = tokenRewardTxHash;
    }

    /**
     * Get formatted file size / 获取格式化的文件大小
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    /**
     * Get status color resource ID / 获取状态颜色资源ID
     */
    public int getStatusColor() {
        if (auditStatus == null) {
            return android.R.color.darker_gray;
        }

        switch (auditStatus.toUpperCase()) {
            case "PENDING":
                return android.R.color.holo_orange_dark;
            case "APPROVED":
                return android.R.color.holo_green_dark;
            case "REJECTED":
                return android.R.color.holo_red_dark;
            default:
                return android.R.color.darker_gray;
        }
    }

    /**
     * Get medal icon / 获取勋章图标
     */
    public String getMedalIcon() {
        if (medalAwarded == null || "NONE".equals(medalAwarded)) {
            return "⚪";
        }

        switch (medalAwarded.toUpperCase()) {
            case "GOLD":
                return "🥇";
            case "SILVER":
                return "🥈";
            case "BRONZE":
                return "🥉";
            default:
                return "⚪";
        }
    }

    /**
     * NFT image information internal class / NFT图片信息内部类
     */
    public static class NftImageInfo {
        private Long id;
        private String originalName;
        private String mintStatus;
        private String mintStatusDesc;
        private String tokenId;
        private String transactionHash;

        // Getter and Setter methods
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getOriginalName() {
            return originalName;
        }

        public void setOriginalName(String originalName) {
            this.originalName = originalName;
        }

        public String getMintStatus() {
            return mintStatus;
        }

        public void setMintStatus(String mintStatus) {
            this.mintStatus = mintStatus;
        }

        public String getMintStatusDesc() {
            return mintStatusDesc;
        }

        public void setMintStatusDesc(String mintStatusDesc) {
            this.mintStatusDesc = mintStatusDesc;
        }

        public String getTokenId() {
            return tokenId;
        }

        public void setTokenId(String tokenId) {
            this.tokenId = tokenId;
        }

        public String getTransactionHash() {
            return transactionHash;
        }

        public void setTransactionHash(String transactionHash) {
            this.transactionHash = transactionHash;
        }
    }
}
