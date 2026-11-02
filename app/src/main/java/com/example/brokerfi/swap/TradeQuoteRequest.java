package com.example.brokerfi.swap;

/** Main-thread read-only intent. A refresh may open a dialog, NEVER submit an operation. */
final class TradeQuoteRequest {
    private String action;
    void request(String action) { this.action = action; }
    void cancel() { action = null; }
    boolean pending() { return action != null; }
    String take() { String result = action; action = null; return result; }
    static boolean autoRefreshDue(long now, long lastCompleted) {
        return lastCompleted == 0 || now - lastCompleted >= 30_000L;
    }
}
