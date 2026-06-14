package com.example.brokerfi.core.config;

/**
 * 区块链连接配置集中入口。
 *
 * 这里管理 RPC、dash gateway、区块浏览器、合约地址等链相关配置；token
 * 模块只保留 token 自身显示和读取策略。
 */
public final class ChainConfig {

    // 链连接开关：保留现有 dash gateway 运行方式，后续 RPC 标准化时再调整。
    public static final boolean USE_LOCAL_BROKERCHAIN_NODE = false;
    public static final boolean USE_DASH_CHAIN_ONLY = true;
    public static final boolean USE_SIGNED_CHAIN_READ = true;

    public static final long LOCAL_CHAIN_ID = 4176L;

    // 主合约地址统一放这里，业务代码不要再从 Holder 或局部变量读取。
    public static final String MAIN_CONTRACT_ADDRESS =
            "0xF5c7A871DE8fa7A3393C528d57A519DcEB275f19";

    // dash gateway 走 443；标准 JSON-RPC 节点走 42515。
    public static final String DASH_GATEWAY_URL =
            ServerConfig.httpsUrl(ServerConfig.DASH_GATEWAY_PORT);

    public static final String CHAIN_JSON_RPC_URL =
            ServerConfig.httpUrl(ServerConfig.CHAIN_RPC_PORT);

    public static final String EXPLORER_TX_URL_TEMPLATE =
            DASH_GATEWAY_URL + "/tx/%s";

    /**
     * gettx2 服务目前挂在 HTTP 根路径，不能简单按 CHAIN_RPC_PORT 拼接。
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
