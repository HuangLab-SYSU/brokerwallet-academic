package com.example.brokerfi.xc.dto;

public class PostDTO {
    public String username;
    public String avatarUrl;
    public String title;
    public String content; // 首页只显示摘要
    public String firstImageUrl; // 取首张图
    public int likeCount;
    public int commentCount;
    public boolean isRewarded;
}