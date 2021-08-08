package com.rpc.utils;


/**
 * @user KyZhang
 * @date
 */

public class Constant{

    public static final String SOCKET_HEARTBEAT_TIME = "heartbeat";

    public static final int REQUEST_RETRY_TIMES = 2;
    public static final Integer CHANNEL_RETRY_TIMES = 2;
    public static final int SERVER_RESELECT_TIMES = 3;
    public static final int IDLE_TIMES = 3;
    public static final int SUBSCRIBE_TIMES = 3;
    public static final long WAIT_REFER_UPDATE = 10000;
    public static final String ONLINE = "ONLINE";
    public static final String OFFLINE = "OFFLINE";

    // servers will update this into " $ip$:$port$ "
    public static String LOCAL_ADDRESS = "localhost";
    public static String IP_PORT_GAP = ":";

    public static int NETTY_BOSS_GROUP = 20;
    public static int NETTY_WORKER_GROUP = 20;

    public static String SERVER_LIST_NAME = "set_server";

    public static String[] SUBSCRIPTION_CHANNEL_PATTERN = new String[]{ONLINE, OFFLINE};

}
