package com.rpc.socket.nettyhandler;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * used in class InvokeHandler
 */
public class NettyRequestInfo {

    volatile static Map<String, String> requestMapOnHandling = new ConcurrentHashMap<>();
    volatile static Map<String, String> requestMapHandled = new ConcurrentHashMap<>();
    volatile static Map<String, String> requestMapFailed = new ConcurrentHashMap<>();
    static AtomicInteger requestNumOnHandling = new AtomicInteger();


    public static int getRequestNumOnHandling(){
        return requestNumOnHandling.get();
    }

    public static int getRequestNumHandled() {
        return requestMapHandled.size();
    }

    public static int getRequestNumFailed() {
        return requestMapFailed.size();
    }

    public static String getClientListOnHandling(){
        return new HashSet(requestMapOnHandling.values()).toString();
    }

    public static String getRequestMapOnHandling() {
        return requestMapOnHandling.toString();
    }

    public static String getRequestMapHandled() {
        return requestMapHandled.toString();
    }

    public static String getRequestMapFailed() {
        return requestMapFailed.toString();
    }


}
