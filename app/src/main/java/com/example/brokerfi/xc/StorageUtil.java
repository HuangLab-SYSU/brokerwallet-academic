package com.example.brokerfi.xc;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

public class StorageUtil {
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String PREF_ACCOUNT = "accountkey";
    private static final String Curacc = "curacc";
    private static final String PREFS_NAME2 = "MyPrefsFile2";
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
        return settings.getString(NoticeId, null); // 如果没有找到，返回null
    }


    public static String getPrivateKey(AppCompatActivity activity) {
        SharedPreferences settings =activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getString(PREF_ACCOUNT, null); // 如果没有找到，返回null
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
}
