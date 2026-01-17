package com.example.brokerfi.xc;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FormatUtil {
    public static String formatBalance(String balance) {
        try {
            double balanceValue = Double.parseDouble(balance);
            
            if (balanceValue >= 1000000000.0) {
                double formatted = balanceValue / 1000000000.0;
                return formatNumber(formatted, 6) + "B";
            } else if (balanceValue >= 1000000.0) {
                double formatted = balanceValue / 1000000.0;
                return formatNumber(formatted, 6) + "M";
            } else if (balanceValue >= 1.0) {
                if (balanceValue == Math.round(balanceValue)) {
                    return String.valueOf((long) balanceValue);
                } else {
                    return formatNumber(balanceValue, 10);
                }
            } else {
                return formatNumber(balanceValue, 13);
            }
        } catch (NumberFormatException e) {
            // If not a valid number, return original
            return balance;
        }
    }
    
    // Helper method, format number and remove trailing zeros
    private static String formatNumber(double number, int maxDecimals) {
        // Create DecimalFormat with max decimal places
        DecimalFormat df = new DecimalFormat("0." + "#".repeat(maxDecimals));
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(number);
    }

}
