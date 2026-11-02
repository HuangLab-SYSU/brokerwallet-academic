package com.example.brokerfi.swap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.web3j.crypto.Hash;
import java.math.BigInteger;

/** A missing/malformed receipt is pending, never a successful estimated amount. */
public final class SwapReceipt {
    public static final String SWAP_TOPIC = Hash.sha3String("Swap(address,uint256,uint256,uint256,uint256,address)");
    public final String status;
    public final BigInteger actualOutput;
    public final BigInteger gasCost;
    private SwapReceipt(String state, BigInteger output, BigInteger gas) {
        status = state; actualOutput = output; gasCost = gas;
    }
    public static SwapReceipt parse(String raw, String expectedHash, BrokerSwapConfig cfg,
                                    SwapAsset from, SwapAsset to, BigInteger input, String wallet) {
        try {
            JsonObject envelope = JsonParser.parseString(raw).getAsJsonObject();
            if (envelope.has("error") || !envelope.has("result") || !envelope.get("result").isJsonObject())
                return pending();
            JsonObject r = envelope.getAsJsonObject("result");
            if (!expectedHash.equalsIgnoreCase(r.get("transactionHash").getAsString()) || !r.has("blockNumber")
                    || r.get("blockNumber").isJsonNull() || !r.has("status")) return pending();
            String state = r.get("status").getAsString();
            BigInteger gas = null;
            if (r.has("gasUsed") && r.has("effectiveGasPrice"))
                gas = quantity(r.get("gasUsed").getAsString()).multiply(quantity(r.get("effectiveGasPrice").getAsString()));
            if ("0x0".equalsIgnoreCase(state)) return new SwapReceipt("FAILED", null, gas);
            if (!"0x1".equalsIgnoreCase(state)) return pending();
            if (!SwapAsset.isAmm(from, to)) return new SwapReceipt("SUCCESS", input, gas);
            BigInteger actual = null;
            JsonArray logs = r.has("logs") && r.get("logs").isJsonArray() ? r.getAsJsonArray("logs") : new JsonArray();
            for (com.google.gson.JsonElement element : logs) {
                try {
                    JsonObject log = element.getAsJsonObject();
                    if (!cfg.pair().equalsIgnoreCase(log.get("address").getAsString())) continue;
                    JsonArray topics = log.getAsJsonArray("topics");
                    String destination = to == SwapAsset.BKC ? cfg.router() : BrokerSwapConfig.walletAddress(wallet);
                    if (topics.size() != 3 || !SWAP_TOPIC.equalsIgnoreCase(topics.get(0).getAsString())
                            || !cfg.router().equalsIgnoreCase(BrokerSwapAbi.address(topics.get(1).getAsString()))
                            || !destination.equalsIgnoreCase(BrokerSwapAbi.address(topics.get(2).getAsString()))) continue;
                    BigInteger[] amounts = BrokerSwapAbi.words(log.get("data").getAsString(), 4);
                    boolean inputMusdt = from == SwapAsset.MUSDT;
                    boolean musdt0 = cfg.musdt().compareTo(cfg.wbkc()) < 0;
                    int inIndex = inputMusdt == musdt0 ? 0 : 1;
                    int outIndex = inIndex == 0 ? 3 : 2;
                    if (!amounts[inIndex].equals(input) || amounts[1-inIndex].signum() != 0
                            || amounts[inIndex+2].signum() != 0 || amounts[outIndex].signum() <= 0) continue;
                    // The fixed-pair Router emits exactly one matching Swap. Ambiguity is not an estimate.
                    if (actual != null) return new SwapReceipt("SUCCESS", null, gas);
                    actual = amounts[outIndex];
                } catch (RuntimeException ignored) { }
            }
            return new SwapReceipt("SUCCESS", actual, gas);
        } catch (RuntimeException e) { return pending(); }
    }
    public static BigInteger quantity(String hex) {
        if (hex == null || !hex.matches("0x[0-9a-fA-F]+")) throw new IllegalArgumentException("Invalid RPC quantity");
        return new BigInteger(hex.substring(2), 16);
    }
    public static boolean validHash(String hash) { return hash != null && hash.matches("0x[0-9a-fA-F]{64}"); }
    /** Dash deployments may return plain hash text or a JSON-RPC result; neither allows arbitrary IDs. */
    public static String sentHash(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("{")) {
            try {
                JsonObject envelope = JsonParser.parseString(value).getAsJsonObject();
                if (envelope.has("error")) throw new IllegalArgumentException("Send rejected");
                value = envelope.get("result").getAsString();
            } catch (RuntimeException e) { throw new IllegalArgumentException("No valid transaction hash"); }
        }
        if (value.matches("[0-9a-fA-F]{64}")) value = "0x" + value;
        if (!validHash(value)) throw new IllegalArgumentException("No valid transaction hash");
        return value.toLowerCase(java.util.Locale.ROOT);
    }
    public static SwapReceipt pending() { return new SwapReceipt("PENDING", null, null); }
}
