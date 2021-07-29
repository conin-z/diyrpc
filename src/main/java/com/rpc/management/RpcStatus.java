package com.rpc.management;


public interface RpcStatus{

    /**
     * to show current status
     */
    void show();

    /**
     * to change status
     * @see RpcCriterion
     *
     * @param condition   the trigger condition have to be met if want to change status
     * @param inputs    some of the parameters needed when altering
     */
    void alter(RpcCriterion condition, Object... inputs);

}
