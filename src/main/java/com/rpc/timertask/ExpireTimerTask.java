package com.rpc.timertask;

import com.rpc.provider.ServerRpcConfig;
import com.rpc.provider.registry.ServiceRegistry;
import org.apache.log4j.Logger;


/**
 * the task to set/keep keys' life regularly with register center
 *
 * @user KyZhang
 * @date
 */
public class ExpireTimerTask extends AbstractTimerTask {

    private static final Logger logger = Logger.getLogger(ExpireTimerTask.class);

    private ServiceRegistry registry;
    private int expireSeconds;

    public ExpireTimerTask(int expireSeconds){
        registry = ServerRpcConfig.applicationContext.getBean(ServerRpcConfig.class).getServiceRegistry();
        expireSeconds = expireSeconds;
    }

    @Override
    public void run() {
        logger.debug("===== scheduled task for keep heartbeat with register center is to begin... =====");
        registry.keepAlive(expireSeconds);
    }

}

