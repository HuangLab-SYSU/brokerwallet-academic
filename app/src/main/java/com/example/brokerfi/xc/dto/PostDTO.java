package com.example.brokerfi.xc.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class PostDTO implements Serializable {
    private long id;
    private String userName;
    private long userId;
    private String avatarUrl;
    private String title;
    private String content;
    private String firstImageUrl; // 取首张图(以后优化掉
    private List<String> images;
    private int likeCount;
    private int commentCount;
    private boolean isRewarded;
    private boolean isLiked;
    private BigDecimal rewardAmount; // 打赏总额
    private String createTime;
    private String address;//发帖人的钱包地址


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFirstImageUrl() {
        return firstImageUrl;
    }

    public void setFirstImageUrl(String firstImageUrl) {
        this.firstImageUrl = firstImageUrl;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public boolean isRewarded() {
        return isRewarded;
    }

    public void setRewarded(boolean rewarded) {
        isRewarded = rewarded;
    }

    public boolean getIsLiked() {
        return isLiked;
    }

    public void setIsLiked(boolean liked) {
        isLiked = liked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public BigDecimal getRewardAmount() {
        return rewardAmount;
    }

    public void setRewardAmount(BigDecimal rewardAmount) {
        this.rewardAmount = rewardAmount;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public void setId(long id) {
        this.id = id;
    }
    public long getId() {return id;}
}