package com.example.brokerfi.token;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcConfig;
import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import com.example.brokerfi.xc.ChainAddressUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Persists imported tokens and per-token enabled state. */
public final class TokenStore {

    private static final String PREFS = "token_store";
    private static final String LEGACY_PREFS = "wbkc_token_store";
    private static final String KEY_CUSTOM = "custom_tokens";
    private static final String KEY_ENABLED_PREFIX = "enabled_";
    private static final Gson GSON = new Gson();

    private TokenStore() {
    }

    private static void migrateLegacyPrefs(Context context) {
        SharedPreferences legacy = context.getApplicationContext()
                .getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE);
        SharedPreferences current = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!legacy.getAll().isEmpty() && current.getAll().isEmpty()) {
            SharedPreferences.Editor editor = current.edit();
            for (java.util.Map.Entry<String, ?> entry : legacy.getAll().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof String) {
                    editor.putString(key, (String) value);
                } else if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof Float) {
                    editor.putFloat(key, (Float) value);
                }
            }
            editor.apply();
        }
    }

    public static List<TokenItem> loadAll(Context context, String wrappedBkcContractAddress) {
        List<TokenItem> result = new ArrayList<>();
        TokenItem wbkc = TokenItem.builtInwrappedBkc(wrappedBkcContractAddress);
        wbkc.setEnabled(isEnabled(context, wbkc.getContractAddress(), true));
        result.add(wbkc);

        SharedPreferences prefs = prefs(context);
        Map<String, TokenItem> custom = loadCustomMap(prefs);
        for (TokenItem item : custom.values()) {
            String addr = ChainAddressUtil.normalizeAddress(item.getContractAddress());
            if (TextUtils.isEmpty(addr) || addr.equals(wbkc.getContractAddress())) {
                continue;
            }
            item.setContractAddress(addr);
            item.setBuiltIn(false);
            item.setEnabled(isEnabled(context, addr, true));
            if (TextUtils.isEmpty(item.getCategory())) {
                item.setCategory("");
            }
            result.add(item);
        }
        return result;
    }

    public static void setEnabled(Context context, String contractAddress, boolean enabled) {
        if (context == null || TextUtils.isEmpty(contractAddress)) {
            return;
        }
        String key = KEY_ENABLED_PREFIX + ChainAddressUtil.normalizeAddress(contractAddress);
        prefs(context).edit().putBoolean(key, enabled).apply();
    }

    public static boolean isEnabled(Context context, String contractAddress, boolean defaultValue) {
        if (context == null || TextUtils.isEmpty(contractAddress)) {
            return defaultValue;
        }
        String key = KEY_ENABLED_PREFIX + ChainAddressUtil.normalizeAddress(contractAddress);
        return prefs(context).getBoolean(key, defaultValue);
    }

    public static boolean contains(Context context, String contractAddress) {
        String addr = ChainAddressUtil.normalizeAddress(contractAddress);
        if (TextUtils.isEmpty(addr)) {
            return false;
        }
        String wbkc = ChainAddressUtil.normalizeAddress(wrappedBkcContractHelper.resolveContractAddress(context));
        if (addr.equals(wbkc)) {
            return true;
        }
        return loadCustomMap(prefs(context)).containsKey(addr);
    }

    public static List<TokenItem> getEnabledTokens(Context context, String wrappedBkcContractAddress) {
        List<TokenItem> result = new ArrayList<>();
        for (TokenItem item : loadAll(context, wrappedBkcContractAddress)) {
            if (item.isEnabled()) {
                result.add(item);
            }
        }
        return result;
    }

    public static String resolveSymbol(Context context, String contractAddress, String wrappedBkcContractAddress) {
        String target = ChainAddressUtil.normalizeAddress(contractAddress);
        if (TextUtils.isEmpty(target)) {
            return wrappedBkcConfig.SYMBOL;
        }
        for (TokenItem item : loadAll(context, wrappedBkcContractAddress)) {
            if (target.equals(ChainAddressUtil.normalizeAddress(item.getContractAddress()))) {
                return !TextUtils.isEmpty(item.getSymbol()) ? item.getSymbol() : wrappedBkcConfig.SYMBOL;
            }
        }
        return wrappedBkcConfig.SYMBOL;
    }

    public static void addCustom(Context context, TokenItem item) {
        if (context == null || item == null || TextUtils.isEmpty(item.getContractAddress())) {
            return;
        }
        SharedPreferences prefs = prefs(context);
        Map<String, TokenItem> custom = loadCustomMap(prefs);
        String addr = ChainAddressUtil.normalizeAddress(item.getContractAddress());
        item.setContractAddress(addr);
        item.setBuiltIn(false);
        if (TextUtils.isEmpty(item.getCategory())) {
            item.setCategory("");
        }
        custom.put(addr, item);
        saveCustomMap(prefs, custom);
        setEnabled(context, addr, true);
    }

    private static SharedPreferences prefs(Context context) {
        migrateLegacyPrefs(context);
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static Map<String, TokenItem> loadCustomMap(SharedPreferences prefs) {
        String json = prefs.getString(KEY_CUSTOM, "[]");
        Type type = new TypeToken<List<TokenItem>>() {}.getType();
        List<TokenItem> list = GSON.fromJson(json, type);
        Map<String, TokenItem> map = new LinkedHashMap<>();
        if (list == null) {
            return map;
        }
        for (TokenItem item : list) {
            if (item == null || TextUtils.isEmpty(item.getContractAddress())) {
                continue;
            }
            map.put(ChainAddressUtil.normalizeAddress(item.getContractAddress()), item);
        }
        return map;
    }

    private static void saveCustomMap(SharedPreferences prefs, Map<String, TokenItem> map) {
        List<TokenItem> list = new ArrayList<>(map.values());
        prefs.edit().putString(KEY_CUSTOM, GSON.toJson(list)).apply();
    }
}
