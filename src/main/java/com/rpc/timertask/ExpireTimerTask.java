package com.rpc.timertask;

import com.rpc.provider.ServerRpcConfig;
import com.rpc.provider.registry.ServiceRegistry;
import org.apache.log4j.Logger;


/**
 * customize the timer task;
 * can be inherited
 */
public class ExpireTimerTask extends AbstractTimerTask {

    private static final Logger logger = Logger.getLogger(ExpireTimerTask.class);

    private ServiceRegistry registry;
    private int expireSeconds;

    public ExpireTimerTask(int expireSeconds){
        this.registry = ServerRpcConfig.applicationContext.getBean(ServerRpcConfig.class).getServiceRegistry();
        this.expireSeconds = expireSeconds;
    }

    @Override
    public void run() {
        logger.debug("===== scheduled task for keep heartbeat with register center is to begin... =====");
        registry.keepAlive(expireSeconds);
    }

}

