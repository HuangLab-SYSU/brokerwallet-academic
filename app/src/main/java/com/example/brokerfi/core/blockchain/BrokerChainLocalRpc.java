package com.example.brokerfi.core.blockchain;

import android.text.TextUtils;

import com.example.brokerfi.core.config.ChainConfig;
import com.example.brokerfi.core.network.GsonConverter;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BrokerChainLocalRpc {

    private BrokerChainLocalRpc() {
    }

    public static String getRpcUrl() {
        return ChainConfig.getLocalChainRpcUrl();
    }

    /** Standard JSON-RPC node uses 42515; the dash :443 root path does not support eth_getLogs. / ?? JSON-RPC ???? 42515?dash :443 ?????? eth_getLogs? */
    public static String getReadOnlyRpcUrl() {
        return ChainConfig.getReadOnlyRpcUrl();
    }

    public static String sendContractTransaction(
            String fromAddress,
            String toContract,
            String dataHex,
            String gasLimitDec,
            String gasPriceDec) {
        return sendContractTransaction(fromAddress, toContract, dataHex, "0x0", gasLimitDec, gasPriceDec);
    }

    public static String sendContractTransaction(
            String fromAddress,
            String toContract,
            String dataHex,
            String valueHex,
            String gasLimitDec,
            String gasPriceDec) {
        try {
            String from = ChainAddressUtil.normalizeAddress(fromAddress);
            String to = ChainAddressUtil.normalizeAddress(toContract);
            String data = dataHex.startsWith("0x") || dataHex.startsWith("0X") ? dataHex : "0x" + dataHex;
            String value = normalizeValueHex(valueHex);

            String nonceHex = jsonRpc("eth_getTransactionCount", Arrays.asList(from, "pending"));
            BigInteger nonce = hexToBigInteger(nonceHex);

            Map<String, Object> tx = new LinkedHashMap<>();
            tx.put("from", from);
            tx.put("to", to);
            tx.put("data", data);
            tx.put("value", value);
            tx.put("gas", toHexQuantity(parseQuantity(gasLimitDec, 1_500_000L)));
            tx.put("gasPrice", toHexQuantity(parseQuantity(gasPriceDec, 1_000_000_000L)));
            tx.put("chainId", "0x" + Long.toHexString(ChainConfig.LOCAL_CHAIN_ID));
            tx.put("nonce", "0x" + nonce.toString(16));

            return jsonRpc("eth_sendTransaction", Arrays.asList(tx));
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isReceiptSuccess(String txHash) {
        try {
            JsonObject receipt = getTransactionReceipt(txHash);
            if (receipt == null) {
                return false;
            }
            String status = receipt.has("status") ? receipt.get("status").getAsString() : "0x0";
            return "0x1".equals(status);
        } catch (Exception e) {
            return false;
        }
    }

    public static JsonObject getTransactionReceipt(String txHash) throws Exception {
        String hash = txHash.startsWith("0x") ? txHash : "0x" + txHash;
        String raw = jsonRpc("eth_getTransactionReceipt", Arrays.asList(hash));
        if (raw == null || raw.trim().isEmpty() || "null".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        if (raw.trim().startsWith("{")) {
            return JsonParser.parseString(raw.trim()).getAsJsonObject();
        }
        return null;
    }

    public static String jsonRpc(String method, List<?> params) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("id", 1);
        body.put("method", method);
        body.put("params", params);

        String response = postJson(getRpcUrl(), GsonConverter.toJson(body));
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        if (json.has("error") && !json.get("error").isJsonNull()) {
            String message = json.get("error").toString();
            if (json.get("error").isJsonObject() && json.getAsJsonObject("error").has("message")) {
                message = json.getAsJsonObject("error").get("message").getAsString();
            }
            throw new IllegalStateException(message);
        }
        if (!json.has("result") || json.get("result").isJsonNull()) {
            return null;
        }
        JsonElement result = json.get("result");
        if (result.isJsonPrimitive()) {
            return result.getAsString();
        }
        return result.toString();
    }

    private static String normalizeValueHex(String valueHex) {
        if (TextUtils.isEmpty(valueHex)) {
            return "0x0";
        }
        String trimmed = valueHex.trim();
        if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
            return trimmed.toLowerCase();
        }
        return "0x" + new BigInteger(trimmed).toString(16);
    }

    private static long parseQuantity(String value, long fallback) {
        if (TextUtils.isEmpty(value)) {
            return fallback;
        }
        String trimmed = value.trim();
        try {
            if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
                return new BigInteger(trimmed.substring(2), 16).longValue();
            }
            return new BigInteger(trimmed).longValue();
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String toHexQuantity(long value) {
        return "0x" + Long.toHexString(value);
    }

    private static BigInteger hexToBigInteger(String hex) {
        if (hex == null || hex.isEmpty()) {
            return BigInteger.ZERO;
        }
        String v = hex.startsWith("0x") || hex.startsWith("0X") ? hex.substring(2) : hex;
        if (v.isEmpty()) {
            return BigInteger.ZERO;
        }
        return new BigInteger(v, 16);
    }

    private static String postJson(String rpcUrl, String jsonBody) throws Exception {
        URL url = new URL(rpcUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(60_000);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = connection.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream(),
                StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("RPC HTTP " + code + ": " + sb);
        }
        return sb.toString();
    }
}
