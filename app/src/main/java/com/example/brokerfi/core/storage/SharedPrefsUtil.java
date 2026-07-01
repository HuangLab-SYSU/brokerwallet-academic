package com.example.brokerfi.core.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SharedPrefsUtil {

    private static final String SP_NAME = "wallet_token";
    private static Context appContext;
    private static SharedPreferences encryptedSp;

    // initialization
    public static void init(Context context){
        if(appContext == null){
            appContext = context.getApplicationContext();
            initEncryptedSP();
        }
    }

    private static void initEncryptedSP() {
        try {
            // 1. Create master key (stored in Android Keystore)
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // 2. Create encrypted SharedPreferences
            encryptedSp = EncryptedSharedPreferences.create(
                    appContext,
                    SP_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("EncryptedSharedPreferences initialization failed", e);
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
