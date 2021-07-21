package com.rpc.config;

import java.util.Set;

/**
 * @user KyZhang
 * @date
 */
public interface SocketConfig {

    void serverInit(int port);
    boolean clientInit(String ip, int port);
    void close();
    void connectAndSave(String itfName, String server, Set<String> serverListForItf);
    
}
