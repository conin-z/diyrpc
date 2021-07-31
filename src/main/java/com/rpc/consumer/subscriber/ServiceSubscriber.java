package com.rpc.consumer.subscriber;

import java.util.Set;

/**
 * for client, to subscribe the RPC service-related information with register center
 *
 * @user KyZhang
 * @date
 */
public interface ServiceSubscriber {


    /**
     * when this RPC service candidate is invalid, remove its information from register center
     * will change this way later ->
     * TODO: use the way of notification, pass unavailability-information to servers to make the decision
     *
     * @param key
     * @param eleName
     * @return
     */
    long removeElement(String key, String eleName);


    /**
     *
     * @param key
     * @return
     */
    Set<String> getServiceList(String key);

}
