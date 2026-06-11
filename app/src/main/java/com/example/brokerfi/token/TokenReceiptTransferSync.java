package com.example.brokerfi.token;

import com.example.brokerfi.xc.ChainAddressUtil;
import com.example.brokerfi.xc.MyUtil;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Restores ERC-20 transfer history from receipt logs when {@code gettx2}
 * misses token transfers.
 */
public final class TokenReceiptTransferSync {

    private static final String TAG = "TokenReceiptTransferSync";
    private static final String TRANSFER_TOPIC =
            "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    private TokenReceiptTransferSync() {
    }

    public static void sync(
            Context context,
            String wallet,
            @Nullable String privateKey,
            Set<String> contractSet,
            List<TokenItem> enabled) {
        if (context == null || TextUtils.isEmpty(wallet) || contractSet == null || contractSet.isEmpty()) {
            return;
        }
        Set<String> hashes = collectHashes(context, wallet);
        if (hashes.isEmpty()) {
            return;
        }
        String walletHex = strip0x(wallet);
        int added = 0;
        int scanned = 0;
        for (String hash : hashes) {
            if (scanned >= 40) {
                break;
            }
            scanned++;
            try {
                added += syncReceipt(context, wallet, walletHex, hash, privateKey, contractSet, enabled);
            } catch (Exception e) {
                Log.w(TAG, "receipt sync failed hash=" + hash, e);
            }
        }
        if (added > 0) {
            Log.i(TAG, "receipt sync added=" + added + " scanned=" + scanned + " wallet=" + walletHex);
        }
    }

    private static Set<String> collectHashes(Context context, String wallet) {
        Set<String> hashes = new LinkedHashSet<>();
        for (String hash : TokenOutboundNotifyStore.allHashes(context)) {
            hashes.add(hash);
        }
        for (String hash : TokenOutboundNotifyStore.hashesForWallet(context, wallet)) {
            hashes.add(hash);
        }
        for (TokenTxRecord record : TokenTxHistoryStore.getAll(context, wallet)) {
            if (record != null && hasHexTxHash(record.txHash)) {
                hashes.add(record.txHash.trim().toLowerCase(Locale.US));
            }
        }
        return hashes;
    }

    private static int syncReceipt(
            Context context,
            String wallet,
            String walletHex,
            String txHash,
            @Nullable String privateKey,
            Set<String> contractSet,
            List<TokenItem> enabled) throws Exception {
        String hash = txHash.startsWith("0x") ? txHash : "0x" + txHash;
        JsonObject receipt = fetchReceipt(hash, privateKey);
        if (receipt == null) {
            return 0;
        }
        if (!"0x1".equalsIgnoreCase(receipt.has("status") ? receipt.get("status").getAsString() : "0x0")) {
            return 0;
        }
        long timestampMs = resolveBlockTimestamp(receipt);
        JsonArray logs = receipt.has("logs") && receipt.get("logs").isJsonArray()
                ? receipt.getAsJsonArray("logs")
                : null;
        if (logs == null || logs.size() == 0) {
            return 0;
        }
        int added = 0;
        for (JsonElement element : logs) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject log = element.getAsJsonObject();
            if (!isTransferLog(log)) {
                continue;
            }
            String contractHex = strip0x(log.has("address") ? log.get("address").getAsString() : "");
            if (!contractSetContains(contractSet, contractHex)) {
                continue;
            }
            String fromHex = topicAddress(log, 1);
            String toHex = topicAddress(log, 2);
            BigInteger value = parseLogValue(log);
            if (value == null || value.signum() <= 0) {
                continue;
            }
            TokenItem token = findTokenByContractHex(enabled, contractHex);
            int decimals = token != null ? token.getDecimals() : TokenConfig.TOKEN_DECIMALS;
            String amount = formatWeiValue(value, decimals);
            if ("0".equals(amount)) {
                continue;
            }
            String contractDisplay = ChainAddressUtil.displayAddress("0x" + contractHex);
            if (addressEquals(toHex, walletHex)) {
                String fromDisplay = ChainAddressUtil.displayAddress("0x" + fromHex);
                TokenTxRecord record = TokenTxRecord.receive(
                        amount,
                        fromDisplay,
                        hash,
                        contractDisplay,
                        timestampMs);
                if (persist(context, wallet, record)) {
                    added++;
                }
            }
            if (addressEquals(fromHex, walletHex)) {
                String toDisplay = ChainAddressUtil.displayAddress("0x" + toHex);
                TokenTxRecord record = TokenTxRecord.send(amount, toDisplay, hash, contractDisplay);
                record.timestampMs = timestampMs;
                if (persist(context, wallet, record)) {
                    added++;
                }
            }
        }
        return added;
    }

    private static boolean persist(Context context, String wallet, TokenTxRecord record) {
        if (TokenTxHistoryStore.isDuplicateOfStored(context, wallet, record)) {
            return false;
        }
        return TokenTxHistoryStore.addIfAbsent(context, wallet, record);
    }

    @Nullable
    private static JsonObject fetchReceipt(String txHash, @Nullable String privateKey) throws Exception {
        String body = null;
        if (!TextUtils.isEmpty(privateKey)) {
            String hashBody = txHash.startsWith("0x") ? txHash.substring(2) : txHash;
            body = MyUtil.getTransactionReceipt(hashBody, privateKey);
        }
        if (TextUtils.isEmpty(body)) {
            body = fetchReceiptUnsigned(txHash);
        }
        if (TextUtils.isEmpty(body)) {
            return null;
        }
        JsonObject json = JsonParser.parseString(body.trim()).getAsJsonObject();
        if (json.has("result") && json.get("result").isJsonObject()) {
            return json.getAsJsonObject("result");
        }
        if (json.has("status") || json.has("logs")) {
            return json;
        }
        return null;
    }

    @Nullable
    private static String fetchReceiptUnsigned(String txHash) throws Exception {
        String rpcUrl = TokenConfig.CHAIN_JSON_RPC_URL;
        String payload = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"eth_getTransactionReceipt\",\"params\":[\""
                + txHash + "\"]}";
        URL url = new URL(rpcUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setDoOutput(true);
        int timeout = (int) TokenConfig.DEFAULT_CHAIN_READ_TIMEOUT_MS;
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static long resolveBlockTimestamp(JsonObject receipt) {
        if (!receipt.has("blockNumber")) {
            return System.currentTimeMillis();
        }
        try {
            String blockHex = receipt.get("blockNumber").getAsString();
            JsonObject block = fetchBlockUnsigned(blockHex);
            if (block != null && block.has("timestamp")) {
                long seconds = Long.decode(block.get("timestamp").getAsString());
                return seconds * 1000L;
            }
        } catch (Exception e) {
            Log.w(TAG, "block timestamp", e);
        }
        return System.currentTimeMillis();
    }

    @Nullable
    private static JsonObject fetchBlockUnsigned(String blockNumberHex) throws Exception {
        String payload = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"eth_getBlockByNumber\",\"params\":[\""
                + blockNumberHex + "\",false]}";
        URL url = new URL(TokenConfig.CHAIN_JSON_RPC_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setDoOutput(true);
        int timeout = (int) TokenConfig.DEFAULT_CHAIN_READ_TIMEOUT_MS;
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            if (json.has("result") && json.get("result").isJsonObject()) {
                return json.getAsJsonObject("result");
            }
        }
        return null;
    }

    private static boolean isTransferLog(JsonObject log) {
        if (!log.has("topics") || !log.get("topics").isJsonArray()) {
            return false;
        }
        JsonArray topics = log.getAsJsonArray("topics");
        if (topics.size() < 3) {
            return false;
        }
        String topic0 = topics.get(0).getAsString().toLowerCase(Locale.US);
        return TRANSFER_TOPIC.equals(topic0);
    }

    @Nullable
    private static String topicAddress(JsonObject log, int index) {
        if (!log.has("topics") || !log.get("topics").isJsonArray()) {
            return "";
        }
        JsonArray topics = log.getAsJsonArray("topics");
        if (topics.size() <= index) {
            return "";
        }
        String topic = topics.get(index).getAsString();
        if (TextUtils.isEmpty(topic)) {
            return "";
        }
        String hex = strip0x(topic);
        if (hex.length() < 40) {
            return "";
        }
        return hex.substring(hex.length() - 40).toLowerCase(Locale.US);
    }

    @Nullable
    private static BigInteger parseLogValue(JsonObject log) {
        if (!log.has("data")) {
            return null;
        }
        String data = log.get("data").getAsString();
        if (TextUtils.isEmpty(data) || "0x".equalsIgnoreCase(data.trim())) {
            return BigInteger.ZERO;
        }
        try {
            String hex = strip0x(data);
            if (hex.isEmpty()) {
                return BigInteger.ZERO;
            }
            return new BigInteger(hex, 16);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean hasHexTxHash(String hash) {
        if (TextUtils.isEmpty(hash)) {
            return false;
        }
        String trimmed = hash.trim().toLowerCase(Locale.US);
        return trimmed.startsWith("0x") && trimmed.length() >= 10;
    }

    private static boolean contractSetContains(Set<String> contractSet, String addressHex) {
        return contractSet != null && contractSet.contains(strip0x(addressHex));
    }

    @Nullable
    private static TokenItem findTokenByContractHex(List<TokenItem> enabled, String contractHex) {
        if (enabled == null || TextUtils.isEmpty(contractHex)) {
            return null;
        }
        String target = strip0x(contractHex);
        for (TokenItem token : enabled) {
            if (target.equals(strip0x(token.getContractAddress()))) {
                return token;
            }
        }
        return null;
    }

    private static boolean addressEquals(String a, String b) {
        if (TextUtils.isEmpty(a) || TextUtils.isEmpty(b)) {
            return false;
        }
        return TextUtils.equals(strip0x(a), strip0x(b));
    }

    private static String formatWeiValue(BigInteger wei, int decimals) {
        return TokenAmountUtil.formatDisplayAmount(
                TokenAmountUtil.fromChainUnits(wei, decimals));
    }

    private static String strip0x(String address) {
        if (address == null) {
            return "";
        }
        String normalized = ChainAddressUtil.normalizeAddress(address);
        return normalized.startsWith("0x") ? normalized.substring(2).toLowerCase(Locale.US) : normalized;
    }
}
