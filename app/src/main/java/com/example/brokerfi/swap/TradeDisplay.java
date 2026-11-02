package com.example.brokerfi.swap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Display-only rounding. Signing, quotes, limits and stored receipts always retain full units. */
public final class TradeDisplay {
    private TradeDisplay() {}
    public static String amount(BigInteger units,int decimals) {
        if (units==null) return "—";
        return decimal(new BigDecimal(units).movePointLeft(decimals));
    }
    public static String decimal(BigDecimal value) {
        if (value.signum()!=0 && value.abs().compareTo(new BigDecimal("0.000001"))<0)
            return value.signum()>0 ? "<0.000001" : ">-0.000001";
        DecimalFormat format=new DecimalFormat("#,##0.######",DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.DOWN); return format.format(value);
    }
    public static String rate(SwapQuoteMath.Quote q) {
        BigDecimal input=new BigDecimal(q.input).movePointLeft(q.from.decimals);
        BigDecimal output=new BigDecimal(q.output).movePointLeft(q.to.decimals);
        return "1 "+q.from.symbol+" ≈ "+decimal(output.divide(input,18,RoundingMode.DOWN))+" "+q.to.symbol;
    }
}
