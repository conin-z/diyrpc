package com.rpc.utils;

import io.netty.handler.timeout.IdleStateEvent;

/**
 * @user KyZhang
 * @date
 */
public class StringUtil {

    /**
     * such as : convert "127.0.0.1::10086" to {"127.0.0.1","10086"}
     *
     * @param server
     * @return
     */
    public static String[] resolveIpPortFromString(String server){
        String gap = Constant.IP_PORT_GAP;
        return server.split(gap);
    }


    public static String getIdleEventInfo(IdleStateEvent event) {
        String eventType = null;
        switch (event.state()){
            case READER_IDLE:
                eventType ="idle for reading";
                break;
            case WRITER_IDLE:
                eventType = "idle for writing";
                break;
            case ALL_IDLE:
                eventType = "idle for both reading and writing";
        }
        return eventType;
    }

}
