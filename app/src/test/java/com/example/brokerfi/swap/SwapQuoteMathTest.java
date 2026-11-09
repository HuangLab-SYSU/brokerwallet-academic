package com.example.brokerfi.swap;

import org.junit.Test;
import java.math.BigInteger;
import static org.junit.Assert.*;

public class SwapQuoteMathTest {
    private final SwapPoolSnapshot pool = new SwapPoolSnapshot(new BigInteger("1845712407"),
            new BigInteger("10947869402511077675"), 1000);
    @Test public void matchesVerifiedOnChainQuoteAndMinimum() {
        SwapQuoteMath.Quote quote = SwapQuoteMath.quote(pool, SwapAsset.BKC, SwapAsset.MUSDT,
                SwapQuoteMath.parseUnits("0.001", 18), 50, 1001);
        assertEquals(new BigInteger("168069"), quote.output);
        assertEquals(new BigInteger("167228"), quote.minimum);
        assertEquals("0.168069", SwapQuoteMath.format(quote.output, 6));
        assertFalse(quote.blocked());
    }
    @Test public void wrappedAndNativeUseIdenticalPoolInput() {
        BigInteger amount = SwapQuoteMath.parseUnits("0.001", 18);
        assertEquals(SwapQuoteMath.quote(pool, SwapAsset.BKC, SwapAsset.MUSDT, amount, 50, 1001).output,
                SwapQuoteMath.quote(pool, SwapAsset.SWAP_WBKC, SwapAsset.MUSDT, amount, 50, 1001).output);
    }
    @Test public void originalWrappedIsNotTheProtocolAsset() {
        assertFalse(SwapAsset.isAmm(SwapAsset.ORIGINAL_WBKC, SwapAsset.MUSDT));
        assertTrue(SwapAsset.isWrap(SwapAsset.BKC, SwapAsset.ORIGINAL_WBKC));
        assertTrue(SwapAsset.isUnwrap(SwapAsset.SWAP_WBKC, SwapAsset.BKC));
    }
    @Test public void allFourAmmDirectionsAreExplicitlyWhitelisted() {
        int routes = 0;
        for (SwapAsset a : SwapAsset.values()) for (SwapAsset b : SwapAsset.values())
            if (SwapAsset.isAmm(a, b)) routes++;
        assertEquals(4, routes);
    }
    @Test(expected = IllegalArgumentException.class) public void refusesStalePool() {
        SwapQuoteMath.quote(pool, SwapAsset.BKC, SwapAsset.MUSDT, BigInteger.TEN, 50, 46_001);
    }
    @Test public void backwardsClockCannotMakeSnapshotFresh() { assertFalse(pool.isFresh(999)); }
    @Test(expected = ArithmeticException.class) public void refusesExtraPrecision() {
        SwapQuoteMath.parseUnits("0.0000001", 6);
    }
    @Test(expected = IllegalArgumentException.class) public void refusesExponentInput() {
        SwapQuoteMath.parseUnits("1e5", 6);
    }
    @Test(expected = IllegalArgumentException.class) public void refusesDustOutput() {
        SwapQuoteMath.quote(pool, SwapAsset.BKC, SwapAsset.MUSDT, BigInteger.ONE, 50, 1001);
    }
    @Test public void excessivePriceImpactBlocksTrade() {
        assertTrue(SwapQuoteMath.quote(pool, SwapAsset.BKC, SwapAsset.MUSDT,
                SwapQuoteMath.parseUnits("10", 18), 50, 1001).blocked());
    }
}
