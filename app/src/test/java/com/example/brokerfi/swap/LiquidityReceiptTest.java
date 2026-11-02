package com.example.brokerfi.swap;

import com.example.brokerfi.token.TokenTxRecord;
import com.google.gson.*;
import java.math.BigInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class LiquidityReceiptTest {
    static BrokerSwapConfig config() {
        JsonObject c=JsonParser.parseString(new Gson().toJson(BrokerSwapAbiTest.config())).getAsJsonObject();
        c.addProperty("liquidityRouter","0xff44a9fd58f4bcd096b0cbf780580cb6846efd0d");
        c.addProperty("liquidityRouterCodeHash","0x3d0e16ff18b343c4a894be08296a6a3e4f9588ee26b612a3a2e7bf56e1fb5f1e");
        return BrokerSwapConfig.fromJson(c.toString());
    }
    private TokenTxRecord record(boolean add) {
        TokenTxRecord r=new TokenTxRecord(); r.liquidityAction=add?"ADD":"REMOVE";
        r.txHash="0x"+BrokerSwapAbiTest.word(BigInteger.ONE); r.lpUnits="10"; r.musdtUnits="200"; r.bkcUnits="300";
        return r;
    }
    private JsonObject log(String topic,String[] addresses,String data) {
        JsonObject l=new JsonObject(); l.addProperty("address",config().pair()); l.addProperty("data",data);
        JsonArray topics=new JsonArray(); topics.add(topic); for(String a:addresses)topics.add(BrokerSwapAbiTest.addressWord(a)); l.add("topics",topics); return l;
    }
    private JsonObject receipt(boolean add) {
        JsonObject r=new JsonObject(); r.addProperty("transactionHash",record(add).txHash); r.addProperty("blockNumber","0x1"); r.addProperty("status","0x1"); r.addProperty("to",config().liquidityRouter());
        JsonArray logs=new JsonArray();
        logs.add(log(add?LiquidityReceipt.MINT:LiquidityReceipt.BURN,add?new String[]{config().liquidityRouter()}:new String[]{config().liquidityRouter(),config().liquidityRouter()},
                "0x"+BrokerSwapAbiTest.word(BigInteger.valueOf(100))+BrokerSwapAbiTest.word(BigInteger.valueOf(200))));
        logs.add(log(LiquidityReceipt.TRANSFER,add?new String[]{"0x0000000000000000000000000000000000000000",BrokerSwapAbiTest.WALLET}:new String[]{BrokerSwapAbiTest.WALLET,config().pair()},
                "0x"+BrokerSwapAbiTest.word(BigInteger.TEN)));
        r.add("logs",logs); return r;
    }
    private LiquidityReceipt parse(JsonObject r,boolean add) { JsonObject env=new JsonObject(); env.add("result",r); return LiquidityReceipt.parse(env.toString(),config(),record(add),BrokerSwapAbiTest.WALLET); }
    @Test public void mintAndLpTransferProveActualAmounts() { LiquidityReceipt r=parse(receipt(true),true); assertEquals("SUCCESS",r.status); assertEquals(BigInteger.TEN,r.lp); assertEquals(BigInteger.valueOf(100),r.musdt); }
    @Test public void burnAndExactWalletLpTransferProveRemoval() { assertEquals(BigInteger.TEN,parse(receipt(false),false).lp); }
    @Test public void missingLogsNeverUseEstimatedAmounts() { JsonObject r=receipt(true);r.remove("logs"); assertEquals("SUCCESS",parse(r,true).status);assertNull(parse(r,true).lp); }
    @Test public void duplicateProofIsAmbiguous() { JsonObject r=receipt(true);JsonArray logs=r.getAsJsonArray("logs");logs.add(logs.get(0).deepCopy()); assertNull(parse(r,true).lp); }
    @Test public void foreignSenderIsNotAccepted() { JsonObject r=receipt(true);r.getAsJsonArray("logs").get(0).getAsJsonObject().getAsJsonArray("topics").set(1,new JsonPrimitive(BrokerSwapAbiTest.addressWord(BrokerSwapAbiTest.WALLET))); assertNull(parse(r,true).lp); }
    @Test public void failedStatusIsNotSuccess() { JsonObject r=receipt(true);r.addProperty("status","0x0"); assertEquals("FAILED",parse(r,true).status); }
    @Test public void missingStatusIsPending() { JsonObject r=receipt(true);r.remove("status"); assertEquals("PENDING",parse(r,true).status); }
    @Test public void wrongHashIsPending() { JsonObject r=receipt(true);r.addProperty("transactionHash","0x"+BrokerSwapAbiTest.word(BigInteger.TEN)); assertEquals("PENDING",parse(r,true).status); }
    @Test public void oldDeploymentDoesNotImplicitlyEnableLiquidity() { assertFalse(BrokerSwapAbiTest.config().hasLiquidity()); assertTrue(config().hasLiquidity()); }
}
