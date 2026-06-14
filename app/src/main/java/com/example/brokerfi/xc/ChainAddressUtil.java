package com.example.brokerfi.xc;

import android.text.TextUtils;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ChainAddressUtil {

    private static final Pattern ADDRESS_PATTERN =
            Pattern.compile("^0x[0-9a-fA-F]{40}$");

    private ChainAddressUtil() {
    }

    public static String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }
        return address.trim().toLowerCase(Locale.US);
    }

    /** Returns the display form and guarantees a lowercase {@code 0x} prefix. */
    public static String displayAddress(String address) {
        String normalized = normalizeAddress(address);
        if (TextUtils.isEmpty(normalized)) {
            return "";
        }
        if (normalized.startsWith("0x")) {
            return normalized;
        }
        return "0x" + normalized;
    }

    public static boolean isValidAddress(String address) {
        return !TextUtils.isEmpty(address) && ADDRESS_PATTERN.matcher(address.trim()).matches();
    }

    /** Parses an Ethereum-style address from QR text, including {@code ethereum:} URIs. */
    public static String parseAddressFromQr(String contents) {
        if (TextUtils.isEmpty(contents)) {
            return null;
        }
        String s = contents.trim();
        if (s.regionMatches(true, 0, "ethereum:", 0, "ethereum:".length())) {
            s = s.substring("ethereum:".length());
            int query = s.indexOf('?');
            if (query >= 0) {
                s = s.substring(0, query);
            }
            int at = s.indexOf('@');
            if (at >= 0) {
                s = s.substring(at + 1);
            }
        }
        int idx = s.indexOf("0x");
        if (idx >= 0) {
            s = s.substring(idx);
            if (s.length() > 42) {
                s = s.substring(0, 42);
            }
        } else if (s.matches("(?i)[0-9a-f]{40}")) {
            s = "0x" + s;
        }
        String display = displayAddress(s);
        return isValidAddress(display) ? display : null;
    }
}
