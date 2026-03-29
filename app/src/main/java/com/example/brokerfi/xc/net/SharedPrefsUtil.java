package com.example.brokerfi.xc.net;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SharedPrefsUtil {

    private static final String SP_NAME = "wallet_token";
    private static Context appContext;
    private static SharedPreferences encryptedSp;

    // 初始化（建议在 Application 或 MainActivity 调用一次）
    public static void init(Context context){
        if(appContext == null){
            appContext = context.getApplicationContext();
            initEncryptedSP();
        }
    }

    private static void initEncryptedSP() {
        try {
            // 1. 创建主密钥（存储在 Android Keystore）
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // 2. 创建加密的 SharedPreferences
            encryptedSp = EncryptedSharedPreferences.create(
                    appContext,
                    SP_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("EncryptedSharedPreferences 初始化失败", e);
        }
    }

    public static void putString(String key, String value){
        encryptedSp.edit().putString(key, value).apply();
    }

    public static String getString(String key, String defVal){
        return encryptedSp.getString(key, defVal);
    }

    public static void remove(String key){
        encryptedSp.edit().remove(key).apply();
    }
}