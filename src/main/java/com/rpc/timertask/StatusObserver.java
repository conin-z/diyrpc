package com.rpc.timertask;

import com.rpc.management.RpcStatus;
import org.springframework.lang.NonNull;

/**
 * RPC-related status observer
 * need match with instance of {@code RpcStatus}
 *
 * @see RpcStatus (com.rpc.management)
 *          SocketConfig (com.rpc.socket)
 *              AbstractNettySocketConfig (com.rpc.socket)
 *              ServerNettySocketConfig (com.rpc.socket)
 *              NettyClientSocketConfig (com.rpc.socket)
 *          RegisterCenterConfig (com.rpc.registerconfig)
 *              RedisRegisterCenterConfig (com.rpc.registerconfig)
 *          ServerInfo (com.rpc.consumer)
 *          DefaultRpcStatus (com.rpc.timertask)
 *
 * @user KyZhang
 * @date
 */
public class StatusObserver extends AbstractTimerTask{

    protected RpcStatus[] states;

    public StatusObserver(@NonNull RpcStatus status){
        states = new RpcStatus[1];
        states[0] = status;
    }

    /**
     * observe multiple states simultaneously/in succession
     *
     * @param states
     */
    public StatusObserver(RpcStatus... states){
        this.states = states;
    }


    @Override
    public void run() {
        for (RpcStatus status : states) {
            status.show();
        }
    }


    public void setStates(@NonNull RpcStatus status) {
        if (states == null || states.length == 0) {
            states = new RpcStatus[1];
        }
        states[0] = status;
    }

    public void setStatus(RpcStatus... states) {
        this.states = states;
    }


}
