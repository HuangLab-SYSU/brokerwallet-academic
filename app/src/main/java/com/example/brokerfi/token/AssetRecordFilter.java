package com.example.brokerfi.token;

import java.util.Locale;

/** Asset legs for modern journal entries, with the old wBKC fallback only for legacy rows. */
final class AssetRecordFilter {
    private AssetRecordFilter() {}
    static boolean matches(TokenTxRecord record,String selected,String oldWbkc) {
        if(record==null)return false;
        String target=normalized(selected),primary=normalized(record.contractAddress),secondary=normalized(record.toContractAddress);
        if(target.isEmpty())return false;
        if(record.status!=null) {
            boolean approval="APPROVAL".equals(record.type);
            if(primary.isEmpty() && "BKC".equals(record.fromAsset))primary=TokenItem.NATIVE_BKC;
            if(!approval && secondary.isEmpty() && "BKC".equals(record.toAsset))secondary=TokenItem.NATIVE_BKC;
            if(record.isLiquidity() && !approval && TokenItem.NATIVE_BKC.equals(target))return true;
            return target.equals(primary) || !approval && target.equals(secondary);
        }
        if(primary.isEmpty()) {
            boolean legacySwap=TokenTxRecord.TYPE_SWAP.equals(record.type) || TokenTxRecord.TYPE_WRAP.equals(record.type) || TokenTxRecord.TYPE_UNWRAP.equals(record.type);
            if(!legacySwap && !(record.txHash!=null && record.txHash.startsWith("0x") && record.txHash.length()>=10))return false;
            primary=normalized(oldWbkc);
        }
        return target.equals(primary);
    }
    private static String normalized(String value) { return value==null?"":value.trim().toLowerCase(Locale.US); }
}
