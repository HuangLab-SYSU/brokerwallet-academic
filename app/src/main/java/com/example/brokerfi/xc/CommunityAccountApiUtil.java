package com.example.brokerfi.xc;

import com.example.brokerfi.xc.dto.UserAccountDTO;

public class CommunityAccountApiUtil {

    private static CommunityAccountApiUtil instance;

    public static CommunityAccountApiUtil getInstance() {
        if (instance == null) {
            instance = new CommunityAccountApiUtil();
        }
        return instance;
    }

    public void login(String walletAddress, OnSuccess success, OnError error) {

        // TODO:接真实接口

        new Thread(() -> {
            try {
                // mock
                UserAccountDTO user = new UserAccountDTO();
                user.setUserId(1L);
                user.setUsername("用户1234");
                user.setAvatarUrl("xxx");
                user.setWalletAddress(walletAddress);

                success.onSuccess(user);

            } catch (Exception e) {
                error.onError(e.getMessage());
            }
        }).start();
    }

    public interface OnSuccess {
        void onSuccess(UserAccountDTO user);
    }

    public interface OnError {
        void onError(String msg);
    }
}