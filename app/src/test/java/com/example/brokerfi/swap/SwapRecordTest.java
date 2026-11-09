package com.example.brokerfi.swap;

import com.example.brokerfi.token.TokenTxHistoryStore;
import com.example.brokerfi.token.TokenTxRecord;
import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.*;

public class SwapRecordTest {
    @Test public void legacyJsonStillHasNoNewStatus() {
        TokenTxRecord r=new Gson().fromJson("{\"type\":\"WRAP\",\"amountDisplay\":\"1\"}",TokenTxRecord.class);
        assertNull(r.status); assertFalse(TokenTxHistoryStore.isUnresolvedSwap(r));
    }
    @Test public void submittingPendingAndUnknownAllLockWallet() {
        TokenTxRecord r=new TokenTxRecord();
        for(String state:new String[]{"SUBMITTING","PENDING","UNKNOWN"}) {
            r.status=state; assertTrue(TokenTxHistoryStore.isUnresolvedSwap(r));
        }
        for(String state:new String[]{"SUCCESS","FAILED","NOT_SENT","ACKNOWLEDGED"}) {
            r.status=state; assertFalse(TokenTxHistoryStore.isUnresolvedSwap(r));
        }
    }
    @Test public void quotedAndActualAmountsRoundTripSeparately() {
        TokenTxRecord r=new TokenTxRecord(); r.estimatedOutput="0.168069"; r.minimumOutput="0.167228";
        r.actualOutput="0.168071"; r.status="SUCCESS"; r.operationId="example";
        TokenTxRecord saved=new Gson().fromJson(new Gson().toJson(r),TokenTxRecord.class);
        assertEquals("0.168069",saved.estimatedOutput); assertEquals("0.168071",saved.actualOutput);
        assertEquals("0.167228",saved.minimumOutput);
    }
    @Test public void confirmedWithoutEventIsExplicitlyUnverified() {
        TokenTxRecord r=new TokenTxRecord(); r.status="SUCCESS"; r.estimatedOutput="1"; r.minimumOutput="0.99";
        assertTrue(SwapRecordPresentation.details(r).contains("Unverified (not replaced with an estimate)"));
    }
}
