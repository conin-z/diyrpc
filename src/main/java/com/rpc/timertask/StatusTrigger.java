package com.rpc.timertask;

import com.rpc.management.RpcCriterion;
import com.rpc.management.RpcStatus;
import org.springframework.lang.NonNull;

/**
 * rpc-related status modifier/invader/...
 * TODO: later will be refined
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
public class StatusTrigger extends AbstractTimerTask {

    protected RpcStatus[] states;
    /**  can be null: no condition to change */
    protected RpcCriterion criterion;
    /** can be null: no extra inputs for changing */
    protected Object input;


    public StatusTrigger(RpcCriterion criterion, Object input, @NonNull RpcStatus status){
        this.criterion = criterion;
        this.input = input;
        states = new RpcStatus[1];
        states[0] = status;
    }

    public StatusTrigger(RpcCriterion criterion, Object input, RpcStatus... states){
        this.criterion = criterion;
        this.input = input;
        this.states = states;
    }


    @Override
    public void run() {
        for (RpcStatus status : states) {
            status.alter(criterion, input);
        }
    }


    public void setStates(RpcStatus... states) {
        this.states = states;
    }

    public void setStatus(@NonNull RpcStatus status) {
        if (states == null || states.length == 0) {
            states = new RpcStatus[1];
        }
        states[0] = status;
    }

    public void setCriterion(RpcCriterion criterion) {
        this.criterion = criterion;
    }

    public void setInput(Object input) {
        this.input = input;
    }
}
