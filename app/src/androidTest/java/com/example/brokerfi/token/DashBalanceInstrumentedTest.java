package com.example.brokerfi.token;

import com.example.brokerfi.xc.SecurityUtil;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 璁惧/CI 闆嗘垚娴嬭瘯锛歞ash 绛惧悕 eth_call 璇诲彇 wBKC balanceOf銆? * 杩愯锛歿@code ./gradlew connectedDebugAndroidTest}
 */
public class DashBalanceInstrumentedTest {

    private static final String wBKC = "0xa217cc08d6579793a89ea20dac173647ecf78100";

    @Test
    public void dashEthCall_balanceOf_zeroForFreshKey() throws Exception {
        String privateKey = SecurityUtil.generatePrivateKey();
        String wallet = SecurityUtil.GetAddress(privateKey);

        BigInteger balance = TokenContractHelper.readBalance(wBKC, wallet, privateKey);

        assertNotNull("dash eth_call must return chain balance", balance);
        assertTrue("fresh wallet has no wBKC", balance.signum() == 0);
    }
}

