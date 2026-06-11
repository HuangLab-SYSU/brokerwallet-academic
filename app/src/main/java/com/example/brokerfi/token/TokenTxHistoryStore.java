package com.example.brokerfi.token;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import com.example.brokerfi.xc.ChainAddressUtil;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** 鎸夐挶鍖呭湴鍧€鎸佷箙鍖栦唬甯佷氦鏄撳巻鍙诧紙鍙寜鍚堢害绛涢€夛級銆?*/
public final class TokenTxHistoryStore {

    private static final String PREFS = "token_tx_history";
    private static final String LEGACY_PREFS = "wbkc_tx_history";
    private static final String KEY_PREFIX = "list_";
    private static final int MAX_RECORDS = 100;
    private static final long DEDUPE_WINDOW_MS = 120_000L;
    private static final Gson GSON = new Gson();

    private TokenTxHistoryStore() {
    }

    static String prefsName() {
        return PREFS;
    }

    static String listKeyPrefix() {
        return KEY_PREFIX;
    }

    public static void add(Context context, String walletAddress, TokenTxRecord record) {
        addIfAbsent(context, walletAddress, record);
    }

    /** 鎸変氦鏄撳搱甯?/ 杩戦噸澶嶄簨浠跺幓閲嶅悗鍐欏叆銆?*/
    public static boolean addIfAbsent(Context context, String walletAddress, TokenTxRecord record) {
        if (context == null || record == null || TextUtils.isEmpty(walletAddress)) {
            return false;
        }
        String key = storageKey(walletAddress);
        List<TokenTxRecord> list = loadList(context, key);
        if (!TextUtils.isEmpty(record.txHash) && containsTxHash(list, record.txHash)) {
            return false;
        }
        if (findNearDuplicate(list, record) != null) {
            return false;
        }
        list.add(record);
        sortNewestFirst(list);
        while (list.size() > MAX_RECORDS) {
            list.remove(list.size() - 1);
        }
        saveList(context, key, list);
        return true;
    }

    /** 鍐欏叆鍓嶆槸鍚︿笌宸叉湁璁板綍杩戦噸澶嶏紙鍚屽悎绾︺€佸悓閲戦銆? 鍒嗛挓鍐呫€佺被鍨嬪啿绐侊級銆?*/
    public static boolean isDuplicateOfStored(
            Context context, String walletAddress, TokenTxRecord record) {
        if (context == null || record == null || TextUtils.isEmpty(walletAddress)) {
            return false;
        }
        return findNearDuplicate(loadList(context, storageKey(walletAddress)), record) != null;
    }

    /** 鎸佷箙鍖栧幓閲嶏細娓呯悊鏈湴宸插瓨鍌ㄧ殑閲嶅浜ゆ槗锛堜繚鐣?wrap/unwrap 绛変紭鍏堢骇楂樼殑璁板綍锛夈€?*/
    public static int compactWallet(Context context, String walletAddress) {
        if (context == null || TextUtils.isEmpty(walletAddress)) {
            return 0;
        }
        String key = storageKey(walletAddress);
        List<TokenTxRecord> list = loadList(context, key);
        if (list.isEmpty()) {
            return 0;
        }
        List<TokenTxRecord> compacted = dedupeForDisplay(list);
        sortNewestFirst(compacted);
        int removed = list.size() - compacted.size();
        if (removed > 0) {
            saveList(context, key, compacted);
        }
        return removed;
    }

