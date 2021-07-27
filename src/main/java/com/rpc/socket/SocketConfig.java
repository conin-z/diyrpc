package com.rpc.socket;

import com.rpc.management.RpcStatus;

/**
 * @user KyZhang
 * @date
 */
public interface SocketConfig extends RpcStatus{

    void init();
    void close();
    boolean connect(final String ip, final int port);

}
