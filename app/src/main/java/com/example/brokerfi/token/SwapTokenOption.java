package com.example.brokerfi.token;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import com.example.brokerfi.token.wrappedbkc.wrappedBkcConfig;
import android.content.Context;
import android.text.TextUtils;

import com.example.brokerfi.token.TokenConfig;

/** Swap-side token choice: native BKC or an enabled ERC-20 token. */
public final class SwapTokenOption {

    public static final String NATIVE_KEY = "native_bkc";

    private final boolean nativeBkc;
    private final TokenItem token;

    private SwapTokenOption(boolean nativeBkc, TokenItem token) {
        this.nativeBkc = nativeBkc;
        this.token = token;
    }

    public static SwapTokenOption nativeBkc() {
        return new SwapTokenOption(true, null);
    }

    public static SwapTokenOption fromToken(TokenItem item) {
        return new SwapTokenOption(false, item);
    }

    public boolean isNativeBkc() {
        return nativeBkc;
    }

    public TokenItem getToken() {
        return token;
    }

    public String getSymbol() {
        if (nativeBkc) {
            return TokenConfig.NATIVE_SYMBOL;
        }
        return token != null && !TextUtils.isEmpty(token.getSymbol())
                ? token.getSymbol()
                : wrappedBkcConfig.SYMBOL;
    }

    public String getName() {
        if (nativeBkc) {
            return TokenConfig.NATIVE_SYMBOL;
        }
        return token != null && !TextUtils.isEmpty(token.getName())
                ? token.getName()
                : getSymbol();
    }

    public String getContractAddress() {
        if (nativeBkc || token == null) {
            return "";
        }
        return token.getContractAddress();
    }

    public boolean isBuiltInwrappedBkc() {
        return !nativeBkc && token != null && token.isBuiltIn();
    }

    /** Returns true when this option points to the official wrapped BKC contract. */
    public boolean isOfficialwrappedBkc(Context context) {
        if (nativeBkc || token == null || context == null) {
            return false;
        }
        String official = wrappedBkcContractHelper.resolveContractAddress(context);
        return token.matchesOfficialwrappedBkc(official);
    }

    public int getDecimals() {
        if (nativeBkc) {
            return TokenConfig.TOKEN_DECIMALS;
        }
        return token != null ? token.getDecimals() : TokenConfig.TOKEN_DECIMALS;
    }

    public String selectionKey() {
        return nativeBkc ? NATIVE_KEY : getContractAddress();
    }

    public static boolean sameOption(SwapTokenOption a, SwapTokenOption b) {
        if (a == null || b == null) {
            return false;
        }
        return a.selectionKey().equals(b.selectionKey());
    }
}
