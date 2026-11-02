package com.example.brokerfi.token;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Compatibility only: never merges conflicting rows, changes statuses or removes source data. */
final class HistoryPreferenceMigration {
    static String canonicalWallet(String wallet) {
        String value=wallet==null?"":wallet.trim().toLowerCase(Locale.US);
        return value.matches("0x[0-9a-f]{40}")?value.substring(2):value;
    }
    static String aliasKey(String key) {
        String wallet=key.startsWith("list_")?key.substring(5):"";
        if(wallet.matches("[0-9a-f]{40}"))return "list_0x"+wallet;
        if(wallet.matches("0x[0-9a-f]{40}"))return "list_"+wallet.substring(2);
        return key;
    }
    static Map<String,String> missingLists(Map<String,?> current,Map<String,?> legacy) {
        Map<String,String> result=new LinkedHashMap<>();
        for(Map.Entry<String,?> row:legacy.entrySet())
            if(row.getKey().startsWith("list_") && row.getValue() instanceof String
                    && !current.containsKey(row.getKey()) && !current.containsKey(aliasKey(row.getKey())))
                result.put(row.getKey(),(String)row.getValue());
        return result;
    }
    private HistoryPreferenceMigration() {}
}
