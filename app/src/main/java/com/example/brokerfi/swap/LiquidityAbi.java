package com.example.brokerfi.swap;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import java.math.BigInteger;

/** Reuses the deployed atomic Liquidity Router ABI; never sends both assets separately. */
public final class LiquidityAbi {
    private LiquidityAbi() {}
    public static String add(BigInteger desiredMusdt, BigInteger minMusdt, BigInteger minBkc,
                             BigInteger minLp, String wallet, long deadline) {
        return BrokerSwapAbi.call("addLiquidityBKC", new Uint256(desiredMusdt), new Uint256(minMusdt),
                new Uint256(minBkc), new Uint256(minLp), new Address(BrokerSwapConfig.walletAddress(wallet)),
                new Uint256(BigInteger.valueOf(deadline)));
    }
    public static String remove(BigInteger lp, BigInteger minMusdt, BigInteger minBkc, String wallet, long deadline) {
        return BrokerSwapAbi.call("removeLiquidityBKC", new Uint256(lp), new Uint256(minMusdt),
                new Uint256(minBkc), new Address(BrokerSwapConfig.walletAddress(wallet)), new Uint256(BigInteger.valueOf(deadline)));
    }
}
