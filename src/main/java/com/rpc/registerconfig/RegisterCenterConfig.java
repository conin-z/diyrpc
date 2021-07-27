package com.rpc.registerconfig;

import com.rpc.management.RpcStatus;

/**
 * @user KyZhang
 * @date
 */
public interface RegisterCenterConfig extends RpcStatus {

    int getExpireSeconds();

    void close();

    void initRegisterCenter();

}
