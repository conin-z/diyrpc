package com.rpc.socket;

import com.rpc.exception.AppException;


/**
 * @user KyZhang
 * @date
 */
public interface SocketConfig {

    void serverInit(int port);
    void clientInit(String ip, int port) throws AppException;
    void close();
    boolean connect(String ip, String port);
    
}
