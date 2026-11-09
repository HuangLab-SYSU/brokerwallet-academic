package com.example.brokerfi.swap;

import org.junit.Test;
import org.web3j.crypto.Hash;
import java.math.BigInteger;
import static org.junit.Assert.*;

public class LiquidityAbiTest {
    @Test public void addCarriesAllMinimumsAndRecipient() {
        String call=LiquidityAbi.add(BigInteger.TEN,BigInteger.valueOf(9),BigInteger.valueOf(8),BigInteger.valueOf(7),BrokerSwapAbiTest.WALLET,1234);
        assertTrue(call.startsWith(Hash.sha3String("addLiquidityBKC(uint256,uint256,uint256,uint256,address,uint256)").substring(0,10)));
        BigInteger[] words=BrokerSwapAbi.words("0x"+call.substring(10),6);
        assertArrayEquals(new BigInteger[]{BigInteger.TEN,BigInteger.valueOf(9),BigInteger.valueOf(8),BigInteger.valueOf(7),BigInteger.ONE,BigInteger.valueOf(1234)},words);
    }
    @Test public void removeUsesExactLpAndBothMinimums() {
        String call=LiquidityAbi.remove(BigInteger.TEN,BigInteger.valueOf(9),BigInteger.valueOf(8),BrokerSwapAbiTest.WALLET,1234);
        assertTrue(call.startsWith(Hash.sha3String("removeLiquidityBKC(uint256,uint256,uint256,address,uint256)").substring(0,10)));
        assertEquals(5,BrokerSwapAbi.words("0x"+call.substring(10),5).length);
    }
}
