package com.example.brokerfi.swap;

import com.example.brokerfi.token.TokenTxRecord;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Demo wording and display formatting only; never used for transaction amounts. */
public final class VideoTradePresentation {
    private VideoTradePresentation() {}
    public static String amount(String value) {
        if(value==null || value.isEmpty())return "Unverified";
        try{return TradeDisplay.decimal(new BigDecimal(value));}catch(NumberFormatException e){return value;}
    }
    public static boolean approval(TokenTxRecord r) {
        return "APPROVAL".equals(r.type) || r.isLiquidity() && r.liquidityAction.startsWith("APPROVE");
    }
    public static String label(TokenTxRecord r) {
        if(approval(r))return "Approve";
        if(r.isLiquidity())return "ADD".equals(r.liquidityAction)?"Add":"Remove";
        if("WRAP".equals(r.type) || "BKC".equals(r.fromAsset) && r.toAsset!=null && r.toAsset.contains("WBKC"))return "Wrap";
        if("UNWRAP".equals(r.type) || "BKC".equals(r.toAsset) && r.fromAsset!=null && r.fromAsset.contains("WBKC"))return "Unwrap";
        if("SEND".equals(r.type))return "Send";
        if("RECEIVE".equals(r.type))return "Receive";
        return "Swap";
    }
    public static String status(String state) {
        if("SUCCESS".equals(state))return "";
        if("UNKNOWN".equals(state))return "Outcome unknown. Do not send again.";
        if("SUBMITTING".equals(state))return "Sending. Do not send again.";
        if("PENDING".equals(state))return "Waiting for confirmation";
        if("FAILED".equals(state))return "Transaction failed";
        if("NOT_SENT".equals(state))return "Not sent";
        return state==null?"":"Manually reviewed";
    }
    public static String time(TokenTxRecord r,boolean shortDate) {
        return new SimpleDateFormat(shortDate?"MM-dd HH:mm":"yyyy-MM-dd HH:mm",Locale.US).format(new Date(r.timestampMs));
    }
    public static String amounts(TokenTxRecord r) {
        if(approval(r))return amount(r.amountDisplay)+" "+r.fromSymbol;
        if(r.isLiquidity())return amount(r.actualBkc)+" BKC + "+amount(r.actualMusdt)+" mUSDT → "+amount(r.actualLp)+" LPC";
        return amount(r.amountDisplay)+" "+r.fromSymbol+" → "+amount(r.actualOutput)+" "+r.toSymbol;
    }
}
