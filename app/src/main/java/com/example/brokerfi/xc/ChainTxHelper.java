package com.example.brokerfi.xc;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.example.brokerfi.R;
import com.example.brokerfi.token.TokenConfig;

import org.json.JSONObject;

/**
 * wBKC 閾句笂鍐欐搷浣滐細dash 绛惧悕 {@code eth_sendTransaction}銆? */
public final class ChainTxHelper {

    private static final String TAG = "ChainTxHelper";
    /** 绾?120s锛氭參鍧楁椂鍑忓皯璇姤 pending銆?*/
    private static final int RECEIPT_POLL_MAX = 80;
    private static final long RECEIPT_POLL_DELAY_MS = 1500L;

    public enum ReceiptStatus {
        SUCCESS,
        PENDING,
        FAILED
    }

    private ChainTxHelper() {
    }

    public static String sendContractCall(
            String privateKey,
            String contractAddress,
            String dataHex,
            String valueHex,
            String gasLimitDec,
            String gasPriceDec) {
        if (TextUtils.isEmpty(privateKey) || TextUtils.isEmpty(contractAddress)) {
            return null;
        }
        if (TokenConfig.useLocalBrokerChainNode()) {
            try {
                String from = SecurityUtil.GetAddress(privateKey);
                String hash = BrokerChainLocalRpc.sendContractTransaction(
                        from,
                        contractAddress,
                        dataHex,
                        valueHex,
                        gasLimitDec,
                        gasPriceDec);
                if (!TextUtils.isEmpty(hash)) {
                    return normalizeHash(hash);
                }
            } catch (Exception e) {
                Log.e(TAG, "local send failed", e);
            }
            return null;
        }

        try {
            String result = DashEthCall.sendSignedTransaction(
                    contractAddress,
                    dataHex,
                    privateKey,
                    valueHex,
                    gasLimitDec,
                    gasPriceDec);
            String hash = parseTransactionHash(result);
            if (!TextUtils.isEmpty(hash)) {
                return hash;
            }
            Log.w(TAG, "gateway send empty hash, raw=" + result);
        } catch (Exception e) {
            Log.e(TAG, "gateway send failed", e);
        }
        return null;
    }

    public static boolean waitReceiptSuccess(String txHash, String privateKey) throws InterruptedException {
        return waitReceiptStatus(txHash, privateKey) == ReceiptStatus.SUCCESS;
    }

    /**
     * @return SUCCESS 宸茬‘璁わ紱PENDING 瓒呮椂鏈鍒板洖鎵э紱FAILED 閾句笂 execution reverted
     */
    public static ReceiptStatus waitReceiptStatus(String txHash, String privateKey)
            throws InterruptedException {
        if (TextUtils.isEmpty(txHash)) {
            return ReceiptStatus.FAILED;
        }
        String hash = txHash.startsWith("0x") ? txHash : "0x" + txHash;
        for (int i = 0; i < RECEIPT_POLL_MAX; i++) {
            int state = queryReceiptState(hash, privateKey);
            if (state == 1) {
                return ReceiptStatus.SUCCESS;
            }
            if (state == 0) {
                return ReceiptStatus.FAILED;
            }
            Thread.sleep(RECEIPT_POLL_DELAY_MS);
        }
        return ReceiptStatus.PENDING;
    }

    /** -1 鏈煡/灏氭棤鍥炴墽锛? 澶辫触锛? 鎴愬姛銆?*/
    private static int queryReceiptState(String txHash, String privateKey) {
        if (TextUtils.isEmpty(txHash)) {
            return -1;
        }
        try {
            if (TokenConfig.useLocalBrokerChainNode()) {
                if (BrokerChainLocalRpc.isReceiptSuccess(txHash)) {
                    return 1;
                }
                return -1;
            }
            if (!TextUtils.isEmpty(privateKey)) {
                String hashBody = txHash.startsWith("0x") ? txHash.substring(2) : txHash;
                String receiptJson = MyUtil.getTransactionReceipt(hashBody, privateKey);
                if (receiptJson == null) {
                    return -1;
                }
                JSONObject response = new JSONObject(receiptJson.trim());
                JSONObject receipt = response.optJSONObject("result");
                if (receipt == null) {
                    return -1;
                }
                String status = receipt.optString("status", "0x0");
                if ("0x1".equals(status)) {
                    return 1;
                }
                if ("0x0".equals(status)) {
                    return 0;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "receipt check", e);
        }
        return -1;
    }

    public static boolean isReceiptSuccess(String txHash, String privateKey) {
        return queryReceiptState(
                txHash.startsWith("0x") ? txHash : "0x" + txHash,
                privateKey) == 1;
    }

    public static String getSendFailureHint(Context context) {
        if (context == null) {
            return "";
        }
        Context app = context.getApplicationContext();
        if (TokenConfig.useDashChainOnly()) {
            return app.getString(R.string.token_send_failure_hint_dash);
        }
        if (TokenConfig.useLocalBrokerChainNode()) {
            return app.getString(R.string.token_send_failure_hint_local);
        }
        return app.getString(R.string.token_send_failure_hint_network);
    }

    static String parseTransactionHash(String gatewayResponse) {
        if (gatewayResponse == null) {
            return null;
        }
        String trimmed = gatewayResponse.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("{")) {
            try {
                JSONObject json = new JSONObject(trimmed);
                if (json.has("error")) {
                    return null;
                }
                String result = json.optString("result", null);
                if (!TextUtils.isEmpty(result) && !"null".equalsIgnoreCase(result)) {
                    return normalizeHash(result);
                }
            } catch (Exception e) {
                Log.w(TAG, "parse gateway json", e);
            }
            return null;
        }
        return normalizeHash(trimmed);
    }

    private static String normalizeHash(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String v = value.trim();
        if (!v.startsWith("0x") && !v.startsWith("0X")) {
            v = "0x" + v;
        }
        return v.length() >= 66 ? v : null;
    }
}


