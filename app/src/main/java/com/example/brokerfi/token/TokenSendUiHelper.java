package com.example.brokerfi.token;

import androidx.annotation.Nullable;

import java.math.BigInteger;

final class TokenSendUiHelper {

    private static final String AVAILABLE_SUFFIX = " available";

    private TokenSendUiHelper() {
    }

    static String formatAvailableBalance(@Nullable BigInteger balance, String symbol, int decimals) {
        return TokenAmountUtil.formatDisplayAmount(balance, decimals)
                + " "
                + symbol
                + AVAILABLE_SUFFIX;
    }

    static boolean isInsufficientFunds(@Nullable BigInteger balance, @Nullable BigInteger amount) {
        if (balance == null || amount == null) {
            return false;
        }
        return balance.compareTo(amount) < 0;
    }
}


