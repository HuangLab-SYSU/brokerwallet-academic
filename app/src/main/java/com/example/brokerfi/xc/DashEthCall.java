package com.example.brokerfi.xc;

import android.text.TextUtils;
import android.util.Log;

import com.example.brokerfi.token.TokenConfig;
import com.google.gson.Gson;
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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/** dash transport wrapper for {@code eth_call} and {@code eth_sendTransaction}. */
public final class DashEthCall {

    private static final String TAG = "DashEthCall";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private DashEthCall() {
    }

    public static String signedCall(String contractAddress, String dataHex, String privateKey) throws Exception {
        return signedCall(contractAddress, dataHex, privateKey, TokenConfig.DASH_ETH_CALL_TIMEOUT_MS);
    }

    public static String signedCall(
            String contractAddress,
            String dataHex,
            String privateKey,
            long timeoutMs) throws Exception {
        if (TextUtils.isEmpty(contractAddress) || TextUtils.isEmpty(dataHex) || TextUtils.isEmpty(privateKey)) {
            return null;
        }
        String to = ChainAddressUtil.normalizeAddress(contractAddress);
        String data = dataHex.startsWith("0x") || dataHex.startsWith("0X") ? dataHex : "0x" + dataHex;

        AtomicReference<String> reference = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        EXECUTOR.execute(() -> {
            try {
                String uuid = UUID.randomUUID().toString();
                CallReq req = new CallReq();
                String thedata = to + data + "0x0" + uuid;
                String[] sign = SecurityUtil.signECDSA(privateKey, thedata);

                req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
                req.setData(data);
                req.setRandomStr(uuid);
                req.setTo(to);
                req.setValue("0x0");
                req.setSign1(sign[0]);
                req.setSign2(sign[1]);

                byte[] bytes = postDash("eth_call", req);
                reference.set(bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null);
            } catch (Exception e) {
                errorRef.set(e);
                Log.w(TAG, "signed eth_call failed", e);
            } finally {
                latch.countDown();
            }
        });

        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("dash eth_call timed out");
        }
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        return extractResultHex(reference.get());
    }

    public static String sendSignedTransaction(
            String contractAddress,
            String dataHex,
            String privateKey,
            String valueHex,
            String gasLimitDec,
            String gasPriceDec) throws Exception {
        if (TextUtils.isEmpty(contractAddress) || TextUtils.isEmpty(dataHex) || TextUtils.isEmpty(privateKey)) {
            return null;
        }
        String to = ChainAddressUtil.normalizeAddress(contractAddress);
        String data = dataHex.startsWith("0x") || dataHex.startsWith("0X") ? dataHex : "0x" + dataHex;
        String value = normalizeValueHex(valueHex);
        String gas = normalizeGasHex(gasLimitDec, "0xf4240");

        AtomicReference<String> reference = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        EXECUTOR.execute(() -> {
            try {
                String uuid = UUID.randomUUID().toString();
                SendETHTXReq req = new SendETHTXReq();
                String thedata = to + data + value + gas + uuid;
                String[] sign = SecurityUtil.signECDSA(privateKey, thedata);

                req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
                req.setData(data);
                req.setRandomStr(uuid);
                req.setTo(to);
                req.setValue(value);
                req.setSign1(sign[0]);
                req.setSign2(sign[1]);
                req.setGas(gas);

                byte[] bytes = postDash("eth_sendTransaction", req);
                reference.set(bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null);
            } catch (Exception e) {
                errorRef.set(e);
                Log.w(TAG, "signed eth_sendTransaction failed", e);
            } finally {
                latch.countDown();
            }
        });

        long timeoutMs = TokenConfig.DASH_ETH_CALL_TIMEOUT_MS;
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("dash eth_sendTransaction timed out");
        }
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        return reference.get();
    }

    public static String directEthCall(String rpcUrl, String toAddress, String dataHex) throws Exception {
        String data = dataHex.startsWith("0x") || dataHex.startsWith("0X") ? dataHex : "0x" + dataHex;
        Map<String, Object> callObject = new LinkedHashMap<>();
        callObject.put("to", toAddress);
        callObject.put("data", data);
        List<Object> params = Arrays.asList(callObject, "latest");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("id", 1);
        body.put("method", "eth_call");
        body.put("params", params);

        String jsonBody = com.example.brokerfi.xc.net.GsonConverter.toJson(body);
        return extractResultHex(postJson(rpcUrl, jsonBody));
    }

    private static String normalizeValueHex(String valueHex) {
        if (TextUtils.isEmpty(valueHex)) {
            return "0x0";
        }
        String trimmed = valueHex.trim();
        if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        try {
            BigInteger value = new BigInteger(trimmed);
            return "0x" + value.toString(16);
        } catch (NumberFormatException e) {
            return "0x0";
        }
    }

    private static String normalizeGasHex(String gasLimit, String fallback) {
        if (gasLimit == null || gasLimit.trim().isEmpty()) {
            return fallback;
        }
        String trimmed = gasLimit.trim();
        if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        try {
            long value = Long.parseLong(trimmed);
            return "0x" + Long.toHexString(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static byte[] postDash(String path, Object requestBody) throws Exception {
        Gson gson = new Gson();
        String jsonInputString = gson.toJson(requestBody);
        String urlString = TokenConfig.getDashGatewayPostUrl(path);

        URL requestUrl = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        int timeoutMs = (int) TokenConfig.DASH_ETH_CALL_TIMEOUT_MS;
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs + 5_000);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HttpResponseCode: " + responseCode);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    private static String postJson(String rpcUrl, String jsonBody) throws Exception {
        URL url = new URL(rpcUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        int timeoutMs = (int) TokenConfig.DASH_ETH_CALL_TIMEOUT_MS;
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs + 5_000);

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

    static String extractResultHex(String responseBody) throws IllegalStateException {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return null;
        }
        String trimmed = responseBody.trim();
        if (!trimmed.startsWith("{")) {
            return normalizeHexResult(trimmed);
        }
        JsonObject json = JsonParser.parseString(trimmed).getAsJsonObject();
        if (json.has("error") && !json.get("error").isJsonNull()) {
            String message;
            if (json.get("error").isJsonObject()
                    && json.getAsJsonObject("error").has("message")) {
                message = json.getAsJsonObject("error").get("message").getAsString();
            } else if (json.get("error").isJsonPrimitive()) {
                message = json.get("error").getAsString();
            } else {
                message = json.get("error").toString();
            }
            throw new IllegalStateException(message);
        }
        if (!json.has("result") || json.get("result").isJsonNull()) {
            return null;
        }
        return normalizeHexResult(json.get("result").getAsString());
    }

    private static String normalizeHexResult(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return null;
        }
        if ("0x".equalsIgnoreCase(v)) {
            return "0x0000000000000000000000000000000000000000000000000000000000000000";
        }
        if (v.startsWith("0xexecution reverted") || v.contains("reverted")) {
            throw new IllegalStateException(v);
        }
        return v;
    }
}
