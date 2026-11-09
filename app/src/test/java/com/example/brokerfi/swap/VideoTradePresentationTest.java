package com.example.brokerfi.swap;

import com.example.brokerfi.token.TokenTxRecord;
import org.junit.Test;
import static org.junit.Assert.*;

public class VideoTradePresentationTest {
    private TokenTxRecord swap() {
        TokenTxRecord r=new TokenTxRecord();r.type="SWAP";r.status="SUCCESS";
        r.amountDisplay="10";r.actualOutput="46.7915";r.fromSymbol="BKC";r.toSymbol="mUSDT";return r;
    }
    @Test public void actualOutputIsNeverReplacedByTheQuote() {
        TokenTxRecord r=swap();r.actualOutput=null;r.estimatedOutput="46.7915";
        assertEquals("10 BKC → Unverified mUSDT",VideoTradePresentation.amounts(r));
        assertNull(r.actualOutput);
    }
    @Test public void tinyConfirmedLpIsNotRenderedAsZero() {
        TokenTxRecord r=swap();r.liquidityAction="ADD";r.actualBkc="0.0005";r.actualMusdt="0.084281";r.actualLp="0.000000006490585915";
        assertEquals("0.0005 BKC + 0.084281 mUSDT → <0.000001 LPC",VideoTradePresentation.amounts(r));
        assertEquals("0.000000006490585915",r.actualLp);
    }
    @Test public void wrapUnwrapAndSwapUseTheDemoLabels() {
        TokenTxRecord r=swap();r.fromAsset="BKC";r.toAsset="SWAP_WBKC";
        assertEquals("Wrap",VideoTradePresentation.label(r));r.fromAsset="ORIGINAL_WBKC";r.toAsset="BKC";
        assertEquals("Unwrap",VideoTradePresentation.label(r));r.fromAsset="MUSDT";assertEquals("Swap",VideoTradePresentation.label(r));
    }
    @Test public void unresolvedStatesRemainExplicitInTheCompactList() {
        assertTrue(VideoTradePresentation.status("UNKNOWN").contains("Do not send again"));
        assertTrue(VideoTradePresentation.status("PENDING").contains("confirmation"));
        assertEquals("Transaction failed",VideoTradePresentation.status("FAILED"));
        assertEquals("",VideoTradePresentation.status("SUCCESS"));
    }
    @Test public void approvalDoesNotRenderAsACompletedSwap() {
        TokenTxRecord r=swap();r.type="APPROVAL";
        assertTrue(VideoTradePresentation.approval(r));assertEquals("Approve",VideoTradePresentation.label(r));
        assertEquals("10 BKC",VideoTradePresentation.amounts(r));
    }
}
