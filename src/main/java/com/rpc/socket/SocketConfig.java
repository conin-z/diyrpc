package com.rpc.socket;

import com.rpc.management.RpcStatus;

/**
 * @user KyZhang
 * @date
 */
public interface SocketConfig extends RpcStatus{

    void init();


    /**
     * shutdown of resources; such as the graceful shutdown of workgroups
     */
    void close();


    /**
     * will used by clients
     *
     * @param ip
     * @param port
     * @return
     */
    boolean connect(final String ip, final int port);

}
