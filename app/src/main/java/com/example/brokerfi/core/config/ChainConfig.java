package com.example.brokerfi.core.config;

/**
 * Central blockchain endpoint and contract configuration.
 */
public final class ChainConfig {

    public static final boolean USE_LOCAL_BROKERCHAIN_NODE = false;
    public static final boolean USE_DASH_CHAIN_ONLY = true;
    public static final boolean USE_SIGNED_CHAIN_READ = true;

    public static final long LOCAL_CHAIN_ID = 4176L;

    public static final String MAIN_CONTRACT_ADDRESS =
            "0xF5c7A871DE8fa7A3393C528d57A519DcEB275f19";

    public static final String DASH_GATEWAY_URL =
            ServerConfig.httpsUrl(ServerConfig.DASH_GATEWAY_PORT);

    public static final String CHAIN_JSON_RPC_URL =
            ServerConfig.httpUrl(ServerConfig.CHAIN_RPC_PORT);

    public static final String EXPLORER_TX_URL_TEMPLATE =
            DASH_GATEWAY_URL + "/tx/%s";

    /**
     * The transaction index service currently listens on the host root HTTP
     * port rather than an explicit service port.
     */
    public static final String DASH_GETTX2_URL =
            ServerConfig.httpRootUrl() + "/gettx2?acc=";

    private ChainConfig() {
    }

    public static String getLocalChainRpcUrl() {
        if (useDashChainOnly()) {
            return CHAIN_JSON_RPC_URL;
        }
        return ServerConfig.httpUrl(ServerConfig.DASH_GATEWAY_PORT);
    }

    public static String getReadOnlyRpcUrl() {
        return CHAIN_JSON_RPC_URL;
    }

    public static String getDashGatewayPostUrl(String methodPath) {
        return ServerConfig.appendPath(DASH_GATEWAY_URL, methodPath);
    }

    public static String getGetTx2AccountUrl(String account) {
        if (account == null || account.trim().isEmpty()) {
            return DASH_GETTX2_URL;
        }
        String hex = account.trim();
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        return DASH_GETTX2_URL + hex;
    }

    public static boolean useDashChainOnly() {
        return USE_DASH_CHAIN_ONLY || !USE_LOCAL_BROKERCHAIN_NODE;
    }

    public static boolean useLocalBrokerChainNode() {
        return USE_LOCAL_BROKERCHAIN_NODE;
    }

    public static boolean useSignedChainRead() {
        return USE_SIGNED_CHAIN_READ;
    }
}
