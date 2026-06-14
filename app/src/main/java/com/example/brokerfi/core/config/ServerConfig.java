package com.example.brokerfi.core.config;

/**
 * Central server host, protocol, and port configuration.
 *
 * Feature endpoints belong in {@link ApiConfig}; chain endpoints belong in
 * {@link ChainConfig}. Keep this class free of API paths so host and port
 * changes stay predictable.
 */
public final class ServerConfig {

    public static final String SERVER_HOST = "dash.broker-chain.com";

    public static final String HTTPS_SCHEME = "https";
    public static final String HTTP_SCHEME = "http";

    public static final int SERVER_PORT = 440;
    public static final int FEATURE_PORT = 444;
    public static final int NFT_DAO_PORT = 5000;
    public static final int COMMUNITY_PORT = 5001;
    public static final int CHAIN_RPC_PORT = 42515;
    public static final int DASH_GATEWAY_PORT = 443;

    private ServerConfig() {
    }

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
