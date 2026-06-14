package com.example.brokerfi.core.config;

/**
 * Central backend API endpoint configuration.
 *
 * Server host and ports are defined in {@link ServerConfig}. Keep concrete
 * feature paths here so Activities, adapters, and utilities never hardcode
 * service URLs.
 */
public final class ApiConfig {

    public static final String BASE_API_URL = ServerConfig.httpsUrl(ServerConfig.SERVER_PORT);
    /** @deprecated use {@link #BASE_API_URL}. */
    @Deprecated
    public static final String BASE_URL = BASE_API_URL;

    public static final String FEATURE_URL = ServerConfig.httpsUrl(ServerConfig.FEATURE_PORT);
    public static final String NFT_DAO_URL = ServerConfig.httpUrl(ServerConfig.NFT_DAO_PORT);
    public static final String NFT_DAO_API_URL = NFT_DAO_URL + "/api";
    public static final String COMMUNITY_URL = ServerConfig.httpUrl(ServerConfig.COMMUNITY_PORT);
    /** @deprecated use {@link #COMMUNITY_URL}. */
    @Deprecated
    public static final String BASE_URL_HTTP = COMMUNITY_URL;

    public static final String API_ABOUT_APP_VERSION = FEATURE_URL + "/appversion";
    /** @deprecated use {@link #API_ABOUT_APP_VERSION}. */
    @Deprecated
    public static final String API_ABOUT_doPost2 = API_ABOUT_APP_VERSION;

    public static final String API_MESSAGE_NEWS = FEATURE_URL + "/news";
    public static final String API_NOTIFICATION_NEWS2 = FEATURE_URL + "/news2";
    public static final String API_NOTIFICATION_NEWS2_USER = FEATURE_URL + "/user/news2";
    /** @deprecated use {@link #API_NOTIFICATION_NEWS2_USER}. */
    @Deprecated
    public static final String API_NOTIFICATION_NEWS2_doGET2 = API_NOTIFICATION_NEWS2_USER;

    public static final String API_BLOCKCHAIN_MEDALS = BASE_API_URL + "/api/blockchain/medals/";
    public static final String API_BLOCKCHAIN_NFT_USER = BASE_API_URL + "/api/blockchain/nft/user/";
    public static final String API_BLOCKCHAIN_NFT_ALL = BASE_API_URL + "/api/blockchain/nft/all";

    public static final String API_MEDAL_RANKING = BASE_API_URL + "/api/medal/ranking";
    public static final String API_MEDAL_QUERY = BASE_API_URL + "/api/medal/query";
    public static final String API_MEDAL_STATS = BASE_API_URL + "/api/medal/stats";
    public static final String API_SERVER_INFO = BASE_API_URL + "/api/server/info";
    public static final String API_HEALTH = BASE_API_URL + "/api/health";

    public static final String API_PROOF_UPLOAD = BASE_API_URL + "/api/proof/upload";
    public static final String API_PROOF_LIST = BASE_API_URL + "/api/proof/list";
    public static final String API_PROOF_DETAIL = BASE_API_URL + "/api/proof/detail";
    public static final String API_PROOF_DELETE = BASE_API_URL + "/api/proof/delete";

    public static final String API_UPLOAD_COMPLETE = BASE_API_URL + "/api/upload/complete";
    public static final String API_UPLOAD_USER_SUBMISSIONS = BASE_API_URL + "/api/upload/user/submissions";
    public static final String API_UPLOAD_SUBMISSION_DETAIL = BASE_API_URL + "/api/upload/submission/detail";
    public static final String API_USER_INFO = BASE_API_URL + "/api/admin/user/info";

    public static final String API_NFT_DAO_UPLOAD_COMPLETE = NFT_DAO_API_URL + "/upload/complete";
    public static final String API_NFT_DAO_SUBMIT_PROOF = NFT_DAO_API_URL + "/submit-proof";
    public static final String API_NFT_DAO_MINT_NFT = NFT_DAO_API_URL + "/mint-nft";
    public static final String API_NFT_DAO_QUERY_NFT = NFT_DAO_API_URL + "/query-nft";
    public static final String API_NFT_DAO_QUERY_ALL_NFTS = NFT_DAO_API_URL + "/query-all-nfts";

    public static final String NFT_PLACEHOLDER_IMAGE_URL =
            "https://via.placeholder.com/300x300?text=NFT";

    public static final String EXTERNAL_BLOCK_EMULATOR_URL = "https://www.blockemulator.com";
    public static final String GITHUB_RELEASE_APK_URL_TEMPLATE =
            "https://github.com/HuangLab-SYSU/brokerwallet-academic/releases/download/V%s/BrokerChain-Wallet.apk";
    public static final String GOOGLE_PLAY_APP_URL_PREFIX =
            "https://play.google.com/store/apps/details?id=";

    private ApiConfig() {
    }

    public static String getServerUrl() {
        return BASE_API_URL;
    }

    public static String getAllNftsUrl(int page, int size) {
        return API_BLOCKCHAIN_NFT_ALL + "?page=" + page + "&size=" + size;
    }

    public static String getMedalQueryUrl(String address) {
        return API_MEDAL_QUERY + "?address=" + address;
    }

    public static String getGithubReleaseApkUrl(String version) {
        return String.format(GITHUB_RELEASE_APK_URL_TEMPLATE, version);
    }

    public static String getGooglePlayAppUrl(String packageName) {
        return GOOGLE_PLAY_APP_URL_PREFIX + packageName;
    }

    public static String resolveNftAssetUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.trim().isEmpty()) {
            return pathOrUrl;
        }
        String value = pathOrUrl.trim();
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:")) {
            if (value.startsWith("http://localhost:5000/")) {
                return value.replace("http://localhost:5000", NFT_DAO_URL);
            }
            return value;
        }
        if (value.startsWith("/")) {
            return NFT_DAO_URL + value;
        }
        return ServerConfig.appendPath(NFT_DAO_URL, value);
    }
}
