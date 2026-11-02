package com.example.brokerfi.swap;

import java.math.BigInteger;

/** Dash currently executes signed writes at 100 gwei even when eth_gasPrice reports 1 gwei.
 * Gas price cannot be selected in this gateway's request. Keep a conservative deployment-specific
 * floor and two-times gas-limit headroom. This is a reserve, not an enforceable fee cap. */
public final class SwapFeePolicy {
    public static final BigInteger GAS_LIMIT = BigInteger.valueOf(1_500_000);
    public static final BigInteger GATEWAY_PRICE_FLOOR = new BigInteger("100000000000");
    private SwapFeePolicy() {}
    public static BigInteger reserve(BigInteger rpcPrice) {
        if (rpcPrice == null || rpcPrice.signum() < 0) throw new IllegalArgumentException("Invalid gas price");
        return rpcPrice.max(GATEWAY_PRICE_FLOOR).multiply(GAS_LIMIT).multiply(BigInteger.valueOf(2));
    }
}
