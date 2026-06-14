package com.example.brokerfi.core.config;

/**
 * 服务器基础配置集中入口。
 *
 * 这里只放域名、协议、端口和 URL 拼接方法；具体业务 API 放在
 * {@link ApiConfig}，链/RPC 配置放在 {@link ChainConfig}。后续更换
 * 域名或端口时，优先改这里，避免业务代码到处硬编码完整 URL。
 */
public final class ServerConfig {

    // 生产环境统一使用域名，不在业务代码里直接写 IP。
    public static final String SERVER_HOST = "dash.broker-chain.com";

    public static final String HTTPS_SCHEME = "https";
    public static final String HTTP_SCHEME = "http";

    // 各后端服务端口按职责区分，调用处只引用配置，不手动拼端口。
    public static final int SERVER_PORT = 440;
    public static final int FEATURE_PORT = 444;
    public static final int NFT_DAO_PORT = 5000;
    public static final int COMMUNITY_PORT = 5001;
    public static final int CHAIN_RPC_PORT = 42515;
    public static final int DASH_GATEWAY_PORT = 443;

    private ServerConfig() {
    }

    // 统一拼接 URL，避免调用处出现 "https://host:port" 这类硬编码。
    public static String httpsUrl(int port) {
        return buildUrl(HTTPS_SCHEME, SERVER_HOST, port);
    }

    public static String httpUrl(int port) {
        return buildUrl(HTTP_SCHEME, SERVER_HOST, port);
    }

    public static String httpRootUrl() {
        return HTTP_SCHEME + "://" + SERVER_HOST;
    }

    public static String buildUrl(String scheme, String host, int port) {
        return scheme + "://" + host + ":" + port;
    }

    public static String appendPath(String baseUrl, String path) {
        if (path == null || path.isEmpty()) {
            return baseUrl;
        }
        boolean baseEndsWithSlash = baseUrl.endsWith("/");
        boolean pathStartsWithSlash = path.startsWith("/");
        if (baseEndsWithSlash && pathStartsWithSlash) {
            return baseUrl + path.substring(1);
        }
        if (!baseEndsWithSlash && !pathStartsWithSlash) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }
}
