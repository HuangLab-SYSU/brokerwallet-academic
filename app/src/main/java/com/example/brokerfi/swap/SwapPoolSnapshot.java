package com.example.brokerfi.swap;

import java.math.BigInteger;

/** Reserves have been mapped using actual token0/token1. Age uses monotonic time, not chain timestamps. */
public final class SwapPoolSnapshot {
    public static final long MAX_AGE_MS = 45_000L;
    public final BigInteger musdtReserve, wbkcReserve;
    public final long capturedAtMs;
    public SwapPoolSnapshot(BigInteger musdt, BigInteger wbkc, long capturedAtMs) {
        if (musdt == null || wbkc == null || musdt.signum() <= 0 || wbkc.signum() <= 0)
            throw new IllegalArgumentException("Pool has no usable liquidity");
        this.musdtReserve = musdt;
        this.wbkcReserve = wbkc;
        this.capturedAtMs = capturedAtMs;
    }
    public boolean isFresh(long now) { return now >= capturedAtMs && now - capturedAtMs <= MAX_AGE_MS; }
}
