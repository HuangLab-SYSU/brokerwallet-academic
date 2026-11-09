package com.example.brokerfi.swap;

import org.junit.Test;
import java.math.BigInteger;
import static org.junit.Assert.*;

public class LiquidityMathTest {
    private static final BigInteger ONE=new BigInteger("1000000000000000000");
    private final SwapPoolSnapshot pool=new SwapPoolSnapshot(BigInteger.valueOf(100000000),ONE,1);
    @Test public void choosesOptimalAmountsAndRefundsExcessNative() {
        LiquidityMath.AddQuote q=LiquidityMath.add(pool,ONE,BigInteger.valueOf(10000000),ONE.divide(BigInteger.valueOf(5)));
        assertEquals(BigInteger.valueOf(10000000),q.musdt);
        assertEquals(ONE.divide(BigInteger.TEN),q.bkc);
        assertEquals(ONE.divide(BigInteger.TEN),q.lp);
    }
    @Test public void smallerNativeAmountLimitsBothSides() {
        LiquidityMath.AddQuote q=LiquidityMath.add(pool,ONE,BigInteger.valueOf(20000000),ONE.divide(BigInteger.TEN));
        assertEquals(BigInteger.valueOf(10000000),q.musdt);
        assertEquals(ONE.divide(BigInteger.TEN),q.lp);
    }
    @Test public void removalIsProportional() {
        BigInteger[] q=LiquidityMath.remove(pool,ONE,ONE.divide(BigInteger.TEN));
        assertEquals(BigInteger.valueOf(10000000),q[0]);
        assertEquals(ONE.divide(BigInteger.TEN),q[1]);
    }
    @Test public void fullRemovalDoesNotRoundAwayLp() { assertEquals(ONE,LiquidityMath.portion(ONE,100)); }
    @Test public void halfPercentMinimumRoundsDown() { assertEquals(BigInteger.valueOf(9950),LiquidityMath.minimum(BigInteger.valueOf(10000))); }
    @Test(expected=IllegalArgumentException.class) public void rejectsZeroInput() { LiquidityMath.add(pool,ONE,BigInteger.ZERO,ONE); }
    @Test(expected=IllegalArgumentException.class) public void rejectsInvalidRemovalPercentage() { LiquidityMath.portion(ONE,101); }
    @Test(expected=IllegalArgumentException.class) public void rejectsMoreThanTotalSupply() { LiquidityMath.remove(pool,ONE,ONE.add(BigInteger.ONE)); }
    @Test public void feesAreZeroAtObservationAndOnlyAnEstimateAfterGrowth() {
        BigInteger root=LiquidityMath.sqrt(pool.musdtReserve.multiply(pool.wbkcReserve));
        assertEquals(BigInteger.ZERO,LiquidityMath.estimatedFeesMusdt(pool,ONE,ONE.divide(BigInteger.TEN),root,ONE));
        SwapPoolSnapshot larger=new SwapPoolSnapshot(pool.musdtReserve.multiply(BigInteger.TWO),pool.wbkcReserve.multiply(BigInteger.TWO),2);
        assertTrue(LiquidityMath.estimatedFeesMusdt(larger,ONE,ONE.divide(BigInteger.TEN),root,ONE).signum()>0);
        assertEquals(BigInteger.ZERO,LiquidityMath.estimatedFeesMusdt(larger,ONE.multiply(BigInteger.TWO),ONE.divide(BigInteger.TEN),root,ONE));
    }
    @Test public void integerSquareRootIsExactOrFloored() {
        assertEquals(BigInteger.valueOf(3),LiquidityMath.sqrt(BigInteger.valueOf(15)));
        assertEquals(BigInteger.valueOf(4),LiquidityMath.sqrt(BigInteger.valueOf(16)));
    }
}
