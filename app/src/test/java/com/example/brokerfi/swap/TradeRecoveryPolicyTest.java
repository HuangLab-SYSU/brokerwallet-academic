package com.example.brokerfi.swap;

import com.example.brokerfi.token.TokenTxHistoryStore;
import com.example.brokerfi.token.TokenTxRecord;
import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.*;

public class TradeRecoveryPolicyTest {
    private TokenTxRecord unknown(){TokenTxRecord r=new TokenTxRecord();r.status="UNKNOWN";r.operationId="old";return r;}
    @Test public void unknownBlocksUntilExplicitRiskAcceptance(){
        TokenTxRecord r=unknown();assertTrue(TradeRecoveryPolicy.blocks(r));assertFalse(TradeRecoveryPolicy.deferred(r));
    }
    @Test public void acceptingRiskDoesNotInventFailureOrCancelRequest(){
        TokenTxRecord r=unknown();assertTrue(TradeRecoveryPolicy.acceptRisk(r,false,123));
        assertEquals("UNKNOWN",r.status);assertNull(r.txHash);assertEquals("old",r.operationId);
        assertTrue(TokenTxHistoryStore.isUnresolvedSwap(r));assertFalse(TokenTxHistoryStore.isBlockingSwap(r));
        assertTrue(TradeRecoveryPolicy.details(r).contains("not canceled or resent"));
    }
    @Test public void activeOperationCannotBeDeferred(){
        TokenTxRecord r=unknown();assertFalse(TradeRecoveryPolicy.acceptRisk(r,true,123));assertTrue(TradeRecoveryPolicy.blocks(r));
    }
    @Test public void hashlessCrashCanBeReviewedButStillRemainsUnresolved(){
        TokenTxRecord r=unknown();r.status="SUBMITTING";
        assertTrue(TradeRecoveryPolicy.acceptRisk(r,false,123));assertTrue(TokenTxHistoryStore.isUnresolvedSwap(r));
        assertEquals("SUBMITTING",r.status);assertFalse(TradeRecoveryPolicy.blocks(r));
    }
    @Test public void validHashIsAlwaysQueriedNotDeferred(){
        TokenTxRecord r=unknown();r.txHash="0x"+String.format("%064d",1);
        assertFalse(TradeRecoveryPolicy.acceptRisk(r,false,123));assertTrue(TradeRecoveryPolicy.blocks(r));
        r.status="PENDING";assertFalse(TradeRecoveryPolicy.acceptRisk(r,false,123));
    }
    @Test public void laterHashRestoresPendingProtection(){
        TokenTxRecord r=unknown();assertTrue(TradeRecoveryPolicy.acceptRisk(r,false,123));
        r.txHash="0x"+String.format("%064d",1);r.status="PENDING";assertTrue(TradeRecoveryPolicy.blocks(r));
    }
    @Test public void completedAndNotSentNeverRequireRiskAcknowledgement(){
        for(String status:new String[]{"SUCCESS","FAILED","NOT_SENT","ACKNOWLEDGED"}){
            TokenTxRecord r=unknown();r.status=status;assertFalse(TradeRecoveryPolicy.blocks(r));
            assertFalse(TradeRecoveryPolicy.acceptRisk(r,false,123));
        }
    }
    @Test public void riskDecisionSurvivesRestartAndEachNewRecordRequiresOwnDecision(){
        TokenTxRecord r=unknown();TradeRecoveryPolicy.acceptRisk(r,false,123);
        TokenTxRecord saved=new Gson().fromJson(new Gson().toJson(r),TokenTxRecord.class);
        assertEquals("UNKNOWN",saved.status);assertFalse(TradeRecoveryPolicy.blocks(saved));
        assertTrue(TradeRecoveryPolicy.blocks(unknown()));
    }
    @Test public void legacyJsonDoesNotSilentlyAcceptRisk(){
        TokenTxRecord r=new Gson().fromJson("{\"status\":\"UNKNOWN\"}",TokenTxRecord.class);
        assertEquals(0,r.continuationRiskAcceptedMs);assertTrue(TradeRecoveryPolicy.blocks(r));
    }
    @Test public void invalidTimestampCannotUnlock(){
        TokenTxRecord r=unknown();assertFalse(TradeRecoveryPolicy.acceptRisk(r,false,0));
        assertFalse(TradeRecoveryPolicy.acceptRisk(r,false,-1));assertTrue(TradeRecoveryPolicy.blocks(r));
    }
    @Test public void nullAndLegacyHistoryAreSafe(){
        assertFalse(TradeRecoveryPolicy.blocks(null));assertFalse(TradeRecoveryPolicy.acceptRisk(null,false,123));
        assertFalse(TradeRecoveryPolicy.blocks(new TokenTxRecord()));
    }
    @Test public void deadlineIsDurableAndOlderRecordsAreNotAssignedAnInventedOne(){
        TokenTxRecord r=unknown();r.deadlineSeconds=1234567;r.receiptAttempts=4;
        TokenTxRecord saved=new Gson().fromJson(new Gson().toJson(r),TokenTxRecord.class);
        assertEquals(1234567,saved.deadlineSeconds);assertEquals(4,saved.receiptAttempts);
        assertEquals(0,new Gson().fromJson("{\"status\":\"UNKNOWN\"}",TokenTxRecord.class).deadlineSeconds);
    }
}
