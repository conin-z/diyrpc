package com.rpc.config;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * use the event propagation of Spring to control consumer/provider's shutdown;
 * will be invoked when ioc closing;
 * can be inherited and the single method can be overridden
 *
 * @user KyZhang
 * @date
 */
public class GracefulShutdownListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger logger = Logger.getLogger(GracefulShutdownListener.class);

    RpcConfiguration app;  //client or server

    public GracefulShutdownListener() {
    }

    public GracefulShutdownListener(final RpcConfiguration app) {
        this.app = app;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        logger.debug("======= shutdownHook in spring for server offline execute...");
        if (app != null) {
            app.destroy();
        }
        logger.debug("======= shutdownHook in spring for server offline execute end");
    }

}
