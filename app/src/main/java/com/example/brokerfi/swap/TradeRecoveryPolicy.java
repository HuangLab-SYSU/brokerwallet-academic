package com.example.brokerfi.swap;

import com.example.brokerfi.token.TokenTxRecord;

/** Separate unknown outcome from a user's explicit decision to start an independent operation. */
public final class TradeRecoveryPolicy {
    private TradeRecoveryPolicy() {}
    public static boolean unresolved(TokenTxRecord r) {
        return r!=null && ("UNKNOWN".equals(r.status)||"SUBMITTING".equals(r.status)||"PENDING".equals(r.status));
    }
    public static boolean canAcceptRisk(TokenTxRecord r,boolean active) {
        return !active && r!=null && ("UNKNOWN".equals(r.status)||"SUBMITTING".equals(r.status))
                && !SwapReceipt.validHash(r.txHash);
    }
    public static boolean deferred(TokenTxRecord r) {
        return canAcceptRisk(r,false) && r.continuationRiskAcceptedMs>0;
    }
    public static boolean blocks(TokenTxRecord r) { return unresolved(r) && !deferred(r); }
    public static boolean acceptRisk(TokenTxRecord r,boolean active,long nowMs) {
        if(!canAcceptRisk(r,active)||nowMs<=0)return false;
        r.continuationRiskAcceptedMs=nowMs;
        return true;
    }
    static String details(TokenTxRecord r) {
        return deferred(r)?"\nPrevious outcome remains unverified. The risk of delayed execution or extra fees was accepted for an independent new transaction. The previous request was not canceled or resent.":"";
    }
}
