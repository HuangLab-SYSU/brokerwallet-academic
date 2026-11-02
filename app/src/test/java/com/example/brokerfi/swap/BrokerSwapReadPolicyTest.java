package com.example.brokerfi.swap;

import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;

public class BrokerSwapReadPolicyTest {
    @Test public void onlyExplicitReadRpcMethodsCanUseRetryPath() {
        for(String method:new String[]{"eth_chainId","eth_getCode","eth_getBalance","eth_gasPrice"})
            assertTrue(BrokerSwapClient.isReadOnlyRpcMethod(method));
        assertFalse(BrokerSwapClient.isReadOnlyRpcMethod("eth_sendTransaction"));
        assertFalse(BrokerSwapClient.isReadOnlyRpcMethod("eth_sendRawTransaction"));
        assertFalse(BrokerSwapClient.isReadOnlyRpcMethod(null));
    }
    @Test(expected=IOException.class) public void emptyResponseCannotBecomeZeroBalance() throws Exception {
        BrokerSwapClient.requireReadHex("0x");
    }
    @Test(expected=IOException.class) public void incompleteAbiWordRejected() throws Exception {
        BrokerSwapClient.requireReadHex("0x0");
    }
    @Test public void actualZeroWordIsValid() throws Exception {
        String zero="0x"+BrokerSwapAbiTest.word(java.math.BigInteger.ZERO);
        assertEquals(zero,BrokerSwapClient.requireReadHex(zero));
    }
    @Test public void fullReserveResponseIsValid() throws Exception {
        String reserves="0x"+BrokerSwapAbiTest.word(java.math.BigInteger.ONE)
                +BrokerSwapAbiTest.word(java.math.BigInteger.TEN)+BrokerSwapAbiTest.word(java.math.BigInteger.ZERO);
        assertEquals(reserves,BrokerSwapClient.requireReadHex(reserves));
    }
}
