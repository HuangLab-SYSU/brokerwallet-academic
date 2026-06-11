package com.example.brokerfi.token;

/**
 * BrokerChain token module: network and shared chain read settings.
 */
public final class TokenConfig {

    public static final boolean USE_LOCAL_BROKERCHAIN_NODE = false;
    public static final boolean USE_DASH_CHAIN_ONLY = true;
    public static final boolean USE_SIGNED_CHAIN_READ = true;

    public static final String DASH_GATEWAY_HOST = "dash.broker-chain.com";
    public static final int DASH_GATEWAY_PORT = 443;
    public static final String DASH_GATEWAY_URL =
            "https://" + DASH_GATEWAY_HOST + ":" + DASH_GATEWAY_PORT;

    public static final String CHAIN_JSON_RPC_URL =
            "http://" + DASH_GATEWAY_HOST + ":42515";

    public static final long DASH_ETH_CALL_TIMEOUT_MS = 25_000L;
    /** 浣欓 balanceOf 涓撶敤瓒呮椂锛涘け璐ュ悗浼氶噸璇曟垨璧?RPC 鍏滃簳銆?*/
    public static final long BALANCE_ETH_CALL_TIMEOUT_MS = 12_000L;
    public static final int BALANCE_READ_MAX_ATTEMPTS = 2;
    public static final long DEFAULT_CHAIN_READ_TIMEOUT_MS = 20_000L;

    public static final String EXPLORER_URL_TEMPLATE =
            "https://" + DASH_GATEWAY_HOST + "/tx/%s";

    /**
     * dash 浜ゆ槗绱㈠紩 {@code gettx2}銆傚綋鍓嶄粎 {@code http://host/gettx2} 鍙敤锛?     * {@code https://host:443/gettx2} 杩斿洖 404锛屽緟 dash 鏀寔鍚庡啀鍒?HTTPS銆?     */
    public static final String DASH_GETTX2_URL =
            "http://" + DASH_GATEWAY_HOST + "/gettx2?acc=";

    public static final long LOCAL_CHAIN_ID = 4176L;

    public static final String NATIVE_SYMBOL = "BKC";
    public static final int DEFAULT_TOKEN_DECIMALS = 18;
    /** @deprecated use {@link #DEFAULT_TOKEN_DECIMALS} */
    public static final int TOKEN_DECIMALS = DEFAULT_TOKEN_DECIMALS;

    public static final String DEMO_GAS_LIMIT = "1500000";
    public static final String DEMO_GAS_PRICE = "1000000000";

    private TokenConfig() {
    }

    public static String getLocalChainRpcUrl() {
        if (useDashChainOnly()) {
            return CHAIN_JSON_RPC_URL;
        }
        return "http://" + DASH_GATEWAY_HOST + ":" + DASH_GATEWAY_PORT;
    }

    public static String getDashGatewayRootUrl() {
        return DASH_GATEWAY_URL;
    }

    public static String getDashGatewayPostUrl(String methodPath) {
        String base = DASH_GATEWAY_URL.endsWith("/") ? DASH_GATEWAY_URL : DASH_GATEWAY_URL + "/";
        String path = methodPath.startsWith("/") ? methodPath.substring(1) : methodPath;
        return base + path;
    }

    /** {@code gettx2?acc=} 瀹屾暣 URL锛寋@code account} 鍙负甯︽垨涓嶅甫 {@code 0x} 鐨勫湴鍧€銆?*/
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


