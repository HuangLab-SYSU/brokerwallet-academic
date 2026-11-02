package com.example.brokerfi.swap;

import org.junit.Test;
import java.math.BigInteger;
import static org.junit.Assert.*;

public class LiquiditySafetyTest {
    private static BigInteger n(long value) { return BigInteger.valueOf(value); }
    @Test public void addAcceptsExactConfirmedMinimums() {
        LiquiditySafety.addMinimums(new LiquidityMath.AddQuote(n(100),n(200),n(300)),n(100),n(200),n(300));
    }
    @Test(expected=IllegalArgumentException.class) public void addRejectsChangedMusdtEvenIfLpIsEnough() {
        LiquiditySafety.addMinimums(new LiquidityMath.AddQuote(n(99),n(200),n(300)),n(100),n(200),n(300));
    }
    @Test(expected=IllegalArgumentException.class) public void addRejectsChangedBkc() {
        LiquiditySafety.addMinimums(new LiquidityMath.AddQuote(n(100),n(199),n(300)),n(100),n(200),n(300));
    }
    @Test(expected=IllegalArgumentException.class) public void addRejectsChangedLp() {
        LiquiditySafety.addMinimums(new LiquidityMath.AddQuote(n(100),n(200),n(299)),n(100),n(200),n(300));
    }
    @Test(expected=IllegalArgumentException.class) public void addNeverAcceptsZeroProtection() {
        LiquiditySafety.addMinimums(new LiquidityMath.AddQuote(n(100),n(200),n(300)),n(0),n(200),n(300));
    }
    @Test public void removeAcceptsExactMinimums() { LiquiditySafety.removeMinimums(new BigInteger[]{n(100),n(200)},n(100),n(200)); }
    @Test(expected=IllegalArgumentException.class) public void removeRejectsChangedMusdt() { LiquiditySafety.removeMinimums(new BigInteger[]{n(99),n(200)},n(100),n(200)); }
    @Test(expected=IllegalArgumentException.class) public void removeRejectsChangedBkc() { LiquiditySafety.removeMinimums(new BigInteger[]{n(100),n(199)},n(100),n(200)); }
    @Test(expected=IllegalArgumentException.class) public void removeNeverAcceptsZeroProtection() { LiquiditySafety.removeMinimums(new BigInteger[]{n(100),n(200)},n(100),n(0)); }
    @Test public void approvalAndActionReserveTwoTransactions() { assertEquals(n(600),LiquiditySafety.reserve(n(300),true)); assertEquals(n(300),LiquiditySafety.reserve(n(300),false)); }
    @Test(expected=IllegalArgumentException.class) public void failedGasReadCannotEnableAction() { LiquiditySafety.reserve(null,false); }
    @Test(expected=IllegalArgumentException.class) public void zeroGasReserveIsRejected() { LiquiditySafety.reserve(BigInteger.ZERO,false); }
}
