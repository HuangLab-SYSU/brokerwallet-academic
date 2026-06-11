package com.example.brokerfi.token;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.example.brokerfi.token.TokenConfig;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class TokenAmountUtil {

    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private static final BigDecimal BILLION = new BigDecimal("1000000000");
    private static final BigDecimal TRILLION = new BigDecimal("1000000000000");
    private static final BigDecimal TINY = new BigDecimal("0.0001");

    private TokenAmountUtil() {
    }

    /**
     * Tokens 鐣岄潰灞曠ず锛氭寜鏁伴噺绾ц嚜閫傚簲灏忔暟浣嶄笌缂╁啓锛岄伩鍏嶄綑棰濆崰婊″睆骞曘€?
     * <ul>
     *   <li>鈮?100 涓囷細K / M / B / T 缂╁啓锛堟渶澶?2 浣嶅皬鏁帮級</li>
     *   <li>鈮?1,000锛氬崈鍒嗕綅 + 鏈€澶?2 浣嶅皬鏁?/li>
     *   <li>鈮?1锛氭渶澶?2 浣嶅皬鏁?/li>
     *   <li>鈮?0.0001锛氭渶澶?4 浣嶅皬鏁?/li>
     *   <li>鏇村皬锛氭渶澶?6 浣嶅皬鏁?/li>
     * </ul>
     */
    public static String formatDisplayAmount(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) {
            return "0";
        }
        try {
            return formatDecimal(new BigDecimal(raw.trim()));
        } catch (Exception e) {
            return raw.trim();
        }
    }

    public static String formatDisplayAmount(@Nullable BigInteger chainAmount) {
        return formatDisplayAmount(fromChainUnits(chainAmount));
    }

    public static String formatDisplayAmount(@Nullable BigInteger chainAmount, int decimals) {
        return formatDisplayAmount(fromChainUnits(chainAmount, decimals));
    }

    private static String formatDecimal(BigDecimal value) {
        if (value.signum() == 0) {
            return "0";
        }
        boolean negative = value.signum() < 0;
        BigDecimal abs = value.abs();

        String body;
        if (abs.compareTo(MILLION) >= 0) {
            body = formatAbbreviated(abs);
        } else if (abs.compareTo(THOUSAND) >= 0) {
            body = formatGrouped(abs, 2);
        } else if (abs.compareTo(ONE) >= 0) {
            body = formatScaledPlain(abs, 2);
        } else if (abs.compareTo(TINY) >= 0) {
            body = formatScaledPlain(abs, 4);
        } else {
            body = formatScaledPlain(abs, 6);
        }
        return negative ? "-" + body : body;
    }

    private static String formatAbbreviated(BigDecimal abs) {
        if (abs.compareTo(TRILLION) >= 0) {
            return formatScaledPlain(abs.movePointLeft(12), 2) + "T";
        }
        if (abs.compareTo(BILLION) >= 0) {
            return formatScaledPlain(abs.movePointLeft(9), 2) + "B";
        }
        if (abs.compareTo(MILLION) >= 0) {
            return formatScaledPlain(abs.movePointLeft(6), 2) + "M";
        }
        return formatScaledPlain(abs.movePointLeft(3), 2) + "K";
    }

    private static String formatGrouped(BigDecimal abs, int maxFractionDigits) {
        DecimalFormat format = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(maxFractionDigits);
        return format.format(abs);
    }

    private static String formatScaledPlain(BigDecimal abs, int maxFractionDigits) {
        return abs.setScale(maxFractionDigits, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    @Nullable
    public static BigInteger toChainUnits(@Nullable String humanAmount) {
        return toChainUnits(humanAmount, TokenConfig.TOKEN_DECIMALS);
    }

    @Nullable
    public static BigInteger toChainUnits(@Nullable String humanAmount, int decimals) {
        if (TextUtils.isEmpty(humanAmount)) {
            return null;
        }
        String raw = humanAmount.trim();
        try {
            if (decimals <= 0) {
                return new BigInteger(raw);
            }
            BigDecimal value = new BigDecimal(raw);
            if (value.signum() <= 0) {
                return null;
            }
            return value.movePointRight(decimals).toBigIntegerExact();
        } catch (Exception e) {
            return null;
        }
    }

    public static String fromChainUnits(@Nullable BigInteger chainAmount) {
        return fromChainUnits(chainAmount, TokenConfig.TOKEN_DECIMALS);
    }

    public static String fromChainUnits(@Nullable BigInteger chainAmount, int decimals) {
        if (chainAmount == null) {
            return "0";
        }
        if (decimals <= 0) {
            return chainAmount.toString();
        }
        return new BigDecimal(chainAmount)
                .movePointLeft(decimals)
                .stripTrailingZeros()
                .toPlainString();
    }
}


