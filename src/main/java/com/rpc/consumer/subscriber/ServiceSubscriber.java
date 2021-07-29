package com.rpc.consumer.subscriber;

import java.util.Set;

/**
 * for client, to subscribe the RPC service-related information with register center
 */
public interface ServiceSubscriber {


    /**
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
