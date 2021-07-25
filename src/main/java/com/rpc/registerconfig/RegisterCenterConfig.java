package com.rpc.registerconfig;


/**
 * @user KyZhang
 * @date
 */
public interface RegisterCenterConfig {

    int getExpireSeconds();

    void close();

    void initRegisterCenter();

}
