package com.example.brokerfi.token;

import com.example.brokerfi.core.storage.StorageUtil;

import com.example.brokerfi.proof.SubmissionUtil;

import com.example.brokerfi.core.security.SecurityUtil;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public final class TokenWalletHelper {

    private static final String PRIVATE_KEY_PREFS = "MyPrefsFile";
    private static final String PRIVATE_KEY_KEY = "accountkey";
    private static final String CURRENT_ACCOUNT_PREFS = "MyPrefsFile2";
    private static final String CURRENT_ACCOUNT_KEY = "curacc";

    private TokenWalletHelper() {
    }

    public static String getWalletAddress(Context context) {
        String privateKey = getCurrentPrivateKey(context);
        if (!TextUtils.isEmpty(privateKey)) {
            return SecurityUtil.GetAddress(privateKey);
        }
        return SubmissionUtil.getCurrentWalletAddress(context);
    }

    public static String getCurrentPrivateKey(Context context) {
        if (context == null) {
            return null;
        }
        SharedPreferences keyPrefs =
                context.getSharedPreferences(PRIVATE_KEY_PREFS, Context.MODE_PRIVATE);
        SharedPreferences accountPrefs =
                context.getSharedPreferences(CURRENT_ACCOUNT_PREFS, Context.MODE_PRIVATE);
        String accountList = keyPrefs.getString(PRIVATE_KEY_KEY, null);
        String currentAccount = accountPrefs.getString(CURRENT_ACCOUNT_KEY, null);
        int index = 0;
        if (!TextUtils.isEmpty(currentAccount)) {
            try {
                index = Integer.parseInt(currentAccount);
            } catch (NumberFormatException ignored) {
                index = 0;
            }
        }
        if (TextUtils.isEmpty(accountList)) {
            return null;
        }
        String[] split = accountList.split(";");
        if (index < 0 || index >= split.length) {
            return null;
        }
        return split[index];
    }
}
