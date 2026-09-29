package com.example.brokerfi.core.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

public class StorageUtil {
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String PREF_ACCOUNT = "accountkey";
    private static final String Curacc = "curacc";
    private static final String PREFS_NAME2 = "MyPrefsFile2";
    // syh_work_01 new：new SharedPreferences file for storaging nicknames
    private static final String NICK_PREFS = "account_nicknames";
    private static final String NoticeIdName = "NoticeIdName";
    private static final String NoticeId = "NoticeId";

    public static void savePrivateKey(AppCompatActivity activity, String acc){
        SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_ACCOUNT, acc);
        editor.apply();
    }
    public static void saveNoticeId(AppCompatActivity activity, String id){
        SharedPreferences settings = activity.getSharedPreferences(NoticeIdName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(NoticeId, id);
        editor.apply();
    }
    public static String getNoticeId(AppCompatActivity activity){
        SharedPreferences settings =activity.getSharedPreferences(NoticeIdName, Context.MODE_PRIVATE);
        return settings.getString(NoticeId, null);
    }


    public static String getPrivateKey(AppCompatActivity activity) {
        SharedPreferences settings =activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getString(PREF_ACCOUNT, null);
    }

    public static String getCurrentAccount(AppCompatActivity activity) {
        SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME2, Context.MODE_PRIVATE);
        return settings.getString(Curacc, null);
    }

    public static void saveCurrentAccount(AppCompatActivity activity, String s) {
        SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME2, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Curacc, s);
        editor.apply();
    }

    public static String getCurrentPrivatekey(AppCompatActivity activity){
        String account = StorageUtil.getPrivateKey(activity);
        String acc = StorageUtil.getCurrentAccount(activity);
        int i;
        if (acc == null){
            i=0;
        }else {
            i = Integer.parseInt(acc);
        }
        if (account != null) {
            String[] split = account.split(";");
            String privatekey = split[i];
            return privatekey;
        }
        return null;
    }

    // syh_work_01 new: storage the nickname of the account(key = lowercase of address)
    public static void saveAccountNickname(AppCompatActivity activity, String address, String nickname) {
        SharedPreferences settings = activity.getSharedPreferences(NICK_PREFS, Context.MODE_PRIVATE);
        settings.edit().putString(address.toLowerCase(), nickname).apply();
    }

    // syh_work_01 new：fetch the nickname of the account
    public static String getAccountNickname(AppCompatActivity activity, String address) {
        SharedPreferences settings = activity.getSharedPreferences(NICK_PREFS, Context.MODE_PRIVATE);
        return settings.getString(address.toLowerCase(), null);
    }

    // syh_work_01 new：remove the nicknames of the account
    public static void removeAccountNickname(AppCompatActivity activity, String address) {
        SharedPreferences settings = activity.getSharedPreferences(NICK_PREFS, Context.MODE_PRIVATE);
        settings.edit().remove(address.toLowerCase()).apply();
    }
}
