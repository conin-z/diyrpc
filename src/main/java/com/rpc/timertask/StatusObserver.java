package com.rpc.timertask;

import com.rpc.management.RpcStatus;

/**
 *
 * rpc-related status observer
 * need match with instance of class RpcStatus
 * see:
 *   RpcStatus (com.rpc.management)
 *      SocketConfig (com.rpc.socket)
 *         AbstractNettySocketConfig (com.rpc.socket)
 *         NettyServerSocketConfig (com.rpc.socket)
 *         NettyClientSocketConfig (com.rpc.socket)
 *      RegisterCenterConfig (com.rpc.registerconfig)
 *         RedisRegisterCenterConfig (com.rpc.registerconfig)
 *      ServerInfo (com.rpc.consumer)
 *      DefaultRpcStatus (com.rpc.timertask)
 */
public class StatusObserver extends AbstractTimerTask{

    private RpcStatus status;

    public StatusObserver(RpcStatus state){
        this.status = state;
    }

    @Override
    public void run() {
        status.show();
    }

    public void setStatus(RpcStatus status) {
        this.status = status;
    }

}