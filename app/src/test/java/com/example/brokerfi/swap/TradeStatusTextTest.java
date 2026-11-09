package com.example.brokerfi.swap;

import com.example.brokerfi.token.TokenTxRecord;
import org.junit.Test;
import static org.junit.Assert.*;

public class TradeStatusTextTest {
    private TokenTxRecord unknown(){TokenTxRecord r=new TokenTxRecord();r.status="UNKNOWN";return r;}
    private TokenTxRecord pending(){TokenTxRecord r=unknown();r.status="PENDING";r.txHash="0x"+String.format("%064d",1);return r;}
    @Test public void preflightAlwaysSaysNotSent() {
        assertEquals("Checking before sending. Not sent yet.",TradeStatusText.operation(false,true,false,null));
    }
    @Test public void confirmingCannotBeMistakenForSending() {
        assertEquals("Awaiting your confirmation. Not sent yet.",TradeStatusText.operation(true,false,false,null));
    }
    @Test public void sendingWithoutHashIsNotYetReportedAsSuccessOrFailure() {
        assertEquals("Sending. Do not send again.",TradeStatusText.operation(false,false,true,unknown()));
    }
    @Test public void activeReceiptWaitIsDistinctFromSending() {
        assertEquals("Waiting for confirmation. Do not send again.",TradeStatusText.operation(false,false,true,pending()));
    }
    @Test public void inactiveValidHashOffersQueryNotResend() {
        assertTrue(TradeStatusText.operation(false,false,false,pending()).contains("check the original transaction (no resend)"));
    }
    @Test public void inactiveHashlessUnknownOffersRecoveryNotFailure() {
        String text=TradeStatusText.operation(false,false,false,unknown());
        assertTrue(text.contains("Outcome unknown"));assertTrue(text.contains("No automatic resend"));assertFalse(text.contains("failed"));
    }
    @Test public void idleHasNoOperationBanner() { assertEquals("",TradeStatusText.operation(false,false,false,null)); }
}
