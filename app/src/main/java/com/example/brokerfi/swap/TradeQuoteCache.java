package com.example.brokerfi.swap;

import android.content.Context;
import android.content.SharedPreferences;

import java.math.BigInteger;

final class TradeQuoteCache {
    private final SharedPreferences preferences;
    private final String poolKey, liquidityKey;

    TradeQuoteCache(Context context, BrokerSwapConfig config) {
        preferences = context.getSharedPreferences("trade_quotes_v1", Context.MODE_PRIVATE);
        poolKey = config.chainId() + "_" + config.factory() + "_" + config.pair() + "_" + config.musdt() + "_" + config.wbkc();
        liquidityKey = poolKey + "_lp";
    }

    SwapPoolSnapshot loadPool() {
        String[] fields = read(poolKey, 3);
        if (fields == null) return null;
        try {
            return decodePool(fields);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    LiquidityQuote loadLiquidity() {
        String[] fields = read(liquidityKey, 4);
        if (fields == null) return null;
        try {
            return new LiquidityQuote(decodePool(fields), new BigInteger(fields[3]));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    void savePool(SwapPoolSnapshot pool) {
        preferences.edit().putString(poolKey, encodePool(pool)).apply();
    }

    void saveLiquidity(LiquidityQuote quote) {
        String pool = encodePool(quote.pool);
        preferences.edit().putString(poolKey, pool).putString(liquidityKey, pool + ":" + quote.supply).apply();
    }

    private String[] read(String key, int count) {
        String[] fields = preferences.getString(key, "").split(":", -1);
        return fields.length == count ? fields : null;
    }

    private static String encodePool(SwapPoolSnapshot pool) {
        return pool.musdtReserve + ":" + pool.wbkcReserve + ":" + pool.updatedAtMs;
    }

    private static SwapPoolSnapshot decodePool(String[] fields) {
        long updatedAtMs = Long.parseLong(fields[2]);
        if (updatedAtMs <= 0) throw new IllegalArgumentException("Missing quote timestamp");
        return new SwapPoolSnapshot(new BigInteger(fields[0]), new BigInteger(fields[1]), -1, updatedAtMs);
    }

    static final class LiquidityQuote {
        final SwapPoolSnapshot pool;
        final BigInteger supply;

        LiquidityQuote(SwapPoolSnapshot pool, BigInteger supply) {
            if (supply.signum() <= 0) throw new IllegalArgumentException("Invalid LP supply");
            this.pool = pool;
            this.supply = supply;
        }
    }
}
