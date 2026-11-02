package com.example.brokerfi.swap;

import org.junit.Test;
import java.math.BigInteger;
import static org.junit.Assert.*;

public class BkcIntegerPrecisionTest {
    @Test public void oneWeiSurvivesNativeAmountParsingAndFormatting() {
        assertEquals(BigInteger.ONE,SwapQuoteMath.parseUnits("0.000000000000000001",18));
        assertEquals("0.000000000000000001",SwapQuoteMath.format(BigInteger.ONE,18));
    }
    @Test public void fullRpcBalanceAboveDoublePrecisionRoundTripsExactly() {
        BigInteger balance=new BigInteger("21254899210543969826");
        assertEquals(balance,SwapQuoteMath.parseUnits(SwapQuoteMath.format(balance,18),18));
        assertEquals(balance,SwapReceipt.quantity("0x"+balance.toString(16)));
    }
    @Test public void adjacentWeiBalancesNeverCollapseToTheSameValue() {
        BigInteger expected=SwapQuoteMath.parseUnits("21.254899210543969825",18);
        BigInteger actual=SwapQuoteMath.parseUnits("21.254899210543969826",18);
        assertEquals(BigInteger.ONE,actual.subtract(expected));
        assertNotEquals(SwapQuoteMath.format(expected,18),SwapQuoteMath.format(actual,18));
    }
    @Test public void recordedReverseSwapReconciliationPreservesTheReportedOneWeiDifference() {
        BigInteger baseline=SwapQuoteMath.parseUnits("21.274328347664993828",18);
        BigInteger fees=SwapQuoteMath.parseUnits("0.0193942",18),input=SwapQuoteMath.parseUnits("0.0001",18);
        BigInteger output=SwapQuoteMath.parseUnits("0.000065062878975997",18);
        BigInteger expected=baseline.subtract(fees).subtract(input).add(output);
        assertEquals("21.254899210543969825",SwapQuoteMath.format(expected,18));
        assertEquals(BigInteger.ONE,SwapQuoteMath.parseUnits("21.254899210543969826",18).subtract(expected));
    }
    @Test public void gasFeeAndTokenDecimalsAreExactIntegers() {
        assertEquals("0.0090086",SwapQuoteMath.format(BigInteger.valueOf(90086).multiply(new BigInteger("100000000000")),18));
        assertEquals(BigInteger.valueOf(163873),SwapQuoteMath.parseUnits("0.163873",6));
    }
}
