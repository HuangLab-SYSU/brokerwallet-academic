package com.example.brokerfi.token;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import com.example.brokerfi.token.wrappedbkc.wrappedBkcConfig;
import android.content.Context;
import android.text.TextUtils;

import com.example.brokerfi.token.TokenConfig;

/** 鍏戞崲椤靛彲閫夎祫浜э細鍘熺敓 BKC 鎴栧凡鍚敤 ERC-20銆?*/
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

    /** 鏄惁涓哄畼鏂?wBKC 鍚堢害锛堟寜鍦板潃姣斿锛岄槻姝㈠亣 wBKC 鍙備笌鍖呰鍏戞崲锛夈€?*/
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


