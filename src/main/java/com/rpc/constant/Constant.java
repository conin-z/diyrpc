package com.rpc.constant;


/**
 * @user KyZhang
 * @date
 */

public class Constant{

    public static final String NETTY_HEARTBEAT_TIME = "heartbeat";

    public static final int REQUEST_RETRY_TIMES = 2;
    public static final int SERVER_RESELECT_TIMES = 3;
    public static final int IDLE_TIMES = 3;
    public static final int SUBSCRIBE_TIMES = 3;
    public static final long WAIT_REFER_UPDATE = 10000;

    /* servers will update this into " $ip$::$port$ " */
    public static String LOCAL_ADDRESS = "localhost";

    public static int NETTY_BOSS_GROUP = 20;
    public static int NETTY_WORKER_GROUP = 20;

    public static String REDIS_SERVER_LIST = "set_server";

    public static String IP_PORT_GAP = "::";

}
