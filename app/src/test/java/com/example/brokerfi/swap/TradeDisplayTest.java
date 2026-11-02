package com.example.brokerfi.swap;

import org.junit.Test;
import java.math.BigInteger;
import static org.junit.Assert.*;

public class TradeDisplayTest {
    @Test public void displayUsesAtMostSixPlacesWithoutChangingUnderlyingUnits() {
        BigInteger input=SwapQuoteMath.parseUnits("21.3118745",18);
        assertEquals("21.311874",TradeDisplay.amount(input,18));assertEquals("21.3118745",SwapQuoteMath.format(input,18));
    }
    @Test public void tinyPositiveOutputNeverPretendsToBeZero() { assertEquals("<0.000001",TradeDisplay.amount(BigInteger.ONE,18)); }
    @Test public void largeBalanceGroupsWithoutDroppingTokenPrecision() { assertEquals("197,972.57",TradeDisplay.amount(SwapQuoteMath.parseUnits("197972.57",6),6)); }
    @Test public void rateUsesQuoteRatherThanInventingOneToOneExchange() {
        assertTrue(TradeDisplay.rate(BrokerSwapAbiTest.quote(SwapAsset.BKC,SwapAsset.MUSDT)).startsWith("1 BKC ≈ "));
    }
}
