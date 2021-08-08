package com.rpc.selector;

import com.rpc.message.RequestImpl;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class ConsistentHashSelector  extends AbstractSelector{


    /** containing Hash selectors for each server */
    private final ConcurrentHashMap<String, LocalSelector> selectors = new ConcurrentHashMap<>();


    @Override
    protected void prepare() {

    }


    @Override
    protected String doSelect(Set<String> serverListForItfClass, RequestImpl request) {
        return null;
    }


    private class LocalSelector {

    }

}
