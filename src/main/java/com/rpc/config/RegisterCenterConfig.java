package com.rpc.config;


/**
 * @user KyZhang
 * @date
 */
public interface RegisterCenterConfig {

    void offlineFromRedis();
    void init(String configPath);

}
