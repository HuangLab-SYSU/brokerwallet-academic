package com.example.brokerfi.swap;

/** Identity is an enum + configured address, never a token symbol supplied by another contract. */
public enum SwapAsset {
    BKC("BKC", 18), ORIGINAL_WBKC("wBKC", 18), SWAP_WBKC("wBKC (Swap)", 18), MUSDT("mUSDT", 6);
    public final String symbol;
    public final int decimals;
    SwapAsset(String symbol, int decimals) { this.symbol = symbol; this.decimals = decimals; }
    public boolean isWrapped() { return this == ORIGINAL_WBKC || this == SWAP_WBKC; }
    public static boolean isWrap(SwapAsset from, SwapAsset to) {
        return from == BKC && to != null && to.isWrapped();
    }
    public static boolean isUnwrap(SwapAsset from, SwapAsset to) { return isWrap(to, from); }
    public static boolean isAmm(SwapAsset from, SwapAsset to) {
        return from != null && to != null && ((from == MUSDT && (to == BKC || to == SWAP_WBKC))
                || (to == MUSDT && (from == BKC || from == SWAP_WBKC)));
    }
}
