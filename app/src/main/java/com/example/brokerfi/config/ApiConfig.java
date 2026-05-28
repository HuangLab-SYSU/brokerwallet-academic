package com.example.brokerfi.config;

import static com.example.brokerfi.config.ServerConfig.FEATURE_PORT;
import static com.example.brokerfi.config.ServerConfig.NFT_DAO_PORT;
import static com.example.brokerfi.config.ServerConfig.SERVER_HOST;
import static com.example.brokerfi.config.ServerConfig.SERVER_PORT;

/**
 * API服务器配置
 * 
 * 🔧 部署配置说明：
 * 
 * 1. USB调试模式（手机连接电脑）：
 *    - 获取电脑的局域网IP地址（例如：192.168.1.100）
 *    - 修改 BASE_URL 为 "http://192.168.1.100:5000"
 *    - 确保手机和电脑在同一WiFi下
 * 
 * 2. 云服务器部署：
 *    - 修改 BASE_URL 为云服务器地址（例如：http://your-domain.com）
 *    - 重新编译APK
 * 
 * 注意：修改此配置后需要重新编译整个应用！
 */
public class ApiConfig {
    
    /**
     * 后端服务器基础URL
     * 
     * 📝 修改说明：
     * - 本地开发：http://127.0.0.1:56741 或 http://localhost:56741
     * - USB调试：http://你的电脑IP:56741（例如：http://192.168.1.100:56741）
     * - 云服务器：http://your-domain.com 或 http://服务器IP:56741
     */
    //public static final String BASE_URL = "http://academic.broker-chain.com:5000";
    //public static final String BASE_URL = "https://academic.broker-chain.com:440";
    //public static final String BASE_URL = "http://10.0.2.2:56741";
    public static final String BASE_URL = "https://" + SERVER_HOST + ":" + SERVER_PORT;
    public static final String FEATURE_URL = "https://" + SERVER_HOST + ":" + FEATURE_PORT;
    public static final String NFT_DAO_URL = "https://" + SERVER_HOST + ":" + NFT_DAO_PORT;

    /**
     * API接口路径
     */
    public static final String API_ABOUT_doPost2 = FEATURE_URL + "/appversion";
    public static final String API_MESSAGE_NEWS = FEATURE_URL + "/news";
    public static final String API_NOTIFICATION_NEWS2 = FEATURE_URL + "/news2";
    public static final String API_NOTIFICATION_NEWS2_doGET2 = FEATURE_URL + "/user/news2";
    public static final String API_BLOCKCHAIN_MEDALS = BASE_URL + "/api/blockchain/medals/";
    public static final String API_BLOCKCHAIN_NFT_USER = BASE_URL + "/api/blockchain/nft/user/";
    public static final String API_BLOCKCHAIN_NFT_ALL = BASE_URL + "/api/blockchain/nft/all";
    public static final String API_UPLOAD_USER_SUBMISSIONS = BASE_URL + "/api/upload/user/submissions";


    
    /**
     * 获取服务器基础URL（用于图片访问）
     */
    public static String getServerUrl() {
        return BASE_URL;
    }
}

