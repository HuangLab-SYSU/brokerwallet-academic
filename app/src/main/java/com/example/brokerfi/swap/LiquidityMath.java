package com.example.brokerfi.swap;

import java.math.BigInteger;

/** Integer arithmetic ported from Du's BrokerLiquidityRouter.quoteAdd/RemoveLiquidity. */
public final class LiquidityMath {
    private static final BigInteger BPS = BigInteger.valueOf(10000);
    private LiquidityMath() {}
    public static BigInteger minimum(BigInteger amount) {
        return amount.multiply(BigInteger.valueOf(9950)).divide(BPS);
    }
    public static final class AddQuote {
        public final BigInteger musdt, bkc, lp;
        AddQuote(BigInteger m, BigInteger b, BigInteger l) { musdt=m; bkc=b; lp=l; }
    }
    public static AddQuote add(SwapPoolSnapshot pool, BigInteger supply, BigInteger desiredMusdt, BigInteger desiredBkc) {
        positive(supply); positive(desiredMusdt); positive(desiredBkc);
        BigInteger b = desiredMusdt.multiply(pool.wbkcReserve).divide(pool.musdtReserve), m=desiredMusdt;
        if (b.compareTo(desiredBkc)>0) { b=desiredBkc; m=b.multiply(pool.musdtReserve).divide(pool.wbkcReserve); }
        positive(b); positive(m);
        BigInteger lp=m.multiply(supply).divide(pool.musdtReserve).min(b.multiply(supply).divide(pool.wbkcReserve));
        positive(lp);
        if (minimum(m).signum()==0 || minimum(b).signum()==0 || minimum(lp).signum()==0)
            throw new IllegalArgumentException("Liquidity amount is too small");
        return new AddQuote(m,b,lp);
    }
    public static BigInteger[] remove(SwapPoolSnapshot pool, BigInteger supply, BigInteger lp) {
        positive(supply); positive(lp);
        if (lp.compareTo(supply)>0) throw new IllegalArgumentException("LP exceeds total supply");
        BigInteger m=lp.multiply(pool.musdtReserve).divide(supply), b=lp.multiply(pool.wbkcReserve).divide(supply);
        positive(m); positive(b);
        return new BigInteger[]{m,b};
    }
    public static BigInteger portion(BigInteger balance, int percent) {
        if (balance==null || balance.signum()<0 || percent<0 || percent>100) throw new IllegalArgumentException("Invalid LP portion");
        return balance.multiply(BigInteger.valueOf(percent)).divide(BigInteger.valueOf(100));
    }
    public static BigInteger counterpart(SwapPoolSnapshot pool, BigInteger input, boolean fromBkc) {
        positive(input);
        return input.multiply(fromBkc ? pool.musdtReserve : pool.wbkcReserve)
                .divide(fromBkc ? pool.wbkcReserve : pool.musdtReserve);
    }
    public static BigInteger sqrt(BigInteger value) {
        if (value.signum()<0) throw new IllegalArgumentException("Negative product");
        if (value.signum()==0) return BigInteger.ZERO;
        BigInteger x=BigInteger.ONE.shiftLeft((value.bitLength()+1)/2), y=x.add(value.divide(x)).shiftRight(1);
        while (y.compareTo(x)<0) { x=y; y=x.add(value.divide(x)).shiftRight(1); }
        return x;
    }
    /** Growth of invariant per LP since observation. An estimate, not separately claimable income. */
    public static BigInteger estimatedFeesMusdt(SwapPoolSnapshot pool, BigInteger supply, BigInteger ownedLp,
                                              BigInteger observedRoot, BigInteger observedSupply) {
        positive(supply); positive(observedSupply); positive(observedRoot);
        if (ownedLp.signum()<0 || ownedLp.compareTo(supply)>0) throw new IllegalArgumentException("Invalid LP ownership");
        BigInteger root=sqrt(pool.musdtReserve.multiply(pool.wbkcReserve));
        BigInteger growth=root.multiply(observedSupply).subtract(observedRoot.multiply(supply));
        if (growth.signum()<=0) return BigInteger.ZERO;
        return pool.musdtReserve.multiply(ownedLp).multiply(BigInteger.valueOf(2)).multiply(growth)
                .divide(supply.multiply(root).multiply(observedSupply));
    }
    private static void positive(BigInteger amount) {
        if (amount==null || amount.signum()<=0) throw new IllegalArgumentException("Invalid liquidity amount");
    }
}
