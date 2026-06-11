package com.example.brokerfi.token;

import com.example.brokerfi.xc.ChainAddressUtil;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 鏈満鐧昏鐨勬垚鍔熼摼涓婁唬甯佽浆鍑猴紙鍚敹娆惧湴鍧€涓?tx hash锛夛紝渚涙敹娆捐处鎴峰湪 gettx2 鏈储寮曟椂缁忓洖鎵у悓姝ャ€? */
public final class TokenOutboundNotifyStore {

    private static final String PREFS = "token_outbound_notify";
    private static final String KEY_LIST = "pending";
    private static final int MAX_ENTRIES = 64;
    private static final Gson GSON = new Gson();

    private TokenOutboundNotifyStore() {
    }

    public static void register(
            Context context,
            String fromWallet,
            String toWallet,
            String contractAddress,
            String txHash,
            String amountDisplay) {
        if (context == null
                || TextUtils.isEmpty(fromWallet)
                || TextUtils.isEmpty(toWallet)
                || TextUtils.isEmpty(contractAddress)
                || TextUtils.isEmpty(txHash)) {
            return;
        }
        String hash = normalizeHash(txHash);
        if (hash == null) {
            return;
        }
        List<Entry> list = load(context);
        for (Entry existing : list) {
            if (hash.equalsIgnoreCase(existing.txHash)) {
                return;
            }
        }
        Entry entry = new Entry();
        entry.txHash = hash;
        entry.fromWallet = ChainAddressUtil.normalizeAddress(fromWallet);
        entry.toWallet = ChainAddressUtil.normalizeAddress(toWallet);
        entry.contractAddress = ChainAddressUtil.normalizeAddress(contractAddress);
        entry.amountDisplay = amountDisplay != null ? amountDisplay.trim() : "";
        entry.createdMs = System.currentTimeMillis();
        list.add(0, entry);
        while (list.size() > MAX_ENTRIES) {
            list.remove(list.size() - 1);
        }
        save(context, list);
    }

    /** 鎵弿鏈満鍏ㄩ儴浠ｅ竵鍘嗗彶涓殑 SEND 璁板綍锛岀櫥璁?tx hash锛堣法璐︽埛鏀舵鍚屾锛夈€?*/
    public static void seedFromAllStoredHistories(Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(TokenTxHistoryStore.prefsName(), Context.MODE_PRIVATE);
        for (String key : prefs.getAll().keySet()) {
            if (key == null || !key.startsWith(TokenTxHistoryStore.listKeyPrefix())) {
                continue;
            }
            String wallet = key.substring(TokenTxHistoryStore.listKeyPrefix().length());
            if (!wallet.startsWith("0x") && !wallet.startsWith("0X")) {
                wallet = "0x" + wallet;
            }
            seedFromHistory(context, wallet);
        }
    }

    public static List<String> allHashes(Context context) {
        if (context == null) {
            return Collections.emptyList();
        }
        List<String> hashes = new ArrayList<>();
        for (Entry entry : load(context)) {
            if (entry != null && !TextUtils.isEmpty(entry.txHash)) {
                hashes.add(entry.txHash);
            }
        }
        return hashes;
    }

    /** 灏嗘湰鍦板凡淇濆瓨鐨?SEND 璁板綍锛堝惈 0x hash锛夊洖濉埌鏈満鐧昏锛屼究浜庢敹娆炬柟鍥炴墽鍚屾銆?*/
    public static void seedFromHistory(Context context, String walletAddress) {
        if (context == null || TextUtils.isEmpty(walletAddress)) {
            return;
        }
        for (TokenTxRecord record : TokenTxHistoryStore.getAll(context, walletAddress)) {
            if (record == null
                    || !TokenTxRecord.TYPE_SEND.equals(record.type)
                    || TextUtils.isEmpty(record.txHash)
                    || TextUtils.isEmpty(record.detail)
                    || TextUtils.isEmpty(record.contractAddress)) {
                continue;
            }
            String hash = record.txHash.trim().toLowerCase(Locale.US);
            if (!hash.startsWith("0x") || hash.length() < 10) {
                continue;
            }
            register(
                    context,
                    walletAddress,
                    record.detail,
                    record.contractAddress,
                    hash,
                    record.amountDisplay);
        }
    }

    /** 涓庡綋鍓嶉挶鍖呯浉鍏崇殑寰呭悓姝ヨ浆鍑?hash锛堟敹娆炬垨浠樻鏂癸級銆?*/
    public static List<String> hashesForWallet(Context context, String walletAddress) {
        if (context == null || TextUtils.isEmpty(walletAddress)) {
            return Collections.emptyList();
        }
        String wallet = ChainAddressUtil.normalizeAddress(walletAddress).toLowerCase(Locale.US);
        List<String> hashes = new ArrayList<>();
        for (Entry entry : load(context)) {
            if (entry == null || TextUtils.isEmpty(entry.txHash)) {
                continue;
            }
            String from = normalize(entry.fromWallet);
            String to = normalize(entry.toWallet);
            if (wallet.equals(from) || wallet.equals(to)) {
                hashes.add(entry.txHash);
            }
        }
        return hashes;
    }

    private static String normalize(String address) {
        if (TextUtils.isEmpty(address)) {
            return "";
        }
        return ChainAddressUtil.normalizeAddress(address).toLowerCase(Locale.US);
    }

    @androidx.annotation.Nullable
    private static String normalizeHash(String txHash) {
        if (TextUtils.isEmpty(txHash)) {
            return null;
        }
        String v = txHash.trim();
        if (!v.startsWith("0x") && !v.startsWith("0X")) {
            v = "0x" + v;
        }
        return v.length() >= 66 ? v.toLowerCase(Locale.US) : null;
    }

    private static List<Entry> load(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_LIST, null);
        if (TextUtils.isEmpty(json)) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<List<Entry>>() {}.getType();
            List<Entry> list = GSON.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void save(Context context, List<Entry> list) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LIST, GSON.toJson(list)).apply();
    }

    static final class Entry {
        String txHash;
        String fromWallet;
        String toWallet;
        String contractAddress;
        String amountDisplay;
        long createdMs;
    }
}


