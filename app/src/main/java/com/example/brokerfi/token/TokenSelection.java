package com.example.brokerfi.token;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import com.example.brokerfi.xc.ChainAddressUtil;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.List;

/** Remembers the currently selected token per wallet address. */
public final class TokenSelection {

    private static final String PREFS = "token_selection";
    private static final String KEY_PREFIX = "selected_";

    public static final String EXTRA_CONTRACT = "token_selected_contract";
    public static final String EXTRA_NAME = "token_selected_name";
    public static final String EXTRA_SYMBOL = "token_selected_symbol";
    public static final String EXTRA_BUILT_IN = "token_selected_built_in";

    private TokenSelection() {
    }

    public static TokenItem getSelected(Context context) {
        return ensureValidSelection(context);
    }

    public static String getSelectedContract(Context context) {
        TokenItem item = getSelected(context);
        return item != null ? item.getContractAddress() : "";
    }

    public static void setSelected(Context context, TokenItem item) {
        if (context == null || item == null || TextUtils.isEmpty(item.getContractAddress())) {
            return;
        }
        String wallet = ChainAddressUtil.normalizeAddress(TokenWalletHelper.getWalletAddress(context));
        if (TextUtils.isEmpty(wallet)) {
            return;
        }
        prefs(context).edit()
                .putString(KEY_PREFIX + wallet, ChainAddressUtil.normalizeAddress(item.getContractAddress()))
                .apply();
    }

    public static TokenItem ensureValidSelection(Context context) {
        String wrappedBkcContract = wrappedBkcContractHelper.resolveContractAddress(context);
        List<TokenItem> enabled = TokenStore.getEnabledTokens(context, wrappedBkcContract);
        if (enabled.isEmpty()) {
            return TokenItem.builtInwrappedBkc(wrappedBkcContract);
        }

        String wallet = ChainAddressUtil.normalizeAddress(TokenWalletHelper.getWalletAddress(context));
        String saved = TextUtils.isEmpty(wallet)
                ? ""
                : prefs(context).getString(KEY_PREFIX + wallet, "");

        if (!TextUtils.isEmpty(saved)) {
            for (TokenItem item : enabled) {
                if (saved.equals(item.getContractAddress())) {
                    return item;
                }
            }
        }

        TokenItem fallback = null;
        for (TokenItem item : enabled) {
            if (item.isBuiltIn()) {
                fallback = item;
                break;
            }
        }
        if (fallback == null) {
            fallback = enabled.get(0);
        }
        setSelected(context, fallback);
        return fallback;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
