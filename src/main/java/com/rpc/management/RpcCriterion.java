package com.rpc.management;

/**
 * TODO: later will be completed
 *
 * @see RpcStatus
 * @user KyZhang
 * @date
 */
public interface RpcCriterion {

    /**
     * to decide the trigger conditions and parameters for change-of-status
     *
     * @param obj   the reference objects of dependency when setting
     */
    void setCondition(Object... obj);


}
