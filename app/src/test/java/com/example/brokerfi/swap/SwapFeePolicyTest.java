package com.example.brokerfi.swap;

import org.junit.Test;
import java.math.BigInteger;
import static org.junit.Assert.*;

public class SwapFeePolicyTest {
    @Test public void lowRpcPriceDoesNotUnderReserveSignedGatewayFees() {
        assertEquals(new BigInteger("300000000000000000"),SwapFeePolicy.reserve(BigInteger.valueOf(1_000_000_000)));
    }
    @Test public void higherRpcPriceIncreasesReserve() {
        assertEquals(new BigInteger("600000000000000000"),SwapFeePolicy.reserve(new BigInteger("200000000000")));
    }
    @Test public void observedSwapFeeFitsConservativeReserve() {
        BigInteger observed = BigInteger.valueOf(78657).multiply(SwapFeePolicy.GATEWAY_PRICE_FLOOR);
        assertEquals("0.0078657",SwapQuoteMath.format(observed,18));
        assertTrue(SwapFeePolicy.reserve(BigInteger.ONE).compareTo(observed)>0);
    }
    @Test(expected=IllegalArgumentException.class) public void negativeRpcPriceRejected() { SwapFeePolicy.reserve(BigInteger.valueOf(-1)); }
}
