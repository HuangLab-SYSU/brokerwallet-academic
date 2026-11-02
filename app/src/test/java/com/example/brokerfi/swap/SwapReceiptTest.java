package com.example.brokerfi.swap;

import org.junit.Test;
import java.math.BigInteger;
import static org.junit.Assert.*;
import static com.example.brokerfi.swap.BrokerSwapAbiTest.*;

public class SwapReceiptTest {
    private static final String HASH = "0x" + word(BigInteger.ONE);
    private String receipt(String status, String logs) {
        return "{\"result\":{\"transactionHash\":\"" + HASH + "\",\"blockNumber\":\"0x1\","
                + status + "\"logs\":" + logs + "}}";
    }
    private String log(String pair, String receiver, BigInteger input) {
        return "{\"address\":\"" + pair + "\",\"topics\":[\"" + SwapReceipt.SWAP_TOPIC + "\",\""
                + addressWord(config().router()) + "\",\"" + addressWord(receiver) + "\"],\"data\":\"0x"
                + word(BigInteger.ZERO) + word(input) + word(BigInteger.valueOf(168069)) + word(BigInteger.ZERO) + "\"}";
    }
    private SwapReceipt parse(String raw) { return SwapReceipt.parse(raw, HASH, config(), SwapAsset.BKC, SwapAsset.MUSDT,
            new BigInteger("1000000000000000"), WALLET); }
    @Test public void emptyResultRemainsPending() { assertEquals("PENDING", parse("{\"result\":null}").status); }
    @Test public void missingStatusRemainsPending() { assertEquals("PENDING", parse(receipt("", "[]")).status); }
    @Test public void statusZeroIsFailed() { assertEquals("FAILED", parse(receipt("\"status\":\"0x0\",", "[]")).status); }
    @Test public void successfulWithoutLogsDoesNotInventOutput() {
        SwapReceipt r = parse(receipt("\"status\":\"0x1\",", "[]")); assertEquals("SUCCESS", r.status); assertNull(r.actualOutput);
    }
    @Test public void validPairSwapReportsActualOutput() {
        SwapReceipt r = parse(receipt("\"status\":\"0x1\",", "[" + log(config().pair(), WALLET, new BigInteger("1000000000000000")) + "]"));
        assertEquals(BigInteger.valueOf(168069), r.actualOutput);
    }
    @Test public void legacyUnprefixedWalletStillMatchesEventRecipient() {
        String raw=receipt("\"status\":\"0x1\",", "["+log(config().pair(),WALLET,new BigInteger("1000000000000000"))+"]");
        assertEquals(BigInteger.valueOf(168069),SwapReceipt.parse(raw,HASH,config(),SwapAsset.BKC,SwapAsset.MUSDT,
                new BigInteger("1000000000000000"),WALLET.substring(2)).actualOutput);
    }
    @Test public void wrongPairIgnored() {
        assertNull(parse(receipt("\"status\":\"0x1\",", "[" + log(WALLET, WALLET, new BigInteger("1000000000000000")) + "]")).actualOutput);
    }
    @Test public void wrongRecipientIgnored() {
        assertNull(parse(receipt("\"status\":\"0x1\",", "[" + log(config().pair(), config().router(), new BigInteger("1000000000000000")) + "]")).actualOutput);
    }
    @Test public void wrongInputIgnored() {
        assertNull(parse(receipt("\"status\":\"0x1\",", "[" + log(config().pair(), WALLET, BigInteger.ONE) + "]")).actualOutput);
    }
    @Test public void validHashIsStrict() { assertTrue(SwapReceipt.validHash(HASH)); assertFalse(SwapReceipt.validHash(HASH + "extra")); }
    @Test public void jsonAndPlainSendHashesAgree() {
        assertEquals(HASH,SwapReceipt.sentHash(HASH));
        assertEquals(HASH,SwapReceipt.sentHash(HASH.substring(2)));
        assertEquals(HASH,SwapReceipt.sentHash("{\"result\":\""+HASH+"\"}"));
    }
    @Test(expected=IllegalArgumentException.class) public void sendErrorWithHashStillRejected() {
        SwapReceipt.sentHash("{\"error\":{\"message\":\"failed\"},\"result\":\""+HASH+"\"}");
    }
    @Test(expected=IllegalArgumentException.class) public void opaqueGatewayIdIsNotTransactionHash() { SwapReceipt.sentHash("some-id"); }
    @Test public void wrongReceiptHashRemainsPending() {
        assertEquals("PENDING",parse(receipt("\"status\":\"0x1\",","[]").replace(HASH,"0x"+word(BigInteger.TEN))).status);
    }
    @Test public void nativeUnwrapOutputRequiresRouterDestination() {
        BigInteger input=BigInteger.valueOf(100000), output=new BigInteger("590000000000000");
        String raw=receipt("\"status\":\"0x1\",", "[{\"address\":\""+config().pair()+"\",\"topics\":[\""
                +SwapReceipt.SWAP_TOPIC+"\",\""+addressWord(config().router())+"\",\""+addressWord(config().router())
                +"\"],\"data\":\"0x"+word(input)+word(BigInteger.ZERO)+word(BigInteger.ZERO)+word(output)+"\"}]");
        assertEquals(output,SwapReceipt.parse(raw,HASH,config(),SwapAsset.MUSDT,SwapAsset.BKC,input,WALLET).actualOutput);
        assertNull(SwapReceipt.parse(raw,HASH,config(),SwapAsset.MUSDT,SwapAsset.SWAP_WBKC,input,WALLET).actualOutput);
    }
}
