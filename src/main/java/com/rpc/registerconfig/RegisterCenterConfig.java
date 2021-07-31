package com.rpc.registerconfig;

import com.rpc.management.RpcStatus;

/**
 * @user KyZhang
 * @date
 */
public interface RegisterCenterConfig extends RpcStatus {

    /**
     * acquire keys' life with register center
     *
     * @return  keys' life
     */
    int getExpireSeconds();


    /**  disconnect from register center and destroy the associated resources such as Redis pool */
    void close();


}
