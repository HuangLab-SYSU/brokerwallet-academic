package com.example.brokerfi.config;

/**
 * 服务器配置文件
 * 位于独立的config包中，方便修改IP配置
 * 用于管理所有后端服务的地址和端口
 * 当IP地址发生变化时，只需修改此文件即可
 */
public class ServerConfig {
    
    // ==================== 服务器配置 ====================
    
    /**
     * 主服务器地址
     * 注意：IP地址可能会变化，请根据实际情况修改
     * 
     * 模拟器使用：10.0.2.2
     * 真机使用：电脑的局域网IP（如：192.168.1.100）
     */
    public static final String SERVER_HOST = "127.0.0.1"; // USB连接使用localhost
    
    /**
     * 主服务器端口
     */
    public static final int SERVER_PORT = 5000;
    
    /**
     * 完整的服务器基础URL
     */
    public static final String BASE_URL = "http://" + SERVER_HOST + ":" + SERVER_PORT;
    
    // ==================== API端点配置 ====================
    
    /**
     *
     *
     * 证明文件上传
     */
    public static final String UPLOAD_PROOF_API = BASE_URL + "/api/proof/upload";
    
    /**
     * 获取证明文件列表API
     */
    public static final String GET_PROOF_LIST_API = BASE_URL + "/api/proof/list";
    
    /**
     * 获取证明文件详情API
     */
    public static final String GET_PROOF_DETAIL_API = BASE_URL + "/api/proof/detail";
    
    /**
     * 删除证明文件API
     */
    public static final String DELETE_PROOF_API = BASE_URL + "/api/proof/delete";
    
    // ==================== 其他功能API（待实现） ====================
    
    /**
     * 勋章排行榜API
     */
    public static final String MEDAL_RANKING_API = BASE_URL + "/api/medal/ranking";
    
    /**
     * NFT铸造API
     */
    public static final String MINT_NFT_API = BASE_URL + "/api/nft/mint";
    
    /**
     * NFT查看API
     */
    public static final String VIEW_NFT_API = BASE_URL + "/api/nft/view";
    
    /**
     * 用户提交历史API
     */
    public static final String USER_SUBMISSIONS_API = BASE_URL + "/api/upload/user/submissions";
    
    /**
     * 提交详情API
     */
    public static final String SUBMISSION_DETAIL_API = BASE_URL + "/api/upload/submission/detail";
    
    /**
     * 获取用户信息API（花名、代表作等）
     */
    public static final String USER_INFO_API = BASE_URL + "/api/admin/user/info";
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取完整的API URL
     * @param endpoint API端点
     * @return 完整的URL
     */
    public static String getApiUrl(String endpoint) {
        return BASE_URL + endpoint;
    }
    
    /**
     * 检查服务器是否可访问
     * @return true if server is reachable
     */
    public static boolean isServerReachable() {
        // 这里可以添加ping检查逻辑
        return true;
    }
}
