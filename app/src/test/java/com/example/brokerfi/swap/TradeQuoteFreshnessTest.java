package com.example.brokerfi.swap;

import org.junit.Test;
import java.math.BigInteger;
import static org.junit.Assert.*;

public class TradeQuoteFreshnessTest {
    @Test public void oldReservesCanSetConfirmationLimitsButCannotPassPreflight() {
        SwapPoolSnapshot pool=new SwapPoolSnapshot(new BigInteger("1000000000"),new BigInteger("10000000000000000000"),100);
        BigInteger input=new BigInteger("100000000000000");
        SwapQuoteMath.Quote reference=SwapQuoteMath.reference(pool,SwapAsset.BKC,SwapAsset.MUSDT,input,50);
        assertTrue(reference.output.signum()>0);
        try { SwapQuoteMath.quote(pool,SwapAsset.BKC,SwapAsset.MUSDT,input,50,45_101);fail(); }
        catch(IllegalArgumentException expected) { }
        assertEquals(reference.output,SwapQuoteMath.quote(pool,SwapAsset.BKC,SwapAsset.MUSDT,input,50,45_100).output);
    }
    @Test public void cachedWallClockTimestampNeverMakesAQuoteFreshForPreflight() {
        SwapPoolSnapshot pool=new SwapPoolSnapshot(BigInteger.valueOf(1_000_000),BigInteger.TEN,-1,1_700_000_000_000L);
        assertFalse(pool.isFresh(100));
        assertThrows(IllegalArgumentException.class,
                () -> SwapQuoteMath.quote(pool,SwapAsset.BKC,SwapAsset.MUSDT,BigInteger.ONE,50,100));
        assertTrue(SwapQuoteMath.reference(pool,SwapAsset.BKC,SwapAsset.MUSDT,BigInteger.ONE,50).minimum.signum()>0);
    }
    @Test public void referenceStillRejectsInvalidAssetsAndSlippage() {
        SwapPoolSnapshot pool=new SwapPoolSnapshot(BigInteger.TEN,BigInteger.TEN,1);
        try { SwapQuoteMath.reference(pool,SwapAsset.ORIGINAL_WBKC,SwapAsset.MUSDT,BigInteger.ONE,50);fail(); }
        catch(IllegalArgumentException expected) { }
        try { SwapQuoteMath.reference(pool,SwapAsset.BKC,SwapAsset.MUSDT,BigInteger.ONE,9999);fail(); }
        catch(IllegalArgumentException expected) { }
    }
}
