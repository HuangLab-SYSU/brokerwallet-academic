package com.example.brokerfi.swap;

import org.junit.Test;
import org.web3j.crypto.Hash;
import java.math.BigInteger;
import static org.junit.Assert.*;

public class BrokerSwapAbiTest {
    static final String WALLET = "0x0000000000000000000000000000000000000001";
    static BrokerSwapConfig config() {
        return BrokerSwapConfig.fromJson("{\"chainId\":1051,\"factory\":\"0xb1185c2a9077ce35572cffabf825dd2db299a662\","
                + "\"router\":\"0x463ea41fdd04bbd0f821291ab825c472ea6a2ffe\",\"pair\":\"0x40283f8eccec16386081b1e4a1e83c5c8939adcb\","
                + "\"wbkc\":\"0x4761865ca85000b23df6fb00d2c4f81aa873513b\",\"musdt\":\"0x2881b7efad41d88f3c244fbb3503df3e12bc0b26\","
                + "\"routerCodeHash\":\"0xdc961f6baa034fa10378a5069846cb5951f49d5427e8e1ca2270f9254d5ada2c\"}");
    }
    static SwapQuoteMath.Quote quote(SwapAsset from, SwapAsset to) {
        return SwapQuoteMath.quote(new SwapPoolSnapshot(new BigInteger("1845712407"), new BigInteger("10947869402511077675"), 1),
                from, to, from == SwapAsset.MUSDT ? BigInteger.valueOf(100000) : new BigInteger("1000000000000000"), 50, 1);
    }
    static String word(BigInteger n) { return String.format("%064x", n); }
    static String addressWord(String address) { return "0x" + word(new BigInteger(address.substring(2), 16)); }
    @Test public void fixedNativeEncodingIncludesMinimumAndDeadline() {
        SwapQuoteMath.Quote q = quote(SwapAsset.BKC, SwapAsset.MUSDT);
        String expected = Hash.sha3String("swapExactBkcForMusdt(uint256,uint256,address,uint256)").substring(0,10)
                + word(q.output) + word(q.minimum) + word(BigInteger.ONE) + word(BigInteger.valueOf(1700000000));
        assertEquals(expected, BrokerSwapAbi.swap(config(), q, WALLET, 1700000000));
    }
    @Test public void wrappedRouteEncodesDynamicPathAfterFiveHeadWords() {
        SwapQuoteMath.Quote q = quote(SwapAsset.SWAP_WBKC, SwapAsset.MUSDT);
        String expected = "0x38ed1739" + word(q.input) + word(q.minimum) + word(BigInteger.valueOf(160))
                + word(BigInteger.ONE) + word(BigInteger.valueOf(1700000000)) + word(BigInteger.valueOf(2))
                + addressWord(config().wbkc()).substring(2) + addressWord(config().musdt()).substring(2);
        assertEquals(expected, BrokerSwapAbi.swap(config(), q, WALLET, 1700000000));
    }
    @Test public void musdtRoutesUseDifferentSelectors() {
        String nativeCall = BrokerSwapAbi.swap(config(), quote(SwapAsset.MUSDT, SwapAsset.BKC), WALLET, 1);
        String wrappedCall = BrokerSwapAbi.swap(config(), quote(SwapAsset.MUSDT, SwapAsset.SWAP_WBKC), WALLET, 1);
        assertTrue(nativeCall.startsWith(Hash.sha3String("swapExactMusdtForBkc(uint256,uint256,uint256,address,uint256)").substring(0,10)));
        assertTrue(wrappedCall.startsWith(Hash.sha3String("swapExactMusdtForWbkc(uint256,uint256,uint256,address,uint256)").substring(0,10)));
        assertNotEquals(nativeCall, wrappedCall);
    }
    @Test(expected=IllegalArgumentException.class) public void emptyReadIsNotZeroBalance() { BrokerSwapAbi.words("0x", 1); }
    @Test(expected=IllegalArgumentException.class) public void truncatedReservesRejected() { BrokerSwapAbi.words("0x" + word(BigInteger.ONE), 3); }
    @Test(expected=IllegalArgumentException.class) public void oversizedAddressRejected() { BrokerSwapAbi.address("0x" + word(BigInteger.ONE.shiftLeft(160))); }
}
