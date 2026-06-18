package com.example.brokerfi.token;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import com.example.brokerfi.core.blockchain.ChainAddressUtil;
import com.example.brokerfi.core.config.ChainConfig;
import com.example.brokerfi.brokerfi.model.Transaction;
import com.example.brokerfi.brokerfi.model.TransactionResponse;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Backfills ERC-20 inbound and outbound history using dash {@code gettx2} plus receipt-based
 * Transfer log recovery.
 * <p>
 * The gettx2 response still contains wallet-to-wallet native rows that should not appear in
 * ERC-20 history, so this sync pass records those ids and removes the corresponding SEND/RECEIVE
 * entries from the token store after the receipt scan finishes.
 */
public final class TokenIncomingTxSync {

    private static final String TAG = "TokenIncomingTxSync";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private TokenIncomingTxSync() {
    }

    public static void sync(Context context, Runnable onComplete) {
        if (context == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                runSync(context.getApplicationContext());
            } catch (Exception e) {
                Log.w(TAG, "sync failed", e);
            } finally {
                if (onComplete != null) {
                    MAIN.post(onComplete);
                }
            }
        });
    }

    private static void runSync(Context context) {
        String wallet = TokenWalletHelper.getWalletAddress(context);
        if (TextUtils.isEmpty(wallet)) {
            return;
        }
        int purged = TokenTxHistoryStore.sanitizeWalletHistory(context, wallet);
        if (purged > 0) {
            Log.i(TAG, "sanitized " + purged + " misclassified peer gettx2 records before sync");
        }
        String wrappedBkcContract = wrappedBkcContractHelper.resolveContractAddress(context);
        List<TokenItem> enabled = TokenStore.getEnabledTokens(context, wrappedBkcContract);
        if (enabled.isEmpty()) {
            return;
        }

        Set<String> contractSet = new HashSet<>();
        for (TokenItem token : enabled) {
            String addr = strip0x(token.getContractAddress());
            if (!TextUtils.isEmpty(addr)) {
                contractSet.add(addr);
            }
        }

        TokenOutboundNotifyStore.seedFromAllStoredHistories(context);
        GetTx2SyncResult walletScan = syncFromGetTx2(context, wallet, contractSet);
        String privateKey = TokenWalletHelper.getCurrentPrivateKey(context);
        TokenReceiptTransferSync.sync(context, wallet, privateKey, contractSet, enabled);
        int removedPeer = TokenTxHistoryStore.removeGetTx2WalletPeerRecords(
                context, wallet, walletScan.walletPeerGetTx2Ids);
        if (removedPeer > 0) {
            Log.i(TAG, "removed " + removedPeer + " wallet-peer gettx2 records from token history");
        }
        int removedMisclassified = TokenTxHistoryStore.removeMisclassifiedPeerGetTx2Records(context, wallet);
        if (removedMisclassified > 0) {
            Log.i(TAG, "removed " + removedMisclassified + " misclassified peer gettx2 records");
        }
        int compacted = TokenTxHistoryStore.compactWallet(context, wallet);
        if (compacted > 0) {
            Log.i(TAG, "compactWallet removed " + compacted + " duplicate records");
        }
    }

    private static final class GetTx2SyncResult {
        final Set<String> walletPeerGetTx2Ids = new HashSet<>();

        GetTx2SyncResult() {
        }
    }

    private static GetTx2SyncResult syncFromGetTx2(
            Context context,
            String wallet,
            Set<String> contractSet,
            String queryAccountHex) {
        GetTx2SyncResult result = new GetTx2SyncResult();
        try {
            String walletHex = strip0x(wallet);
            boolean walletQuery = addressEquals(queryAccountHex, walletHex);
            String url = ChainConfig.getGetTx2AccountUrl(queryAccountHex);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(15_000);
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "gettx2 HTTP " + connection.getResponseCode());
                return result;
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();

            TransactionResponse transactionResponse =
                    new Gson().fromJson(response.toString(), TransactionResponse.class);
            if (transactionResponse == null || transactionResponse.getData() == null) {
                return result;
            }

            for (Transaction tx : transactionResponse.getData()) {
                if (!passesActivityFilter(tx)) {
                    continue;
                }
                String from = strip0x(tx.getFrom());
                String to = strip0x(tx.getTo());
                String txId = tx.getId() != null ? String.valueOf(tx.getId()) : "";
                if (TextUtils.isEmpty(txId)) {
                    continue;
                }
                if (walletQuery && isWalletOnlyPeerTransfer(from, to, contractSet, walletHex)) {
                    result.walletPeerGetTx2Ids.add(txId);
                }
                if (isNativeBkcPeerTransfer(tx)) {
                    result.walletPeerGetTx2Ids.add(txId);
                }
                // Ignore gettx2 numeric SEND/RECEIVE rows here; ERC-20 history is recovered elsewhere.
            }
            Log.i(TAG, "gettx2 wallet scan done wallet=" + walletHex
                    + " peerIds=" + result.walletPeerGetTx2Ids.size());
        } catch (Exception e) {
            Log.w(TAG, "gettx2 sync failed", e);
        }
        return result;
    }

    private static GetTx2SyncResult syncFromGetTx2(
            Context context,
            String wallet,
            Set<String> contractSet) {
        return syncFromGetTx2(context, wallet, contractSet, strip0x(wallet));
    }

    /**
     * Returns true when a gettx2 row is only a wallet-to-wallet native transfer and does not
     * reference any tracked ERC-20 contract address.
     */
    private static boolean isWalletOnlyPeerTransfer(
            String from,
            String to,
            Set<String> contractSet,
            String walletHex) {
        if (TextUtils.isEmpty(from) || TextUtils.isEmpty(to)) {
            return false;
        }
        if (!isValidApiAddress("0x" + from) || !isValidApiAddress("0x" + to)) {
            return false;
        }
        if (contractSetContains(contractSet, from) || contractSetContains(contractSet, to)) {
            return false;
        }
        return addressEquals(from, walletHex) || addressEquals(to, walletHex);
    }

    /** Heuristic for native BKC peer transfers reported by gettx2: valid endpoints plus a fee field. */
    static boolean isNativeBkcPeerTransfer(Transaction tx) {
        if (tx == null) {
            return false;
        }
        String from = strip0x(tx.getFrom());
        String to = strip0x(tx.getTo());
        if (TextUtils.isEmpty(from) || TextUtils.isEmpty(to)) {
            return false;
        }
        if (!isValidApiAddress("0x" + from) || !isValidApiAddress("0x" + to)) {
            return false;
        }
        String fee = tx.getFee();
        if (fee == null || fee.trim().isEmpty()) {
            return false;
        }
        try {
            return new BigInteger(fee.trim()).signum() >= 0;
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean contractSetContains(Set<String> contractSet, String addressHex) {
        if (contractSet == null || TextUtils.isEmpty(addressHex)) {
            return false;
        }
        return contractSet.contains(strip0x(addressHex));
    }

    private static boolean addressEquals(String a, String b) {
        if (TextUtils.isEmpty(a) || TextUtils.isEmpty(b)) {
            return false;
        }
        return TextUtils.equals(strip0x(a), strip0x(b));
    }

    private static boolean passesActivityFilter(Transaction tx) {
        if (tx == null) {
            return false;
        }
        boolean containsB2E = (tx.getFrom() != null && tx.getFrom().contains("B2E"))
                || (tx.getTo() != null && tx.getTo().contains("B2E"));
        boolean hasInvalidAddress = !isValidApiAddress(tx.getFrom()) || !isValidApiAddress(tx.getTo());
        return !containsB2E && !hasInvalidAddress;
    }

    private static boolean isValidApiAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }
        String s = address.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        return s.length() == 40 && s.matches("^[0-9a-fA-F]+$");
    }

    private static String strip0x(String address) {
        if (address == null) {
            return "";
        }
        String normalized = ChainAddressUtil.normalizeAddress(address);
        return normalized.startsWith("0x") ? normalized.substring(2) : normalized;
    }
}
