package com.example.brokerfi.config;

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
     * - 本地开发：http://127.0.0.1:5000 或 http://localhost:5000
     * - USB调试：http://你的电脑IP:5000（例如：http://192.168.1.100:5000）
     * - 云服务器：http://your-domain.com 或 http://服务器IP:5000
     */
    public static final String BASE_URL = "http://127.0.0.1:5000";
    
    /**
     * API接口路径
     */
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

