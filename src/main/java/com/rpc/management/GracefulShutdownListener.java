package com.rpc.management;

import com.rpc.consumer.ClientRpcConfig;
import com.rpc.provider.ServerRpcConfig;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;


/**
 * close RPC Gracefully;
 *
 * in this project, there are several ways :
 * 1. by JVM shutdownHook registry :
 *       way of code block during the object instantiation of {@code ClientRpcConfig} or {@code ServerRpcConfig}
 * 2. by relying on Spring :
 *       based on the logic flow of doClose() :
 *             way1:   use ApplicationListener<ContextClosedEvent>'s onApplicationEvent() {@link #onApplicationEvent(ContextClosedEvent)}
 *             way2:   implements LifeCycle's stop()
 *             way3:   implements Disposable's destroy() {@link ClientRpcConfig#destroy()} {@link ServerRpcConfig#destroy()}
 *
 * @user KyZhang
 * @date
 */
public class GracefulShutdownListener implements ApplicationListener<ContextClosedEvent> {

    protected RpcConfig app;  // consumer or provider

    public GracefulShutdownListener() {
    }

    public GracefulShutdownListener(final RpcConfig app) {
        this.app = app;
    }


    /**
     * way1 : use the event propagation of Spring to control consumer/provider's shutdown;
     *
     * will be invoked when ioc closing;
     * can be inherited and the single method can be overridden
     */
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (app != null) {
            app.destroy();
        }
    }

}
