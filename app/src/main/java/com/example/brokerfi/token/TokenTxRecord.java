package com.example.brokerfi.token;

import android.text.TextUtils;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcConfig;
import com.example.brokerfi.core.blockchain.ChainAddressUtil;

import java.io.Serializable;

/** Local token transaction record for swap, send, receive, and related actions. */
public class TokenTxRecord implements Serializable {

    public static final String TYPE_SWAP = "SWAP";
    /** @deprecated legacy type kept for display compatibility with {@link #TYPE_SWAP}. */
    public static final String TYPE_WRAP = "WRAP";
    /** @deprecated legacy type kept for display compatibility with {@link #TYPE_SWAP}. */
    public static final String TYPE_UNWRAP = "UNWRAP";
    public static final String TYPE_SEND = "SEND";
    public static final String TYPE_RECEIVE = "RECEIVE";

    public String type;
    public String amountDisplay;
    public String detail;
    public String txHash;
    public long timestampMs;
    /** Contract for this transaction; old records default to the wrapped BKC contract. */
    public String contractAddress;
    /** Paying-side symbol for swap-style records. */
    public String fromSymbol;
    /** Receiving-side symbol for swap-style records. */
    public String toSymbol;

    public TokenTxRecord() {
    }

    public TokenTxRecord(String type, String amountDisplay, String detail, String txHash, long timestampMs) {
        this.type = type;
        this.amountDisplay = amountDisplay;
        this.detail = detail;
        this.txHash = txHash;
        this.timestampMs = timestampMs;
    }

    public static boolean isSwapType(String type) {
        return TYPE_SWAP.equals(type)
                || TYPE_WRAP.equals(type)
                || TYPE_UNWRAP.equals(type);
    }

    public boolean isSwap() {
        return isSwapType(type);
    }

    public String resolveFromSymbol() {
        if (!TextUtils.isEmpty(fromSymbol)) {
            return normalizeWrappedBkcSymbol(fromSymbol);
        }
        if (TYPE_WRAP.equals(type)) {
            return TokenConfig.NATIVE_SYMBOL;
        }
        if (TYPE_UNWRAP.equals(type)) {
            return wrappedBkcConfig.SYMBOL;
        }
        return "";
    }

    public String resolveToSymbol() {
        if (!TextUtils.isEmpty(toSymbol)) {
            return normalizeWrappedBkcSymbol(toSymbol);
        }
        if (TYPE_WRAP.equals(type)) {
            return wrappedBkcConfig.SYMBOL;
        }
        if (TYPE_UNWRAP.equals(type)) {
            return TokenConfig.NATIVE_SYMBOL;
        }
        return "";
    }

    public static TokenTxRecord swap(
            String amountDisplay,
            String fromSymbol,
            String toSymbol,
            String txHash,
            String contractAddress) {
        TokenTxRecord record = new TokenTxRecord(
                TYPE_SWAP,
                amountDisplay,
                null,
                txHash,
                System.currentTimeMillis());
        record.fromSymbol = fromSymbol;
        record.toSymbol = toSymbol;
        record.contractAddress = ChainAddressUtil.normalizeAddress(contractAddress);
        return record;
    }

    public static TokenTxRecord send(String amountDisplay, String toAddress, String txHash, String contractAddress) {
        TokenTxRecord record = new TokenTxRecord(
                TYPE_SEND,
                amountDisplay,
                toAddress,
                txHash,
                System.currentTimeMillis());
        record.contractAddress = ChainAddressUtil.normalizeAddress(contractAddress);
        return record;
    }

    public static TokenTxRecord receive(
            String amountDisplay,
            String fromAddress,
            String txHash,
            String contractAddress,
            long timestampMs) {
        TokenTxRecord record = new TokenTxRecord(
                TYPE_RECEIVE,
                amountDisplay,
                fromAddress,
                txHash,
                timestampMs > 0 ? timestampMs : System.currentTimeMillis());
        record.contractAddress = ChainAddressUtil.normalizeAddress(contractAddress);
        return record;
    }

    private static String normalizeWrappedBkcSymbol(String symbol) {
        if (wrappedBkcConfig.SYMBOL.equalsIgnoreCase(symbol)) {
            return wrappedBkcConfig.SYMBOL;
        }
        return symbol;
    }
}
