package com.example.brokerfi.swap;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

/** ABI for the deployed MiniV2, not the obsolete demo Router. */
public final class BrokerSwapAbi {
    private BrokerSwapAbi() {}
    public static String call(String name, Type... args) {
        return FunctionEncoder.encode(new Function(name, Arrays.asList(args), Collections.emptyList()));
    }
    public static String swap(BrokerSwapConfig cfg, SwapQuoteMath.Quote q, String recipient, long deadline) {
        Address to = new Address(recipient);
        Uint256 expiry = new Uint256(BigInteger.valueOf(deadline));
        if (q.from == SwapAsset.BKC)
            return call("swapExactBkcForMusdt", new Uint256(q.output), new Uint256(q.minimum), to, expiry);
        if (q.from == SwapAsset.MUSDT)
            return call(q.to == SwapAsset.BKC ? "swapExactMusdtForBkc" : "swapExactMusdtForWbkc",
                    new Uint256(q.input), new Uint256(q.output), new Uint256(q.minimum), to, expiry);
        if (q.from == SwapAsset.SWAP_WBKC && q.to == SwapAsset.MUSDT)
            return call("swapExactTokensForTokens", new Uint256(q.input), new Uint256(q.minimum),
                    new DynamicArray<>(Address.class, new Address(cfg.wbkc()), new Address(cfg.musdt())), to, expiry);
        throw new IllegalArgumentException("Unsupported swap route");
    }
    public static BigInteger[] words(String hex, int count) {
        if (hex == null || !hex.matches("0x[0-9a-fA-F]{" + (64 * count) + "}"))
            throw new IllegalArgumentException("Invalid ABI response");
        BigInteger[] result = new BigInteger[count];
        for (int i = 0; i < count; i++) result[i] = new BigInteger(hex.substring(2 + 64*i, 66 + 64*i), 16);
        return result;
    }
    public static String address(String hex) {
        BigInteger value = words(hex, 1)[0];
        if (value.signum() == 0 || value.bitLength() > 160) throw new IllegalArgumentException("Invalid address response");
        return "0x" + String.format(java.util.Locale.ROOT, "%040x", value);
    }
}
