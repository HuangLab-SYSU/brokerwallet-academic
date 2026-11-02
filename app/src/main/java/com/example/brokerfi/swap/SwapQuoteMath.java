package com.example.brokerfi.swap;

import java.math.BigDecimal;
import java.math.BigInteger;

/** Same integer arithmetic as MiniV2, including the 997/1000 input fee. No doubles or silent rounding. */
public final class SwapQuoteMath {
    private static final BigInteger BPS = BigInteger.valueOf(10_000);
    private SwapQuoteMath() {}
    public static BigInteger parseUnits(String text, int decimals) {
        if (text == null || !text.trim().matches("[0-9]+(\\.[0-9]+)?"))
            throw new IllegalArgumentException("Enter a valid amount");
        BigInteger units = new BigDecimal(text.trim()).movePointRight(decimals).toBigIntegerExact();
        if (units.signum() <= 0 || units.bitLength() > 256) throw new IllegalArgumentException("Invalid amount");
        return units;
    }
    public static String format(BigInteger units, int decimals) {
        return new BigDecimal(units).movePointLeft(decimals).stripTrailingZeros().toPlainString();
    }
    public static BigInteger amountOut(BigInteger input, BigInteger reserveIn, BigInteger reserveOut) {
        if (input == null || input.signum() <= 0 || reserveIn.signum() <= 0 || reserveOut.signum() <= 0)
            throw new IllegalArgumentException("Invalid amount or empty pool");
        BigInteger feeInput = input.multiply(BigInteger.valueOf(997));
        BigInteger result = feeInput.multiply(reserveOut)
                .divide(reserveIn.multiply(BigInteger.valueOf(1000)).add(feeInput));
        if (result.signum() <= 0) throw new IllegalArgumentException("Amount too small to exchange");
        return result;
    }
    public static Quote quote(SwapPoolSnapshot pool, SwapAsset from, SwapAsset to,
                              BigInteger input, int slippageBps, long now) {
        if (!SwapAsset.isAmm(from, to)) throw new IllegalArgumentException("Unsupported pool route");
        if (pool == null || !pool.isFresh(now)) throw new IllegalArgumentException("Quote expired; refresh the pool");
        return reference(pool, from, to, input, slippageBps);
    }
    /** Display-only calculation, including stale reserves. Financial code must call quote(). */
    public static Quote reference(SwapPoolSnapshot pool, SwapAsset from, SwapAsset to,
                                  BigInteger input, int slippageBps) {
        if (pool == null || !SwapAsset.isAmm(from, to)) throw new IllegalArgumentException("No pool route");
        if (slippageBps != 50 && slippageBps != 100 && slippageBps != 200 && slippageBps != 500)
            throw new IllegalArgumentException("Unsupported slippage");
        BigInteger reserveIn = from == SwapAsset.MUSDT ? pool.musdtReserve : pool.wbkcReserve;
        BigInteger reserveOut = from == SwapAsset.MUSDT ? pool.wbkcReserve : pool.musdtReserve;
        BigInteger output = amountOut(input, reserveIn, reserveOut);
        BigInteger minimum = output.multiply(BPS.subtract(BigInteger.valueOf(slippageBps))).divide(BPS);
        if (minimum.signum() <= 0) throw new IllegalArgumentException("Minimum received rounds to zero");
        BigInteger feeInput = input.multiply(BigInteger.valueOf(997));
        // Price-curve impact excludes the separately displayed 0.3% exchange fee; round up conservatively.
        BigInteger denominator = reserveIn.multiply(BigInteger.valueOf(1000)).add(feeInput);
        int impact = feeInput.multiply(BPS).add(denominator.subtract(BigInteger.ONE)).divide(denominator).intValue();
        return new Quote(pool, from, to, input, output, minimum, slippageBps, impact);
    }
    public static final class Quote {
        public final SwapPoolSnapshot pool;
        public final SwapAsset from, to;
        public final BigInteger input, output, minimum;
        public final int slippageBps, impactBps;
        Quote(SwapPoolSnapshot pool, SwapAsset from, SwapAsset to, BigInteger in, BigInteger out,
              BigInteger min, int slip, int impact) {
            this.pool = pool; this.from = from; this.to = to; input = in; output = out;
            minimum = min; slippageBps = slip; impactBps = impact;
        }
        public boolean blocked() { return impactBps >= 1000; }
    }
}
