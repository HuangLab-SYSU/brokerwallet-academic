package com.example.brokerfi.xc.dto;

public class UserAccountDTO {
    private Long userId;
    private String username;
    private String avatarUrl;
    private String walletAddress;
    private String token;

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public UserAccountDTO() {
    }

    // Getter
    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    // Setter
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }
}
