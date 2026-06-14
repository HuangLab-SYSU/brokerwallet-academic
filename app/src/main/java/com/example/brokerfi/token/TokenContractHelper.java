package com.example.brokerfi.token;

import android.text.TextUtils;
import android.util.Log;

import com.example.brokerfi.core.blockchain.ChainAddressUtil;
import com.example.brokerfi.core.blockchain.DashEthCall;
import com.example.brokerfi.core.config.ChainConfig;
import com.example.brokerfi.core.security.SecurityUtil;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Generic ERC-20 chain reads and encoders (BrokerChain). */
public final class TokenContractHelper {

    private static final String TAG = "TokenContractHelper";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(String message);
    }

    private TokenContractHelper() {
    }

    public static <T> void runAsync(Callable<T> task, Callback<T> callback) {
        runAsyncWithTimeout(TokenConfig.DEFAULT_CHAIN_READ_TIMEOUT_MS, task, callback);
    }

    public static <T> void runAsyncWithTimeout(long timeoutMs, Callable<T> task, Callback<T> callback) {
        Thread waiter = new Thread(() -> {
            Future<T> future = EXECUTOR.submit(task);
            try {
                T value = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                callback.onSuccess(value);
            } catch (TimeoutException e) {
                future.cancel(true);
                callback.onError("chain_read_timeout");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                callback.onError(cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.cancel(true);
                callback.onError("interrupted");
            }
        }, "token-read");
        waiter.setDaemon(true);
        waiter.start();
    }

    public static BigInteger readBalance(String contractAddress, String owner, String privateKey) throws Exception {
        if (TextUtils.isEmpty(privateKey)) {
            Log.i(TAG, "readBalance skipped: privateKey required for dash eth_call");
            return null;
        }
        String ownerAddr = !TextUtils.isEmpty(owner)
                ? ChainAddressUtil.normalizeAddress(owner)
                : ChainAddressUtil.normalizeAddress(SecurityUtil.GetAddress(privateKey));
        if (TextUtils.isEmpty(ownerAddr)) {
            Log.i(TAG, "readBalance skipped: owner address empty");
            return null;
        }
        Function function = new Function(
                "balanceOf",
                Collections.singletonList(new Address(ownerAddr)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
        String data = FunctionEncoder.encode(function);
        String resultHex = queryBalanceRead(contractAddress, data, privateKey);
        if (TextUtils.isEmpty(resultHex)) {
            return null;
        }
        List<Type> decoded = FunctionReturnDecoder.decode(resultHex, function.getOutputParameters());
        if (decoded.isEmpty() || decoded.get(0) == null) {
            return null;
        }
        return (BigInteger) decoded.get(0).getValue();
    }

    /**
     * Reads an ERC-20 balance with the signed dash {@code eth_call} path first, then falls back
     * to the plain JSON-RPC endpoint. Signed calls are retried up to
     * {@link TokenConfig#BALANCE_READ_MAX_ATTEMPTS} times.
     */
    private static String queryBalanceRead(String contractAddress, String dataHex, String privateKey)
            throws Exception {
        Exception lastError = null;
        int attempts = TokenConfig.BALANCE_READ_MAX_ATTEMPTS;
        for (int i = 0; i < attempts; i++) {
            try {
                String result = DashEthCall.signedCall(
                        contractAddress,
                        dataHex,
                        privateKey,
                        TokenConfig.BALANCE_ETH_CALL_TIMEOUT_MS);
                if (!TextUtils.isEmpty(result)) {
                    return result;
                }
            } catch (Exception e) {
                lastError = e;
                Log.w(TAG, "signed balanceOf attempt " + (i + 1) + " failed", e);
            }
        }
        try {
            String to = ChainAddressUtil.normalizeAddress(contractAddress);
            String result = DashEthCall.directEthCall(ChainConfig.CHAIN_JSON_RPC_URL, to, dataHex);
            if (!TextUtils.isEmpty(result)) {
                Log.i(TAG, "balanceOf via RPC fallback succeeded");
                return result;
            }
        } catch (Exception e) {
            lastError = e;
            Log.w(TAG, "RPC balanceOf fallback failed", e);
        }
        if (lastError != null) {
            throw lastError;
        }
        return null;
    }

    public static String encodeTransfer(String toAddress, BigInteger amount) {
        String to = ChainAddressUtil.normalizeAddress(toAddress);
        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(to), new Uint256(amount)),
                Collections.emptyList());
        return FunctionEncoder.encode(function);
    }

    public static String toValueHex(BigInteger weiAmount) {
        if (weiAmount == null || weiAmount.signum() <= 0) {
            return "0x0";
        }
        return "0x" + weiAmount.toString(16);
    }

    public static String queryViaSignedEthCall(String contractAddress, String dataHex, String privateKey)
            throws Exception {
        return DashEthCall.signedCall(contractAddress, dataHex, privateKey);
    }

    /** Executes a generic signed dash {@code eth_call} for ERC-20 metadata or other read-only calls. */
    public static String queryChainRead(String contractAddress, String dataHex, String privateKey)
            throws Exception {
        if (TextUtils.isEmpty(privateKey)) {
            throw new IllegalArgumentException("privateKey required for dash eth_call");
        }
        String to = ChainAddressUtil.normalizeAddress(contractAddress);
        String data = dataHex.startsWith("0x") || dataHex.startsWith("0X") ? dataHex : "0x" + dataHex;
        return DashEthCall.signedCall(to, data, privateKey);
    }
}
