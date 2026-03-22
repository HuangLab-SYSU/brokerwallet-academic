package com.example.brokerfi.xc;

import android.content.Context;
import android.content.SharedPreferences;

public class UserStorageUtil {

    private static final String PREF_NAME = "user_prefs";

    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_AVATAR = "avatar";
    private static final String KEY_WALLET = "wallet";

    public static void saveUser(Context context, Long userId, String username, String avatar, String wallet) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_AVATAR, avatar)
                .putString(KEY_WALLET, wallet)
                .apply();
    }

    public static Long getUserId(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_USER_ID, -1);
    }

    public static String getUsername(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USERNAME, "");
    }

    public static String getAvatar(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_AVATAR, "");
    }

    public static String getWallet(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_WALLET, "");
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply();
    }
}
