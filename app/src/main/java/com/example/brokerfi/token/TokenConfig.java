package com.example.brokerfi.token;

import com.example.brokerfi.core.config.ChainConfig;

/**
 * BrokerChain token module: token display and read timing settings.
 */
public final class TokenConfig {

    /** @deprecated use {@link ChainConfig#USE_LOCAL_BROKERCHAIN_NODE}. */
    @Deprecated
    public static final boolean USE_LOCAL_BROKERCHAIN_NODE = ChainConfig.USE_LOCAL_BROKERCHAIN_NODE;
    /** @deprecated use {@link ChainConfig#USE_DASH_CHAIN_ONLY}. */
    @Deprecated
    public static final boolean USE_DASH_CHAIN_ONLY = ChainConfig.USE_DASH_CHAIN_ONLY;
    /** @deprecated use {@link ChainConfig#USE_SIGNED_CHAIN_READ}. */
    @Deprecated
    public static final boolean USE_SIGNED_CHAIN_READ = ChainConfig.USE_SIGNED_CHAIN_READ;

    /** @deprecated use {@link ChainConfig#DASH_GATEWAY_URL}. */
    @Deprecated
    public static final String DASH_GATEWAY_URL = ChainConfig.DASH_GATEWAY_URL;

    /** @deprecated use {@link ChainConfig#CHAIN_JSON_RPC_URL}. */
    @Deprecated
    public static final String CHAIN_JSON_RPC_URL = ChainConfig.CHAIN_JSON_RPC_URL;

    public static final long DASH_ETH_CALL_TIMEOUT_MS = 25_000L;
    /** Dedicated timeout for {@code balanceOf}; callers may retry or fall back to raw RPC. */
    public static final long BALANCE_ETH_CALL_TIMEOUT_MS = 12_000L;
    public static final int BALANCE_READ_MAX_ATTEMPTS = 2;
    public static final long DEFAULT_CHAIN_READ_TIMEOUT_MS = 20_000L;

    /** @deprecated use {@link ChainConfig#EXPLORER_TX_URL_TEMPLATE}. */
    @Deprecated
    public static final String EXPLORER_URL_TEMPLATE = ChainConfig.EXPLORER_TX_URL_TEMPLATE;

    /** @deprecated use {@link ChainConfig#DASH_GETTX2_URL}. */
    @Deprecated
    public static final String DASH_GETTX2_URL = ChainConfig.DASH_GETTX2_URL;

    /** @deprecated use {@link ChainConfig#LOCAL_CHAIN_ID}. */
    @Deprecated
    public static final long LOCAL_CHAIN_ID = ChainConfig.LOCAL_CHAIN_ID;

    public static final String NATIVE_SYMBOL = "BKC";
    public static final int DEFAULT_TOKEN_DECIMALS = 18;
    /** @deprecated use {@link #DEFAULT_TOKEN_DECIMALS} */
    public static final int TOKEN_DECIMALS = DEFAULT_TOKEN_DECIMALS;

    public static final String DEMO_GAS_LIMIT = "1500000";
    public static final String DEMO_GAS_PRICE = "1000000000";

    private TokenConfig() {
    }

    /** @deprecated use {@link ChainConfig#getLocalChainRpcUrl()}. */
    @Deprecated
    public static String getLocalChainRpcUrl() {
        return ChainConfig.getLocalChainRpcUrl();
    }

    /** @deprecated use {@link ChainConfig#DASH_GATEWAY_URL}. */
    @Deprecated
    public static String getDashGatewayRootUrl() {
        return ChainConfig.DASH_GATEWAY_URL;
    }

    /** @deprecated use {@link ChainConfig#getDashGatewayPostUrl(String)}. */
    @Deprecated
    public static String getDashGatewayPostUrl(String methodPath) {
        return ChainConfig.getDashGatewayPostUrl(methodPath);
    }

    /** Builds the {@code gettx2?acc=} URL. {@code account} may include or omit {@code 0x}. */
    /** @deprecated use {@link ChainConfig#getGetTx2AccountUrl(String)}. */
    @Deprecated
    public static String getGetTx2AccountUrl(String account) {
        return ChainConfig.getGetTx2AccountUrl(account);
    }

    /** @deprecated use {@link ChainConfig#useDashChainOnly()}. */
    @Deprecated
    public static boolean useDashChainOnly() {
        return ChainConfig.useDashChainOnly();
    }

    /** @deprecated use {@link ChainConfig#useLocalBrokerChainNode()}. */
    @Deprecated
    public static boolean useLocalBrokerChainNode() {
        return ChainConfig.useLocalBrokerChainNode();
    }

    /** @deprecated use {@link ChainConfig#useSignedChainRead()}. */
    @Deprecated
    public static boolean useSignedChainRead() {
        return ChainConfig.useSignedChainRead();
    }
}
