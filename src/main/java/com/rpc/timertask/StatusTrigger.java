package com.rpc.timertask;

import com.rpc.management.RpcCriterion;
import com.rpc.management.RpcStatus;
import org.springframework.lang.NonNull;

/**
 * rpc-related status modifier/invader/...
 * TODO: later will be refined
 * need match with instance of <code>RpcStatus<code/>
 *
 * @see RpcStatus (com.rpc.management)
 *          SocketConfig (com.rpc.socket)
 *              AbstractNettySocketConfig (com.rpc.socket)
 *              NettyServerSocketConfig (com.rpc.socket)
 *              NettyClientSocketConfig (com.rpc.socket)
 *          RegisterCenterConfig (com.rpc.registerconfig)
 *              RedisRegisterCenterConfig (com.rpc.registerconfig)
 *          ServerInfo (com.rpc.consumer)
 *          DefaultRpcStatus (com.rpc.timertask)
 */
public class StatusTrigger extends AbstractTimerTask {

    protected RpcStatus status;
    protected RpcCriterion criterion;
    protected Object[] inputs;

    public StatusTrigger(@NonNull RpcStatus state, RpcCriterion criterion, Object... inputs){
        this.status = state;
        this.criterion = criterion;
        this.inputs = inputs;
    }

    @Override
    public void run() {
        status.alter(criterion, inputs); // callback
    }
}
