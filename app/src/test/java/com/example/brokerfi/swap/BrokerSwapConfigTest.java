package com.example.brokerfi.swap;

import org.junit.Test;
import static org.junit.Assert.*;

public class BrokerSwapConfigTest {
    @Test public void originalAndProtocolWrappedNeverShareAnAddress() {
        BrokerSwapConfig cfg=BrokerSwapAbiTest.config();
        assertNotEquals(cfg.wbkc(),cfg.tokenAddress(SwapAsset.ORIGINAL_WBKC,"0xf66338bff6a95a4ce5f7ec688b120f223096acd5"));
        assertEquals("",cfg.tokenAddress(SwapAsset.BKC,""));
        assertEquals(1051,cfg.chainId());
    }
    @Test(expected=IllegalArgumentException.class) public void zeroAddressRejected() { BrokerSwapConfig.address("0x0000000000000000000000000000000000000000"); }
    @Test(expected=IllegalArgumentException.class) public void shortAddressRejected() { BrokerSwapConfig.address("0x1234"); }
    @Test(expected=IllegalArgumentException.class) public void missingDeploymentRejected() { BrokerSwapConfig.fromJson("{}"); }
    @Test public void legacyWalletIdsGetPrefixOnlyAtRpcBoundary() {
        String raw="abcdefabcdefabcdefabcdefabcdefabcdefabcd";
        assertEquals(raw,BrokerSwapConfig.walletId("0x"+raw));
        assertEquals("0x"+raw,BrokerSwapConfig.walletAddress(raw));
        assertEquals(raw,BrokerSwapConfig.walletId(raw.toUpperCase(java.util.Locale.ROOT)));
        assertEquals("",BrokerSwapConfig.walletId("NONSTANDARD_PRIVATE_KEY"));
    }
}
