package com.example.brokerfi.xc.dto;

import java.util.List;

public class PostDTO {
    public long id;
    public String username;
    public long userId;
    public String avatarUrl;
    public String title;
    public String content; // 首页只显示摘要
    public String firstImageUrl; // 取首张图(以后优化掉
    private List<String> imageUrls;
    public int likeCount;
    public int commentCount;
    public boolean isRewarded;
    public boolean isLiked;

    public double rewardTotal; // 打赏总额
    public String createTime;


    public void setId(long id) {
        this.id = id;
    }
}