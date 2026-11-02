package com.example.brokerfi.swap;

import com.example.brokerfi.token.TokenTxRecord;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigInteger;
import org.web3j.crypto.Hash;

/** Status is authoritative; amounts require a unique matching Mint/Burn and LP transfer proof. */
public final class LiquidityReceipt {
    public static final String TRANSFER=Hash.sha3String("Transfer(address,address,uint256)");
    public static final String MINT=Hash.sha3String("Mint(address,uint256,uint256)");
    public static final String BURN=Hash.sha3String("Burn(address,uint256,uint256,address)");
    public final String status;
    public final BigInteger musdt, bkc, lp, gasCost;
    private LiquidityReceipt(String s, BigInteger m, BigInteger b, BigInteger l, BigInteger g) { status=s; musdt=m; bkc=b; lp=l; gasCost=g; }
    private static LiquidityReceipt pending() { return new LiquidityReceipt("PENDING",null,null,null,null); }
    public static LiquidityReceipt parse(String raw, BrokerSwapConfig cfg, TokenTxRecord record, String wallet) {
        try {
            JsonObject envelope=JsonParser.parseString(raw).getAsJsonObject();
            if (envelope.has("error") || !envelope.has("result") || !envelope.get("result").isJsonObject()) return pending();
            JsonObject r=envelope.getAsJsonObject("result");
            if (!record.txHash.equalsIgnoreCase(r.get("transactionHash").getAsString()) || !r.has("blockNumber")
                    || r.get("blockNumber").isJsonNull() || !r.has("status")) return pending();
            BigInteger gas=null;
            if (r.has("gasUsed") && r.has("effectiveGasPrice")) gas=SwapReceipt.quantity(r.get("gasUsed").getAsString())
                    .multiply(SwapReceipt.quantity(r.get("effectiveGasPrice").getAsString()));
            if ("0x0".equalsIgnoreCase(r.get("status").getAsString())) return new LiquidityReceipt("FAILED",null,null,null,gas);
            if (!"0x1".equalsIgnoreCase(r.get("status").getAsString())) return pending();
            LiquidityReceipt unknown=new LiquidityReceipt("SUCCESS",null,null,null,gas);
            if (record.liquidityAction.startsWith("APPROVE")) return unknown;
            boolean add="ADD".equals(record.liquidityAction);
            String target=BrokerSwapConfig.walletAddress(wallet);
            if (r.has("to") && !cfg.liquidityRouter().equalsIgnoreCase(r.get("to").getAsString())) return unknown;
            BigInteger m=null,b=null,l=null; int events=0,proofs=0;
            JsonArray logs=r.has("logs") && r.get("logs").isJsonArray() ? r.getAsJsonArray("logs") : new JsonArray();
            for (com.google.gson.JsonElement entry:logs) {
                try {
                    JsonObject log=entry.getAsJsonObject();
                    if (!cfg.pair().equalsIgnoreCase(log.get("address").getAsString())) continue;
                    JsonArray t=log.getAsJsonArray("topics");
                    if (t.size()==(add?2:3) && (add?MINT:BURN).equalsIgnoreCase(t.get(0).getAsString())
                            && cfg.liquidityRouter().equalsIgnoreCase(BrokerSwapAbi.address(t.get(1).getAsString()))
                            && (add || cfg.liquidityRouter().equalsIgnoreCase(BrokerSwapAbi.address(t.get(2).getAsString())))) {
                        BigInteger[] amounts=BrokerSwapAbi.words(log.get("data").getAsString(),2);
                        boolean musdt0=cfg.musdt().compareTo(cfg.wbkc())<0;
                        m=amounts[musdt0?0:1]; b=amounts[musdt0?1:0]; events++;
                    }
                    if (t.size()==3 && TRANSFER.equalsIgnoreCase(t.get(0).getAsString())) {
                        BigInteger from=BrokerSwapAbi.words(t.get(1).getAsString(),1)[0];
                        BigInteger to=BrokerSwapAbi.words(t.get(2).getAsString(),1)[0];
                        boolean match=add ? from.signum()==0 && to.equals(new BigInteger(target.substring(2),16))
                                : from.equals(new BigInteger(target.substring(2),16)) && to.equals(new BigInteger(cfg.pair().substring(2),16));
                        if (match) { l=BrokerSwapAbi.words(log.get("data").getAsString(),1)[0]; proofs++; }
                    }
                } catch (RuntimeException ignored) { }
            }
            if (events!=1 || proofs!=1 || m==null || b==null || l==null || m.signum()<=0 || b.signum()<=0 || l.signum()<=0) return unknown;
            if (!add && !l.equals(new BigInteger(record.lpUnits))) return unknown;
            if (add && (m.compareTo(new BigInteger(record.musdtUnits))>0 || b.compareTo(new BigInteger(record.bkcUnits))>0)) return unknown;
            if (record.minimumMusdtUnits!=null && m.compareTo(new BigInteger(record.minimumMusdtUnits))<0
                    || record.minimumBkcUnits!=null && b.compareTo(new BigInteger(record.minimumBkcUnits))<0
                    || add && record.minimumLpUnits!=null && l.compareTo(new BigInteger(record.minimumLpUnits))<0) return unknown;
            return new LiquidityReceipt("SUCCESS",m,b,l,gas);
        } catch (RuntimeException e) { return pending(); }
    }
}
