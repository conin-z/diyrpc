package com.rpc.timertask;

import com.rpc.provider.registry.ServiceRegistry;


/**
 * customize the timer task;
 * can be inherited
 */
public class ExpireTimerTask extends AbstractTimerTask {

    private ServiceRegistry registry;
    private int expireSeconds;

    public ExpireTimerTask(ServiceRegistry centerConfig, int expireSeconds){
        this.registry = centerConfig;
        this.expireSeconds = expireSeconds;
    }

    @Override
    public void run() {
        registry.keepAlive(expireSeconds);
    }

}

