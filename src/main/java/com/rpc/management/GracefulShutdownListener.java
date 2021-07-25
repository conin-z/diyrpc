package com.rpc.management;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 *
 * way1 : use the event propagation of Spring to control consumer/provider's shutdown;
 * will be invoked when ioc closing;
 * can be inherited and the single method can be overridden
 *
 * @user KyZhang
 * @date
 */
public class GracefulShutdownListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger logger = Logger.getLogger(GracefulShutdownListener.class);

    RpcConfiguration app;  // consumer or provider

    public GracefulShutdownListener() {
    }

    public GracefulShutdownListener(final RpcConfiguration app) {
        this.app = app;
    }


    /**
     * close rpc when user has called the method of context's registerShutHook():
     *      1.hook registry  2. doClose :
     *                            way1:   ApplicationListener<ContextClosedEvent>'s onApplicationEvent()
     *                            way2:   LifeCycle's stop()
     *                            way3:   Disposable's destroy()
     *
     */
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (app != null) {
            app.destroy();
        }
    }

}
