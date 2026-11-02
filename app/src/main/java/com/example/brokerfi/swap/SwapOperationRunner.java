package com.example.brokerfi.swap;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.example.brokerfi.token.TokenTxHistoryStore;
import com.example.brokerfi.token.TokenTxRecord;
import com.example.brokerfi.token.TokenWalletHelper;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Continues saving receipts after leaving the screen. Reopening only polls, NEVER resends. */
public final class SwapOperationRunner {
    private static final Set<String> ACTIVE = java.util.Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final ExecutorService WORK = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    public interface Listener { void finished(TokenTxRecord record); }
    private SwapOperationRunner() {}
    public static boolean active(String wallet) { return ACTIVE.contains(wallet.toLowerCase(Locale.ROOT)); }
    public static boolean submit(Context context, BrokerSwapConfig cfg, String wallet, TokenTxRecord record,
                                 String target, String data, BigInteger value, Listener listener) {
        Context app = context.getApplicationContext();
        String lock = wallet.toLowerCase(Locale.ROOT);
        if (!ACTIVE.add(lock)) return false;
        WORK.execute(() -> {
            SwapSendDiagnostics diagnostics=new SwapSendDiagnostics(record,
                    () -> TokenTxHistoryStore.upsertSwap(app,wallet,record));
            boolean hashJournaled=false;
            try {
                if (!wallet.equalsIgnoreCase(BrokerSwapConfig.walletId(TokenWalletHelper.getWalletAddress(app)))) throw new IllegalStateException("Account changed");
                for (TokenTxRecord old : TokenTxHistoryStore.getAll(app, wallet))
                    if (TokenTxHistoryStore.isBlockingSwap(old)) throw new IllegalStateException("Unresolved operation");
                record.status = "SUBMITTING";
                record.sendRequestId=java.util.UUID.randomUUID().toString();
                record.sendStartedMs=System.currentTimeMillis();
                diagnostics.stage("PREPARE");
                String key = TokenWalletHelper.getCurrentPrivateKey(app);
                if (!wallet.equalsIgnoreCase(BrokerSwapConfig.walletId(TokenWalletHelper.getWalletAddress(app)))
                        || !wallet.equalsIgnoreCase(BrokerSwapConfig.walletId(com.example.brokerfi.core.security.SecurityUtil.GetAddress(key))))
                    throw new IllegalStateException("Account changed");
                BrokerSwapClient client = new BrokerSwapClient(cfg);
                record.txHash = client.send(key, target, data, value, diagnostics);
                record.status = "PENDING";
                diagnostics.stage("HASH_RECEIVED");
                hashJournaled=true;
                poll(app, client, key, wallet, record);
            } catch (Exception e) {
                if (hashJournaled) {
                    if(record.receiptFailure==null)record.receiptFailure=SwapSendDiagnostics.category(e);
                }
                else diagnostics.failure(e);
                record.status = SwapSendDiagnostics.failureStatus(record);
                TokenTxHistoryStore.upsertSwap(app, wallet, record);
            } finally {
                ACTIVE.remove(lock);
                MAIN.post(() -> listener.finished(record));
            }
        });
        return true;
    }
    public static boolean resume(Context context, BrokerSwapConfig cfg, String wallet, TokenTxRecord record, Listener listener) {
        if (!SwapReceipt.validHash(record.txHash)) return false;
        String lock = wallet.toLowerCase(Locale.ROOT);
        if (!ACTIVE.add(lock)) return false;
        Context app = context.getApplicationContext();
        WORK.execute(() -> {
            try {
                if (wallet.equalsIgnoreCase(BrokerSwapConfig.walletId(TokenWalletHelper.getWalletAddress(app))))
                    poll(app, new BrokerSwapClient(cfg), TokenWalletHelper.getCurrentPrivateKey(app), wallet, record);
            } catch (Exception e) {
                if(record.receiptFailure==null)record.receiptFailure=SwapSendDiagnostics.category(e);
                TokenTxHistoryStore.upsertSwap(app,wallet,record);
            } finally {
                ACTIVE.remove(lock); MAIN.post(() -> listener.finished(record));
            }
        });
        return true;
    }
    private static void poll(Context app, BrokerSwapClient client, String key, String wallet, TokenTxRecord record) throws Exception {
        record.receiptFailure=null;
        long stopAt=android.os.SystemClock.elapsedRealtime()+15*60*1000;
        for (int i = 0; i < 30; i++) {
            if(i>0 && android.os.SystemClock.elapsedRealtime()>=stopAt)break;
            record.receiptAttempts++;
            record.receiptLastCheckedMs=System.currentTimeMillis();
            if (record.isLiquidity()) {
                LiquidityReceipt receipt=LiquidityReceipt.parse(client.receiptRaw(key,record.txHash),client.deployment(),record,wallet);
                record.status=receipt.status;
                if (receipt.musdt!=null) record.actualMusdt=SwapQuoteMath.format(receipt.musdt,6);
                if (receipt.bkc!=null) record.actualBkc=SwapQuoteMath.format(receipt.bkc,18);
                if (receipt.lp!=null) record.actualLp=SwapQuoteMath.format(receipt.lp,18);
                if (receipt.gasCost!=null) record.gasCost=SwapQuoteMath.format(receipt.gasCost,18);
                saveReceipt(app,wallet,record);
                if (!"PENDING".equals(record.status)) break;
                if (i<29) Thread.sleep(2000);
                continue;
            }
            SwapReceipt receipt = client.receipt(key, record.txHash, SwapAsset.valueOf(record.fromAsset),
                    SwapAsset.valueOf(record.toAsset), new BigInteger(record.inputUnits), wallet);
            record.status = receipt.status;
            if (receipt.actualOutput != null && !"APPROVAL".equals(record.type))
                record.actualOutput = SwapQuoteMath.format(receipt.actualOutput, SwapAsset.valueOf(record.toAsset).decimals);
            if (receipt.gasCost != null) record.gasCost = SwapQuoteMath.format(receipt.gasCost, 18);
            saveReceipt(app,wallet,record);
            if (!"PENDING".equals(record.status)) break;
            if (i < 29) Thread.sleep(2000);
        }
    }
    private static void saveReceipt(Context app,String wallet,TokenTxRecord record) throws java.io.IOException {
        if(!TokenTxHistoryStore.upsertSwap(app,wallet,record)) {
            record.receiptFailure="JOURNAL";
            throw new java.io.IOException("Cannot persist receipt");
        }
    }
}
