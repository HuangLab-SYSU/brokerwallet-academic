package com.example.brokerfi.token;

import com.example.brokerfi.brokerfi.model.Transaction;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class NativeAssetHistoryTest {
    private static final String WALLET="0x1111111111111111111111111111111111111111",PEER="0x2222222222222222222222222222222222222222";
    private Transaction transaction(boolean sent) {
        Transaction t=new Transaction();t.setId("123");t.setFrom(sent?WALLET:PEER);t.setTo(sent?PEER:WALLET);
        t.setValue("1000000000000000000");t.setFee("1000000000000000");t.setTimestamp("2026-10-11T00:00:00Z");return t;
    }
    @Test public void nativeSendKeepsNativeIdAndExactAmount() {
        TokenTxRecord r=NativeAssetHistory.convert(Collections.singletonList(transaction(true)),WALLET).get(0);
        assertEquals(TokenItem.NATIVE_BKC,r.contractAddress);assertEquals("SEND",r.type);assertEquals("1",r.amountDisplay);assertEquals("123",r.txHash);
    }
    @Test public void receiveUsesWalletOwnershipNotTokenSymbol() {
        TokenTxRecord r=NativeAssetHistory.convert(Collections.singletonList(transaction(false)),WALLET).get(0);
        assertEquals("RECEIVE",r.type);assertEquals(PEER,r.detail);
    }
    @Test public void contractCallsWithoutNativeFeeAreNotInventedTransfers() {
        Transaction t=transaction(true);t.setFee(null);assertTrue(NativeAssetHistory.convert(Collections.singletonList(t),WALLET).isEmpty());
    }
    @Test public void otherAccountCannotLeakIntoAssetsHistory() { assertTrue(NativeAssetHistory.convert(Collections.singletonList(transaction(true)),"0x3333333333333333333333333333333333333333").isEmpty()); }
    @Test public void zeroValueCallsAreNotAssetReceipts() { Transaction t=transaction(true);t.setValue("0");assertTrue(NativeAssetHistory.convert(Collections.singletonList(t),WALLET).isEmpty()); }
    @Test public void timestampIsUtcAndInvalidTimestampDoesNotBecomeNow() {
        assertEquals(1791676800000L,NativeAssetHistory.timestamp("2026-10-11T00:00:00Z"));assertEquals(0,NativeAssetHistory.timestamp("invalid"));
    }
}
