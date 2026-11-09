package com.example.brokerfi.swap;

import android.content.Context;
import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Verified fixed-pair deployment; does not change the wallet's global chain configuration. */
public final class BrokerSwapConfig {
    private long chainId;
    private String factory, router, pair, wbkc, musdt, routerCodeHash;
    private String liquidityRouter, liquidityRouterCodeHash;

    public static BrokerSwapConfig load(Context context) throws Exception {
        try (InputStream in = context.getAssets().open("broker_swap_deployment.json")) {
            return parse(new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
                    BrokerSwapConfig.class));
        }
    }

    public static BrokerSwapConfig fromJson(String json) {
        return parse(new Gson().fromJson(json, BrokerSwapConfig.class));
    }

    private static BrokerSwapConfig parse(BrokerSwapConfig config) {
        if (config == null || config.chainId <= 0) throw new IllegalArgumentException("Invalid swap network");
        config.factory = address(config.factory);
        config.router = address(config.router);
        config.pair = address(config.pair);
        config.wbkc = address(config.wbkc);
        config.musdt = address(config.musdt);
        if (config.wbkc.equals(config.musdt)) throw new IllegalArgumentException("Identical swap assets");
        if (config.routerCodeHash == null || !config.routerCodeHash.matches("0x[0-9a-fA-F]{64}"))
            throw new IllegalArgumentException("Missing router code fingerprint");
        config.routerCodeHash = config.routerCodeHash.toLowerCase(Locale.US);
        if (config.liquidityRouter != null || config.liquidityRouterCodeHash != null) {
            config.liquidityRouter = address(config.liquidityRouter);
            if (config.liquidityRouterCodeHash == null || !config.liquidityRouterCodeHash.matches("0x[0-9a-fA-F]{64}"))
                throw new IllegalArgumentException("Missing liquidity router fingerprint");
            config.liquidityRouterCodeHash = config.liquidityRouterCodeHash.toLowerCase(Locale.US);
            if (config.liquidityRouter.equals(config.router)) throw new IllegalArgumentException("Liquidity and swap routers differ");
        }
        return config;
    }

    public static String address(String value) {
        if (value == null || !value.matches("0x[0-9a-fA-F]{40}")
                || value.matches("0x0{40}")) throw new IllegalArgumentException("Invalid contract address");
        return value.toLowerCase(Locale.US);
    }

    /** The legacy wallet/history uses unprefixed IDs; RPC, ABI and event topics use 0x addresses. */
    public static String walletId(String value) {
        if (value == null) return "";
        String id = value.trim().toLowerCase(Locale.US);
        if (id.startsWith("0x")) id = id.substring(2);
        return id.matches("[0-9a-f]{40}") ? id : "";
    }
    public static String walletAddress(String value) {
        String id = walletId(value);
        if (id.isEmpty()) throw new IllegalArgumentException("Invalid wallet address");
        return "0x" + id;
    }

    public long chainId() { return chainId; }
    public String factory() { return factory; }
    public String router() { return router; }
    public String pair() { return pair; }
    public String wbkc() { return wbkc; }
    public String musdt() { return musdt; }
    public String routerCodeHash() { return routerCodeHash; }
    public boolean hasLiquidity() { return liquidityRouter != null; }
    public String liquidityRouter() {
        if (!hasLiquidity()) throw new IllegalStateException("Liquidity deployment is not configured");
        return liquidityRouter;
    }
    public String liquidityRouterCodeHash() { return liquidityRouterCodeHash; }

    public String tokenAddress(SwapAsset asset, String originalWrapped) {
        switch (asset) {
            case ORIGINAL_WBKC: return address(originalWrapped);
            case SWAP_WBKC: return wbkc;
            case MUSDT: return musdt;
            default: return "";
        }
    }
}
