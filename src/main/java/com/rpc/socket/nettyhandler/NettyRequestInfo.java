package com.rpc.socket.nettyhandler;

import com.rpc.consumer.ServerInfo;
import com.rpc.management.RpcCriterion;
import com.rpc.management.RpcStatus;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * to record the status of request
 * used in {@link InvokeHandler}
 */
public final class NettyRequestInfo implements RpcStatus {

    private static final Logger logger = Logger.getLogger(NettyRequestInfo.class);

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


    @Override
    public void show() {
        String info = "\n========= requests BEING HANDLING count : {" + getRequestNumOnHandling() + "}\n" +
                "with corresponding clients are :\n" + getRequestMapOnHandling() + "\n" +
                "========= requests HANDLED-SUCCESS count : {" + getRequestNumHandled() + "}\n" +
                "with corresponding clients are :\n" + getRequestMapHandled() + "\n" +
                "========= requests HANDLED-FAIL count : {" + getRequestNumFailed() + "}\n" +
                "with corresponding clients are :\n" + getRequestMapFailed() + "\n" +
                "=================================          =================================";
        logger.info(info);
    }

    @Override
    public void alter(RpcCriterion condition, Object... inputs) {

    }

}