    /**
     * 绉婚櫎 gettx2 閽卞寘鈫旈挶鍖呯储寮曡鍐欏叆鐨勮褰曪紙鍚凡鏍囨敞 wBKC 鍚堢害鐨勬棫鏁版嵁锛夈€?     * 涓嶅奖鍝嶆湰鍦?{@code 0x} hash 璁板綍鍙婂悎绾︾储寮曠殑 wrap/unwrap/鏀舵銆?     */
    public static int removeGetTx2WalletPeerRecords(
            Context context,
            String walletAddress,
            java.util.Set<String> walletPeerGetTx2Ids) {
        if (context == null
                || TextUtils.isEmpty(walletAddress)
                || walletPeerGetTx2Ids == null
                || walletPeerGetTx2Ids.isEmpty()) {
            return 0;
        }
        String key = storageKey(walletAddress);
        List<TokenTxRecord> list = loadList(context, key);
        if (list.isEmpty()) {
            return 0;
        }
        List<TokenTxRecord> kept = new ArrayList<>();
        int removed = 0;
        for (TokenTxRecord record : list) {
            if (shouldRemoveWalletPeerGetTx2Record(record, walletPeerGetTx2Ids)) {
                removed++;
            } else {
                kept.add(record);
            }
        }
        if (removed > 0) {
            saveList(context, key, kept);
        }
        return removed;
    }

    /**
     * 璇诲彇鎴栧睍绀哄墠绂荤嚎娓呯悊璇啓鍏ョ殑 gettx2 閽卞寘鈫旈挶鍖呰褰曪紙涓嶄緷璧栫綉缁滐級銆?     */
    public static int sanitizeWalletHistory(Context context, String walletAddress) {
        return removeMisclassifiedPeerGetTx2Records(context, walletAddress);
    }

    /**
     * 绂荤嚎娓呯悊锛歡ettx2 浠呮暟瀛?id 鐨?SEND/RECEIVE 涓嶅彲闈狅紙鍚悎绾︾储寮曡鏍?wBKC锛夛紝鍙繚鐣?0x 閾句笂 hash銆?     */
    public static int removeMisclassifiedPeerGetTx2Records(Context context, String walletAddress) {
        if (context == null || TextUtils.isEmpty(walletAddress)) {
            return 0;
        }
        String key = storageKey(walletAddress);
        List<TokenTxRecord> list = loadList(context, key);
        if (list.isEmpty()) {
            return 0;
        }
        List<TokenTxRecord> kept = new ArrayList<>();
        int removed = 0;
        for (TokenTxRecord record : list) {
            if (shouldRemoveMisclassifiedPeerRecord(record)) {
                removed++;
            } else {
                kept.add(record);
            }
        }
        if (removed > 0) {
            saveList(context, key, kept);
        }
        return removed;
    }

    private static boolean shouldRemoveWalletPeerGetTx2Record(
            TokenTxRecord record,
            java.util.Set<String> walletPeerGetTx2Ids) {
        if (record == null || TokenTxRecord.isSwapType(record.type)) {
            return false;
        }
        if (!TokenTxRecord.TYPE_SEND.equals(record.type)
                && !TokenTxRecord.TYPE_RECEIVE.equals(record.type)) {
            return false;
        }
        if (hasHexTxHash(record.txHash)) {
            return false;
        }
        String txId = record.txHash != null ? record.txHash.trim() : "";
        return !TextUtils.isEmpty(txId) && walletPeerGetTx2Ids.contains(txId);
    }

    /** 灞曠ず灞傝繃婊わ細gettx2 鏁板瓧 id 鐨?SEND/RECEIVE锛堥潪閾句笂 0x hash锛夈€?*/
    public static boolean isMisclassifiedPeerGetTx2Record(TokenTxRecord record) {
        return shouldRemoveMisclassifiedPeerRecord(record);
    }

    private static boolean shouldRemoveMisclassifiedPeerRecord(TokenTxRecord record) {
        if (record == null || TokenTxRecord.isSwapType(record.type)) {
            return false;
        }
        if (!TokenTxRecord.TYPE_SEND.equals(record.type)
                && !TokenTxRecord.TYPE_RECEIVE.equals(record.type)) {
            return false;
        }
        return !hasHexTxHash(record.txHash) && isNumericGetTx2Id(record.txHash);
    }

    private static boolean isNumericGetTx2Id(String txHash) {
        if (TextUtils.isEmpty(txHash)) {
            return false;
        }
        return txHash.trim().matches("\\d+");
    }

