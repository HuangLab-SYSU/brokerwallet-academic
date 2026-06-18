package com.example.brokerfi.token;

import com.example.brokerfi.core.blockchain.ChainAddressUtil;
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
 * Local registry of successful outbound token transfers, including recipient
 * address and tx hash, so receipt-based sync can backfill history before
 * {@code gettx2} catches up.
 */
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

    /** Seeds the registry from every locally stored SEND record that has a 0x tx hash. */
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

    /** Seeds the registry from all SEND records stored for the given wallet. */
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

    /** Returns all registered hashes related to the current wallet, inbound or outbound. */
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
