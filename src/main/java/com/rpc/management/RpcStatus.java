package com.rpc.management;

import com.rpc.timertask.StatusObserver;
import com.rpc.timertask.StatusTrigger;

/**
 * RPC-related status in the processes
 *
 * @see StatusObserver
 * @see StatusTrigger
 *
 * @user KyZhang
 * @date
 */
public interface RpcStatus{

    /**
     * to show current status
     */
    void show();

    /**
     * to change status
     *
     * @see RpcCriterion
     * @param condition   the trigger condition have to be met if want to change status
     * @param input    some parameters needed when altering
     */
    void alter(RpcCriterion condition, Object input);

}