    private static boolean containsTxHash(List<TokenTxRecord> list, String txHash) {
        String target = txHash.trim().toLowerCase(Locale.US);
        for (TokenTxRecord existing : list) {
            if (existing != null
                    && !TextUtils.isEmpty(existing.txHash)
                    && existing.txHash.trim().toLowerCase(Locale.US).equals(target)) {
                return true;
            }
        }
        return false;
    }

    public static List<TokenTxRecord> getAll(Context context, String walletAddress) {
        if (context == null || TextUtils.isEmpty(walletAddress)) {
            return Collections.emptyList();
        }
        removeMisclassifiedPeerGetTx2Records(context, walletAddress);
        List<TokenTxRecord> list = loadList(context, storageKey(walletAddress));
        sortNewestFirst(list);
        return list;
    }

    public static List<TokenTxRecord> getForContract(
            Context context, String walletAddress, String contractAddress) {
        if (context == null || TextUtils.isEmpty(walletAddress) || TextUtils.isEmpty(contractAddress)) {
            return Collections.emptyList();
        }
        String target = ChainAddressUtil.normalizeAddress(contractAddress);
        String defaultContract = ChainAddressUtil.normalizeAddress(
                wrappedBkcContractHelper.resolveContractAddress(context));
        List<TokenTxRecord> result = new ArrayList<>();
        for (TokenTxRecord record : getAll(context, walletAddress)) {
            if (isMisclassifiedPeerGetTx2Record(record)) {
                continue;
            }
            String recordContract = ChainAddressUtil.normalizeAddress(record.contractAddress);
            if (TextUtils.isEmpty(recordContract)) {
                if (TokenTxRecord.isSwapType(record.type)) {
                    recordContract = defaultContract;
                } else if (hasHexTxHash(record.txHash)) {
                    recordContract = defaultContract;
                } else {
                    continue;
                }
            }
            if (target.equals(recordContract)) {
                result.add(record);
            }
        }
        List<TokenTxRecord> display = dedupeForDisplay(result);
        sortNewestFirst(display);
        return display;
    }

    /** 鎸夋椂闂撮檷搴忥細鏈€鏂板湪涓婏紱鏃堕棿鐩稿悓鍒欐寜 txHash 绋冲畾鎺掑簭銆?*/
    private static void sortNewestFirst(List<TokenTxRecord> records) {
        if (records == null || records.size() < 2) {
            return;
        }
        Collections.sort(records, TX_NEWEST_FIRST);
    }

    private static final Comparator<TokenTxRecord> TX_NEWEST_FIRST = (a, b) -> {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int byTime = Long.compare(b.timestampMs, a.timestampMs);
        if (byTime != 0) {
            return byTime;
        }
        String ha = a.txHash != null ? a.txHash.trim() : "";
        String hb = b.txHash != null ? b.txHash.trim() : "";
        return hb.compareToIgnoreCase(ha);
    };

    /**
     * 灞曠ず鍓嶅幓閲嶏細unwrap/wrap 鏈湴璁板綍浼樺厛浜?gettx2 璇爣鐨?鎺ユ敹/鍙戦€併€?     */
    private static List<TokenTxRecord> dedupeForDisplay(List<TokenTxRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<TokenTxRecord> kept = new ArrayList<>();
        for (TokenTxRecord candidate : records) {
            TokenTxRecord duplicateOf = findNearDuplicate(kept, candidate);
            if (duplicateOf == null) {
                kept.add(candidate);
                continue;
            }
            if (recordPriority(candidate) > recordPriority(duplicateOf)) {
                kept.remove(duplicateOf);
                kept.add(candidate);
            }
        }
        return kept;
    }

    @androidx.annotation.Nullable
    private static TokenTxRecord findNearDuplicate(List<TokenTxRecord> list, TokenTxRecord incoming) {
        if (list == null || incoming == null) {
            return null;
        }
        for (TokenTxRecord existing : list) {
            if (isNearDuplicate(existing, incoming)) {
                return existing;
            }
        }
        return null;
    }

