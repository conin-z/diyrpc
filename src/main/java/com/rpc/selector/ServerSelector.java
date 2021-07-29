package com.rpc.selector;

import com.rpc.message.RequestImpl;

import java.util.Set;

/**
 * load balancing strategy
 *
 * @user KyZhang
 * @date
 */
@FunctionalInterface
public interface ServerSelector {

    /**
     * load balancing strategy
     *
     * @param serverListForItfClass   service-provider candidates
     * @param request  may need some information for decision from the request to be sent
     * @return
     */
    String select(Set<String> serverListForItfClass, RequestImpl request);

}
