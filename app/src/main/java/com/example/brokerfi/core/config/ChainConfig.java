package com.example.brokerfi.core.config;

/**
 * Centralized entrance to blockchain connection configuration. / 区块链连接配置集中入口。
 *
 * This manages RPC, dash gateway, block browser, contract address and other chain-related configurations; token. / 这里管理 RPC、dash gateway、区块浏览器、合约地址等链相关配置；token
 * The module only retains the token's own display and reading strategies. / 模块只保留 token 自身显示和读取策略。
 */
public final class ChainConfig {

    // Chain connection switch: retain the existing dash gateway operation mode and adjust it when RPC is standardized later.
    public static final boolean USE_LOCAL_BROKERCHAIN_NODE = false;
    public static final boolean USE_DASH_CHAIN_ONLY = true;
    public static final boolean USE_SIGNED_CHAIN_READ = true;

    public static final long LOCAL_CHAIN_ID = 4176L;

    // The main contract address is placed here, and the business code should no longer read from the Holder or local variables.
    public static final String MAIN_CONTRACT_ADDRESS =
            "0xF5c7A871DE8fa7A3393C528d57A519DcEB275f19";

    // The dash gateway goes to 443; the standard JSON-RPC node goes to 42515.
    public static final String DASH_GATEWAY_URL =
            ServerConfig.httpsUrl(ServerConfig.DASH_GATEWAY_PORT);

    public static final String CHAIN_JSON_RPC_URL =
            ServerConfig.httpUrl(ServerConfig.CHAIN_RPC_PORT);

    public static final String EXPLORER_TX_URL_TEMPLATE =
            DASH_GATEWAY_URL + "/tx/%s";

    /**
     * The gettx2 service is currently hung on the HTTP root path and cannot be spliced ​​simply by CHAIN_RPC_PORT. / gettx2 服务目前挂在 HTTP 根路径，不能简单按 CHAIN_RPC_PORT 拼接。
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
