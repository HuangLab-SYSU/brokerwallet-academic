package com.example.brokerfi.token;

import com.example.brokerfi.brokerfi.model.Transaction;
import com.example.brokerfi.swap.BrokerSwapConfig;
import com.example.brokerfi.swap.SwapQuoteMath;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

/** Reuses native gettx2 records, keeping native IDs distinct from EVM receipt hashes. */
public final class NativeAssetHistory {
    private NativeAssetHistory() {}
    public static List<TokenTxRecord> convert(List<Transaction> transactions,String wallet) {
        List<TokenTxRecord> result=new ArrayList<>(); String owner=BrokerSwapConfig.walletId(wallet);
        if (owner.isEmpty() || transactions==null) return result;
        for (Transaction t:transactions) {
            try {
                String from=BrokerSwapConfig.walletId(t.getFrom()),to=BrokerSwapConfig.walletId(t.getTo());
                if (from.isEmpty() || to.isEmpty() || t.getFee()==null || t.getFee().trim().isEmpty()
                        || t.getId()==null || t.getId().trim().isEmpty()) continue;
                boolean sent=owner.equals(from),received=owner.equals(to);
                if (!sent && !received) continue;
                BigInteger value=new BigInteger(t.getValue()),fee=new BigInteger(t.getFee());
                if (value.signum()<=0 || fee.signum()<0) continue;
                long time=timestamp(t.getTimestamp()); if (time<=0) continue;
                TokenTxRecord r=new TokenTxRecord(sent?TokenTxRecord.TYPE_SEND:TokenTxRecord.TYPE_RECEIVE,
                        SwapQuoteMath.format(value,18),"0x"+(sent?to:from),t.getId().trim(),time);
                r.contractAddress=TokenItem.NATIVE_BKC; r.fromSymbol="BKC"; r.gasCost=SwapQuoteMath.format(fee,18); result.add(r);
            } catch (RuntimeException ignored) { }
        }
        return result;
    }
    static long timestamp(String value) {
        if (value==null) return 0;
        try { long n=Long.parseLong(value); return n<100000000000L ? n*1000 : n; } catch (NumberFormatException ignored) { }
        for(String pattern:new String[]{"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'","yyyy-MM-dd'T'HH:mm:ss'Z'"}) {
            try { SimpleDateFormat fmt=new SimpleDateFormat(pattern,Locale.US);fmt.setLenient(false);fmt.setTimeZone(TimeZone.getTimeZone("UTC"));return fmt.parse(value).getTime(); }
            catch (java.text.ParseException ignored) { }
        }
        return 0;
    }
}
