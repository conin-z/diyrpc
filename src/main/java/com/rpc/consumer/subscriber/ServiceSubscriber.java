package com.rpc.consumer.subscriber;

import java.util.Set;

public interface ServiceSubscriber {

    void init();

    long removeElement(String key, String eleName);

    Set<String> getServiceList(String key);

}
