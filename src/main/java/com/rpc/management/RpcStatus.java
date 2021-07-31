package com.rpc.management;

/**
 * RPC-related status in the processes
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
     * @param input    some of the parameters needed when altering
     */
    void alter(RpcCriterion condition, Object input);

}