    private static boolean isNearDuplicate(TokenTxRecord a, TokenTxRecord b) {
        if (a == null || b == null) {
            return false;
        }
        if (!sameContract(a, b)) {
            return false;
        }
        if (!TextUtils.equals(normalizeAmount(a.amountDisplay), normalizeAmount(b.amountDisplay))) {
            return false;
        }
        if (Math.abs(a.timestampMs - b.timestampMs) > DEDUPE_WINDOW_MS) {
            return false;
        }
        return isConflictingTypePair(a.type, b.type);
    }

    private static boolean sameContract(TokenTxRecord a, TokenTxRecord b) {
        String ca = ChainAddressUtil.normalizeAddress(a.contractAddress);
        String cb = ChainAddressUtil.normalizeAddress(b.contractAddress);
        return TextUtils.equals(ca, cb);
    }

    private static String normalizeAmount(String amount) {
        if (amount == null) {
            return "";
        }
        return amount.trim();
    }

    /**
     * gettx2 甯告妸 wrap/unwrap 閾句笂浜嬩欢鏍囨垚 鎺ユ敹/鍙戦€侊紝涓庢湰鍦?瀛樺叆/鍙栧洖 閲嶅銆?     */
    private static boolean isConflictingTypePair(String typeA, String typeB) {
        if (TextUtils.equals(typeA, typeB)) {
            return true;
        }
        return pairs(typeA, typeB, TokenTxRecord.TYPE_RECEIVE, TokenTxRecord.TYPE_UNWRAP)
                || pairs(typeA, typeB, TokenTxRecord.TYPE_RECEIVE, TokenTxRecord.TYPE_WRAP)
                || pairs(typeA, typeB, TokenTxRecord.TYPE_RECEIVE, TokenTxRecord.TYPE_SWAP)
                || pairs(typeA, typeB, TokenTxRecord.TYPE_SEND, TokenTxRecord.TYPE_WRAP)
                || pairs(typeA, typeB, TokenTxRecord.TYPE_SEND, TokenTxRecord.TYPE_UNWRAP)
                || pairs(typeA, typeB, TokenTxRecord.TYPE_SEND, TokenTxRecord.TYPE_SWAP);
    }

    private static boolean pairs(String a, String b, String x, String y) {
        return (TextUtils.equals(a, x) && TextUtils.equals(b, y))
                || (TextUtils.equals(a, y) && TextUtils.equals(b, x));
    }

    private static int recordPriority(TokenTxRecord record) {
        if (record == null || record.type == null) {
            return 0;
        }
        switch (record.type) {
            case TokenTxRecord.TYPE_SWAP:
            case TokenTxRecord.TYPE_WRAP:
            case TokenTxRecord.TYPE_UNWRAP:
                return 3;
            case TokenTxRecord.TYPE_SEND:
                return hasHexTxHash(record.txHash) ? 2 : 1;
            case TokenTxRecord.TYPE_RECEIVE:
                return 0;
            default:
                return 0;
        }
    }

    private static boolean hasHexTxHash(String hash) {
        if (TextUtils.isEmpty(hash)) {
            return false;
        }
        String trimmed = hash.trim().toLowerCase(Locale.US);
        return trimmed.startsWith("0x") && trimmed.length() >= 10;
    }

    private static String storageKey(String walletAddress) {
        return KEY_PREFIX + ChainAddressUtil.normalizeAddress(walletAddress);
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
                }
            }
            editor.apply();
        }
    }

    private static List<TokenTxRecord> loadList(Context context, String key) {
        migrateLegacyPrefs(context);
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(key, null);
        if (TextUtils.isEmpty(json)) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<List<TokenTxRecord>>() {}.getType();
            List<TokenTxRecord> list = GSON.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void saveList(Context context, String key, List<TokenTxRecord> list) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(key, GSON.toJson(list)).apply();
    }
}


