package com.example.brokerfi.token;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import com.example.brokerfi.core.blockchain.ChainAddressUtil;
import com.example.brokerfi.core.util.MyUtil;
import com.example.brokerfi.core.security.SecurityUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory snapshot cache for native BKC and ERC-20 balances.
 * Native balance may be seeded from the home screen, while token balances are refreshed through
 * signed {@code eth_call} reads and persisted as raw chain units for quick reuse.
 */
public final class TokenBalanceCache {

    private static final String TAG = "TokenBalanceCache";
    private static final String PREFS = "token_balance_cache";

    private static final Object LOCK = new Object();

    private static volatile boolean tokenFetchInFlight;
    private static volatile long fetchGeneration;

    /** Pending request state used to coalesce multiple token balance fetches into one follow-up read. */
    private static String pendingContract;
    private static android.content.Context pendingContext;
    private static String pendingPrivateKey;
    private static String pendingWallet;
    private static boolean pendingForce;

    private static String cachedWallet;
    private static String cachedContract;
    private static String cachedNativeBalance = "0";
    private static BigInteger cachedTokenBalance = BigInteger.ZERO;
    private static long cachedAtMs;
    private static boolean nativeFromHome;

    private static final List<Runnable> pendingCompletes = new ArrayList<>();

    /** LRU snapshots keyed by wallet+contract so recent ERC-20 balances can be reused across screens. */
    private static final LinkedHashMap<String, Snapshot> tokenSnapshots =
            new LinkedHashMap<String, Snapshot>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Snapshot> eldest) {
                    return size() > 12;
                }
            };

    public static final class Snapshot {
        public final String walletAddress;
        public final String contractAddress;
        public final String nativeBalanceDisplay;
        public final BigInteger tokenBalance;

        Snapshot(String walletAddress, String contractAddress,
                 String nativeBalanceDisplay, BigInteger tokenBalance) {
            this.walletAddress = walletAddress;
            this.contractAddress = contractAddress;
            this.nativeBalanceDisplay = nativeBalanceDisplay;
            this.tokenBalance = tokenBalance != null ? tokenBalance : BigInteger.ZERO;
        }
    }

    private TokenBalanceCache() {
    }

    /**
     * Seeds the native BKC balance from the home screen cache and optionally schedules the token
     * side of the snapshot if it is still missing.
     */
    public static void seedNativeFromHome(Context context, String nativeBalance) {
        String wallet = TokenWalletHelper.getWalletAddress(context);
        if (TextUtils.isEmpty(wallet) || nativeBalance == null) {
            return;
        }
        wallet = ChainAddressUtil.normalizeAddress(wallet);
        String contract = wrappedBkcContractHelper.resolveContractAddress(context);
        synchronized (LOCK) {
            boolean sameWallet = TextUtils.equals(cachedWallet, wallet);
            BigInteger keepToken = sameWallet ? cachedTokenBalance : BigInteger.ZERO;
            cachedWallet = wallet;
            cachedContract = contractKey(contract);
            cachedNativeBalance = nativeBalance;
            cachedTokenBalance = keepToken;
            cachedAtMs = System.currentTimeMillis();
            nativeFromHome = true;
        }
        // Native balance is already known here; fetch the token side only when needed.
        if (getSnapshot(wallet, contract) == null) {
            prefetchTokenBalance(context, contract, false, null);
        }
    }

    public static boolean hasValidSnapshot(String walletAddress, String contractAddress) {
        synchronized (LOCK) {
            return matchesAccount(walletAddress, contractAddress) && cachedAtMs > 0;
        }
    }

    /** Returns true when the cache has a native balance for the wallet, typically seeded from home. */
    public static boolean hasNativeBalance(String walletAddress) {
        synchronized (LOCK) {
            return matchesAccount(walletAddress, null) && cachedAtMs > 0 && nativeFromHome;
        }
    }

    public static Snapshot getSnapshot(String walletAddress, String contractAddress) {
        synchronized (LOCK) {
            return tokenSnapshots.get(snapshotKey(walletAddress, contractAddress));
        }
    }

    /** Resolves a snapshot from memory first, then from persisted token-chain units on disk. */
    @Nullable
    public static Snapshot resolveSnapshot(
            Context context, String walletAddress, String contractAddress) {
        Snapshot memory = getSnapshot(walletAddress, contractAddress);
        if (memory != null) {
            return memory;
        }
        if (context == null || TextUtils.isEmpty(walletAddress) || TextUtils.isEmpty(contractAddress)) {
            return null;
        }
        BigInteger persisted = loadPersistedWei(context, walletAddress, contractAddress);
        if (persisted == null) {
            return null;
        }
        String wallet = ChainAddressUtil.normalizeAddress(walletAddress);
        String contract = contractKey(contractAddress);
        Snapshot disk = new Snapshot(wallet, contract, "0", persisted);
        synchronized (LOCK) {
            tokenSnapshots.put(snapshotKey(wallet, contract), disk);
        }
        return disk;
    }

    /** Returns the cached native BKC display balance for the active wallet, if available. */
    @Nullable
    public static String getNativeBalanceDisplay(String walletAddress) {
        synchronized (LOCK) {
            if (TextUtils.isEmpty(walletAddress)
                    || TextUtils.isEmpty(cachedWallet)
                    || cachedAtMs <= 0) {
                return null;
            }
            if (!TextUtils.equals(cachedWallet, ChainAddressUtil.normalizeAddress(walletAddress))) {
                return null;
            }
            return cachedNativeBalance;
        }
    }

    public static void store(String walletAddress, String contractAddress,
                             String nativeBalanceDisplay, BigInteger tokenBalance) {
        synchronized (LOCK) {
            String wallet = TextUtils.isEmpty(walletAddress)
                    ? walletAddress
                    : ChainAddressUtil.normalizeAddress(walletAddress);
            String contract = contractKey(contractAddress);
            cachedWallet = wallet;
            cachedContract = contract;
            if (nativeBalanceDisplay != null) {
                cachedNativeBalance = nativeBalanceDisplay;
            }
            cachedTokenBalance = tokenBalance != null ? tokenBalance : BigInteger.ZERO;
            cachedAtMs = System.currentTimeMillis();
            tokenSnapshots.put(
                    snapshotKey(wallet, contract),
                    new Snapshot(wallet, contract, cachedNativeBalance, cachedTokenBalance));
        }
    }

    public static void invalidate() {
        synchronized (LOCK) {
            clearCacheStateLocked();
        }
    }

    /** Clears all cached balance state when the active wallet changes. */
    public static void invalidateOnAccountSwitch() {
        synchronized (LOCK) {
            clearCacheStateLocked();
            fetchGeneration++;
            tokenFetchInFlight = false;
            pendingContract = null;
            pendingContext = null;
            pendingPrivateKey = null;
            pendingWallet = null;
            pendingForce = false;
            pendingCompletes.clear();
        }
    }

    private static void clearCacheStateLocked() {
        cachedAtMs = 0;
        cachedWallet = null;
        cachedContract = null;
        cachedNativeBalance = "0";
        cachedTokenBalance = BigInteger.ZERO;
        nativeFromHome = false;
        tokenSnapshots.clear();
    }

    /**
     * Invalidates one token snapshot after a token write. This keeps the native side available
     * but forces the next ERC-20 balance read to refresh from chain.
     */
    public static void invalidateTokenSnapshot(String walletAddress, String contractAddress) {
        invalidateAfterWrite(walletAddress, contractAddress);
    }

    /**
     * Invalidates the wallet/contract pair after a send, wrap, unwrap, or swap so subsequent
     * screens do not keep showing stale token data.
     */
    public static void invalidateAfterWrite(String walletAddress, String contractAddress) {
        if (TextUtils.isEmpty(walletAddress)) {
            return;
        }
        synchronized (LOCK) {
            String wallet = ChainAddressUtil.normalizeAddress(walletAddress);
            String contract = contractKey(contractAddress);
            tokenSnapshots.remove(snapshotKey(wallet, contract));
            if (TextUtils.equals(cachedWallet, wallet)) {
                nativeFromHome = false;
                if (TextUtils.equals(cachedContract, contract)) {
                    cachedTokenBalance = BigInteger.ZERO;
                }
            }
        }
    }

    private static String snapshotKey(String walletAddress, String contractAddress) {
        return ChainAddressUtil.normalizeAddress(walletAddress)
                + "|"
                + contractKey(contractAddress);
    }

    private static String contractKey(String contractAddress) {
        if (TextUtils.isEmpty(contractAddress)) {
            return "";
        }
        return ChainAddressUtil.normalizeAddress(contractAddress);
    }

    private static void cacheTokenSnapshot(
            Context context,
            String wallet,
            String contract,
            String nativeBal,
            BigInteger tokenBal) {
        Snapshot snapshot = new Snapshot(wallet, contract, nativeBal, tokenBal);
        tokenSnapshots.put(snapshotKey(wallet, contract), snapshot);
        cachedWallet = wallet;
        cachedContract = contract;
        cachedNativeBalance = nativeBal;
        cachedTokenBalance = tokenBal;
        cachedAtMs = System.currentTimeMillis();
        persistSnapshotWei(context, wallet, contract, tokenBal);
    }

    private static void persistSnapshotWei(
            Context context, String wallet, String contract, BigInteger tokenBal) {
        if (context == null || tokenBal == null) {
            return;
        }
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(snapshotKey(wallet, contract), tokenBal.toString())
                .apply();
    }

    @Nullable
    private static BigInteger loadPersistedWei(
            Context context, String walletAddress, String contractAddress) {
        if (context == null || TextUtils.isEmpty(walletAddress) || TextUtils.isEmpty(contractAddress)) {
            return null;
        }
        String raw = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(snapshotKey(walletAddress, contractAddress), null);
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        try {
            return new BigInteger(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void prefetch(Context context) {
        prefetchTokenOnly(context, false, null);
    }

    public static void prefetchForce(Context context) {
        prefetchTokenOnly(context, true, null);
    }

    public static void prefetchForce(Context context, Runnable onComplete) {
        prefetchTokenOnly(context, true, onComplete);
    }

    public static void prefetchForce(Context context, Runnable onComplete, Runnable onFetchFailed) {
        prefetchTokenOnly(context, true, onComplete, onFetchFailed);
    }

    /** Prefetches the selected token snapshot while reusing any native balance already in cache. */
    public static void prefetchTokenOnly(Context context, boolean force, Runnable onComplete) {
        prefetchTokenOnly(context, force, onComplete, null);
    }

    public static void prefetchTokenOnly(
            Context context, boolean force, Runnable onComplete, Runnable onFetchFailed) {
        String contract = TokenSelection.getSelectedContract(context);
        if (TextUtils.isEmpty(contract)) {
            contract = wrappedBkcContractHelper.resolveContractAddress(context);
        }
        prefetchTokenBalance(context, contract, force, onComplete, onFetchFailed);
    }

    /** Prefetches the balance snapshot for a specific ERC-20 contract. */
    public static void prefetchTokenBalance(
            Context context, String contractAddress, boolean force, Runnable onComplete) {
        prefetchTokenBalance(context, contractAddress, force, onComplete, null);
    }

    /** Prefetches an ERC-20 snapshot and optionally reports a forced-refresh miss to the caller. */
    public static void prefetchTokenBalance(
            Context context,
            String contractAddress,
            boolean force,
            Runnable onComplete,
            Runnable onFetchFailed) {
        final String privateKey = TokenWalletHelper.getCurrentPrivateKey(context);
        String wallet = TokenWalletHelper.getWalletAddress(context);
        if (TextUtils.isEmpty(wallet) && !TextUtils.isEmpty(privateKey)) {
            wallet = SecurityUtil.GetAddress(privateKey);
        }
        if (!TextUtils.isEmpty(wallet)) {
            wallet = ChainAddressUtil.normalizeAddress(wallet);
        }
        if (TextUtils.isEmpty(wallet) || TextUtils.isEmpty(privateKey)) {
            Log.i(TAG, "prefetchTokenBalance skipped: wallet=" + wallet
                    + " privateKey=" + (TextUtils.isEmpty(privateKey) ? "missing" : "present")
                    + " contract=" + contractAddress + " force=" + force);
            dispatchComplete(onComplete);
            return;
        }
        Log.i(TAG, "prefetchTokenBalance start contract=" + contractAddress
                + " wallet=" + wallet + " force=" + force);
        prefetchTokenBalanceWithAccount(
                context, contractAddress, wallet, privateKey, force, onComplete, onFetchFailed);
    }

    private static void prefetchTokenBalanceWithAccount(
            Context context,
            String contractAddress,
            String walletFinal,
            String privateKey,
            boolean force,
            Runnable onComplete,
            Runnable onFetchFailed) {
        final String contract = contractAddress;

        final long generation;
        final boolean hadSnapshot;
        synchronized (LOCK) {
            if (!force && tokenSnapshots.containsKey(snapshotKey(walletFinal, contract))) {
                Log.i(TAG, "prefetch cache hit, skip fetch contract=" + contract);
                dispatchComplete(onComplete);
                return;
            }
            if (tokenFetchInFlight) {
                if (force) {
                    fetchGeneration++;
                    Log.i(TAG, "prefetch supersede in-flight gen=" + fetchGeneration
                            + " contract=" + contract);
                } else {
                    Log.i(TAG, "prefetch queued (in-flight) contract=" + contract + " force=" + force);
                }
                pendingContract = contractKey(contract);
                pendingContext = context.getApplicationContext();
                pendingPrivateKey = privateKey;
                pendingWallet = walletFinal;
                pendingForce = pendingForce || force;
                if (onComplete != null) {
                    pendingCompletes.add(onComplete);
                }
                return;
            }
            generation = ++fetchGeneration;
            hadSnapshot = resolveSnapshot(context, walletFinal, contract) != null;
            tokenFetchInFlight = true;
            pendingContract = null;
            pendingContext = null;
            pendingPrivateKey = null;
            pendingWallet = null;
            pendingForce = false;
        }

        final String fetchContract = contractKey(contract);
        final boolean forceRefresh = force;
        Log.i(TAG, "prefetch worker starting gen=" + generation
                + " contract=" + fetchContract + " force=" + forceRefresh);
        Thread worker = new Thread(() -> {
            boolean fetchFailed = false;
            try {
                String nativeBal = resolveNativeBalance(walletFinal, privateKey, forceRefresh);
                BigInteger tokenBal = null;
                if (TextUtils.isEmpty(fetchContract)) {
                    Log.i(TAG, "prefetch worker: empty contract, skip eth_call");
                } else {
                    try {
                        String owner = walletFinal;
                        if (TextUtils.isEmpty(owner) && !TextUtils.isEmpty(privateKey)) {
                            owner = SecurityUtil.GetAddress(privateKey);
                        }
                        Log.i(TAG, "readBalance calling eth_call contract=" + fetchContract
                                + " owner=" + owner);
                        tokenBal = TokenContractHelper.readBalance(
                                fetchContract, owner, privateKey);
                        Log.i(TAG, "readBalance result contract=" + fetchContract
                                + " => " + tokenBal);
                    } catch (Exception e) {
                        Log.i(TAG, "readBalance failed contract=" + fetchContract, e);
                    }
                }
                synchronized (LOCK) {
                    if (tokenBal != null) {
                        cacheTokenSnapshot(context, walletFinal, fetchContract, nativeBal, tokenBal);
                        Log.i(TAG, "cached token balance contract=" + fetchContract
                                + " value=" + tokenBal + " gen=" + generation);
                    } else if (generation == fetchGeneration) {
                        cachedWallet = walletFinal;
                        cachedContract = fetchContract;
                        cachedNativeBalance = nativeBal;
                        cachedAtMs = System.currentTimeMillis();
                        Log.i(TAG, "eth_call returned null, snapshot not updated contract="
                                + fetchContract);
                    } else {
                        Log.i(TAG, "stale null result ignored gen=" + generation
                                + " current=" + fetchGeneration);
                    }
                }
                fetchFailed = forceRefresh && tokenBal == null && !hadSnapshot;
            } catch (Exception e) {
                Log.w(TAG, "prefetchTokenBalance failed", e);
                fetchFailed = forceRefresh && !hadSnapshot;
            } finally {
                finishTokenBalanceFetch(context, onComplete, onFetchFailed, fetchFailed, generation);
            }
        }, "token-balance-fetch");
        worker.setDaemon(true);
        worker.start();
    }

    private static String resolveNativeBalance(String wallet, String privateKey, boolean forceRefresh) {
        synchronized (LOCK) {
            if (nativeFromHome
                    && TextUtils.equals(cachedWallet, wallet)
                    && !TextUtils.isEmpty(cachedNativeBalance)) {
                return cachedNativeBalance;
            }
        }
        if (TextUtils.isEmpty(privateKey)) {
            return "0";
        }
        try {
            return MyUtil.getNativeBalanceForPrivateKey(privateKey);
        } catch (Exception e) {
            Log.w(TAG, "GetAddrAndBalance fallback failed", e);
        }
        return "0";
    }

    private static boolean matchesAccount(String walletAddress, String contractAddress) {
        if (TextUtils.isEmpty(walletAddress) || TextUtils.isEmpty(cachedWallet)) {
            return false;
        }
        if (!TextUtils.equals(cachedWallet, ChainAddressUtil.normalizeAddress(walletAddress))) {
            return false;
        }
        if (contractAddress == null) {
            return true;
        }
        return TextUtils.equals(cachedContract, contractKey(contractAddress));
    }

    private static void finishTokenBalanceFetch(
            Context context,
            Runnable onComplete,
            Runnable onFetchFailed,
            boolean notifyFetchFailed,
            long generation) {
        List<Runnable> queued;
        String followContract;
        Context followContext;
        String followPrivateKey;
        String followWallet;
        boolean followForce;
        synchronized (LOCK) {
            tokenFetchInFlight = false;
            queued = new ArrayList<>(pendingCompletes);
            pendingCompletes.clear();
            followContract = pendingContract;
            followContext = pendingContext;
            followPrivateKey = pendingPrivateKey;
            followWallet = pendingWallet;
            followForce = pendingForce;
            pendingContract = null;
            pendingContext = null;
            pendingPrivateKey = null;
            pendingWallet = null;
            pendingForce = false;
        }
        if (notifyFetchFailed) {
            dispatchComplete(onFetchFailed);
        }
        if (!TextUtils.isEmpty(followContract) && followContext != null
                && !TextUtils.isEmpty(followPrivateKey) && !TextUtils.isEmpty(followWallet)) {
            Runnable chainComplete = () -> {
                dispatchComplete(onComplete);
                for (Runnable r : queued) {
                    dispatchComplete(r);
                }
            };
            prefetchTokenBalanceWithAccount(
                    followContext,
                    followContract,
                    followWallet,
                    followPrivateKey,
                    followForce,
                    chainComplete,
                    onFetchFailed);
        } else {
            dispatchComplete(onComplete);
            for (Runnable r : queued) {
                dispatchComplete(r);
            }
        }
    }

    private static void dispatchComplete(Runnable onComplete) {
        if (onComplete != null) {
            onComplete.run();
        }
    }
}
