package com.example.brokerfi.swap;

import java.math.BigInteger;

/** Pure preflight guards, independently tested without keys, a wallet or chain writes. */
final class LiquiditySafety {
    private LiquiditySafety() {}
    static void addMinimums(LiquidityMath.AddQuote quote,BigInteger minM,BigInteger minB,BigInteger minLp) {
        if(minM.signum()<=0 || minB.signum()<=0 || minLp.signum()<=0
                || quote.musdt.compareTo(minM)<0 || quote.bkc.compareTo(minB)<0 || quote.lp.compareTo(minLp)<0)
            throw new IllegalArgumentException("Confirmed add minimum changed");
    }
    static void removeMinimums(BigInteger[] quote,BigInteger minM,BigInteger minB) {
        if(minM.signum()<=0 || minB.signum()<=0 || quote[0].compareTo(minM)<0 || quote[1].compareTo(minB)<0)
            throw new IllegalArgumentException("Confirmed remove minimum changed");
    }
    static BigInteger reserve(BigInteger gas,boolean needsApproval) {
        if(gas==null || gas.signum()<=0)throw new IllegalArgumentException("No verified gas reserve");
        return gas.multiply(BigInteger.valueOf(needsApproval?2:1));
    }
}
