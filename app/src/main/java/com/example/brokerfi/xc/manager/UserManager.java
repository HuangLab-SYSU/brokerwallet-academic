package com.example.brokerfi.xc.manager;

import android.content.Context;

import com.example.brokerfi.core.storage.UserStorageUtil;

// Community user cache
public class UserManager {

    private static UserManager instance;

    private Long userId;
    private String username;
    private String avatar;
    private String wallet;

    private UserManager() {}

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public void init(Context context) {
        this.userId = UserStorageUtil.getUserId(context);
        this.username = UserStorageUtil.getUsername(context);
        this.avatar = UserStorageUtil.getAvatar(context);
        this.wallet = UserStorageUtil.getWallet(context);
    }

    public void setUser(Context context, Long userId, String username, String avatar, String wallet) {
        this.userId = userId;
        this.username = username;
        this.avatar = avatar;
        this.wallet = wallet;

        UserStorageUtil.saveUser(context, userId, username, avatar, wallet);
    }

    public Long getUserId() {
        return userId;
    }
}
