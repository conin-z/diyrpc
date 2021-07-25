package com.rpc.selector;

import com.rpc.message.RequestImpl;

import java.util.Set;

/**
 * @user KyZhang
 * @date
 */
@FunctionalInterface
public interface ServerSelector {

    /**
     * load balancing strategy
     *
     * @param serverListForItfClass
     * @param request
     * @return
     */
    String select(Set<String> serverListForItfClass, RequestImpl request);

}
