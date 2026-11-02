package com.example.brokerfi.swap;

import org.junit.Test;
import java.math.BigInteger;
import static org.junit.Assert.*;

public class TradeQuoteRequestTest {
    @Test public void refreshOnlyRemembersAnIntentAndConsumesItOnce() {
        TradeQuoteRequest request=new TradeQuoteRequest();
        assertFalse(request.pending()); request.request("SWAP"); assertTrue(request.pending());
        assertEquals("SWAP",request.take()); assertNull(request.take()); assertFalse(request.pending());
    }
    @Test public void editingOrLeavingCancelsThePendingConfirmation() {
        TradeQuoteRequest request=new TradeQuoteRequest();request.request("ADD");request.cancel();
        assertNull(request.take());assertFalse(request.pending());
    }
    @Test public void latestIntentWinsAndPanelsDoNotShareRequests() {
        TradeQuoteRequest swap=new TradeQuoteRequest(),lp=new TradeQuoteRequest();
        swap.request("SWAP");lp.request("ADD");lp.request("REMOVE");
        assertEquals("REMOVE",lp.take());assertEquals("SWAP",swap.take());
    }
    @Test public void autoRefreshWaitsAfterTheLastSuccessfulCompletion() {
        assertTrue(TradeQuoteRequest.autoRefreshDue(1,0));
        assertFalse(TradeQuoteRequest.autoRefreshDue(29_999,1));
        assertTrue(TradeQuoteRequest.autoRefreshDue(30_001,1));
    }
    @Test public void oldReservesCanBeDisplayedButNeverUsedAsAFinancialQuote() {
        SwapPoolSnapshot pool=new SwapPoolSnapshot(new BigInteger("1000000000"),new BigInteger("10000000000000000000"),100);
        BigInteger input=new BigInteger("100000000000000");
        SwapQuoteMath.Quote reference=SwapQuoteMath.reference(pool,SwapAsset.BKC,SwapAsset.MUSDT,input,50);
        assertTrue(reference.output.signum()>0);
        try { SwapQuoteMath.quote(pool,SwapAsset.BKC,SwapAsset.MUSDT,input,50,45_101);fail(); }
        catch(IllegalArgumentException expected) { }
        assertEquals(reference.output,SwapQuoteMath.quote(pool,SwapAsset.BKC,SwapAsset.MUSDT,input,50,45_100).output);
    }
    @Test public void referenceStillRejectsInvalidAssetsAndSlippage() {
        SwapPoolSnapshot pool=new SwapPoolSnapshot(BigInteger.TEN,BigInteger.TEN,1);
        try { SwapQuoteMath.reference(pool,SwapAsset.ORIGINAL_WBKC,SwapAsset.MUSDT,BigInteger.ONE,50);fail(); }
        catch(IllegalArgumentException expected) { }
        try { SwapQuoteMath.reference(pool,SwapAsset.BKC,SwapAsset.MUSDT,BigInteger.ONE,9999);fail(); }
        catch(IllegalArgumentException expected) { }
    }
}
