package com.example.brokerfi.swap;

import com.example.brokerfi.token.TokenTxRecord;

/** One priority order for the banner and buttons; stale previews never mask an operation. */
final class TradeStatusText {
    static String operation(boolean confirming, boolean preparing, boolean active, TokenTxRecord record) {
        if (preparing) return "Checking before sending. Not sent yet.";
        if (active) return record != null && SwapReceipt.validHash(record.txHash)
                ? "Waiting for confirmation. Do not send again." : "Sending. Do not send again.";
        if (confirming) return "Awaiting your confirmation. Not sent yet.";
        if (record != null) return SwapReceipt.validHash(record.txHash)
                ? "Waiting for confirmation. Tap to check the original transaction (no resend)."
                : "Outcome unknown. Tap to review recovery options. No automatic resend.";
        return "";
    }
    private TradeStatusText() {}
}
