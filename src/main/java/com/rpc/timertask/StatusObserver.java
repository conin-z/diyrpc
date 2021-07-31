package com.rpc.timertask;

import com.rpc.consumer.ServerInfo;
import com.rpc.management.RpcStatus;
import com.rpc.registerconfig.RedisRegisterCenterConfig;
import com.rpc.registerconfig.RegisterCenterConfig;
import com.rpc.socket.AbstractNettySocketConfig;
import com.rpc.socket.ClientNettySocketConfig;
import com.rpc.socket.ServerNettySocketConfig;
import com.rpc.socket.SocketConfig;
import com.rpc.socket.nettyhandler.NettyRequestInfo;
import org.springframework.lang.NonNull;

/**
 * RPC-related status observer
 * need match with instance of {@code RpcStatus}
 *
 * @see RpcStatus
 * @see DefaultRpcStatus
 * @see SocketConfig
 * @see AbstractNettySocketConfig
 * @see ServerNettySocketConfig
 * @see ClientNettySocketConfig
 * @see RegisterCenterConfig
 * @see RedisRegisterCenterConfig
 * @see ServerInfo
 * @see NettyRequestInfo
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
