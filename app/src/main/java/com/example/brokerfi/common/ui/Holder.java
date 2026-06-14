package com.example.brokerfi.common.ui;

import com.example.brokerfi.core.config.ChainConfig;
import com.example.brokerfi.core.config.ServerConfig;

/**
 * 旧代码兼容入口。
 *
 * 新代码不要再把服务器或合约配置放到 Holder；服务器配置使用
 * ServerConfig/ApiConfig，链配置使用 ChainConfig。
 */
public class Holder {
    /** @deprecated use {@link ServerConfig#SERVER_HOST}. */
    @Deprecated
    public static final String serverHost = ServerConfig.SERVER_HOST;

    /** @deprecated use {@link ServerConfig#SERVER_PORT}. */
    @Deprecated
    public static final String serverPort = String.valueOf(ServerConfig.SERVER_PORT);

    /** @deprecated use {@link ChainConfig#MAIN_CONTRACT_ADDRESS}. */
    @Deprecated
    public static final String contractaddr = ChainConfig.MAIN_CONTRACT_ADDRESS;
}
