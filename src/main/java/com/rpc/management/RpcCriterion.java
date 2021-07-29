package com.rpc.management;

/**
 * @see RpcStatus
 *
 *  TODO: later will be completed
 */
public interface RpcCriterion {

    /**
     * to decide the trigger conditions and parameters for change-of-status
     *
     * @param obj   the reference objects of dependency when setting
     */
    void setCondition(Object... obj);


}
