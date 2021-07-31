package com.rpc.consumer;

import com.rpc.consumer.proxy.RpcItfScanner;
import com.rpc.management.AbstractRpcConfig;
import com.rpc.management.GracefulShutdownListener;
import com.rpc.management.RpcStatus;
import com.rpc.provider.ServerRpcConfig;
import com.rpc.registerconfig.RegisterCenterConfig;
import com.rpc.management.RpcConfig;
import com.rpc.socket.ClientNettySocketConfig;
import com.rpc.consumer.subscriber.ServiceSubscriber;
import com.rpc.selector.ServerSelector;
import com.rpc.exception.AppException;
import com.rpc.timertask.ScheduleSubscribeTimerTask;

import io.netty.channel.Channel;

import org.apache.log4j.Logger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * entrance for client (service consumer);
 * associated with one client;
 * associated with one 'ServerInfo';
 *
 * @see ServerRpcConfig
 * @user KyZhang
 * @date
 */
public class ClientRpcConfig extends AbstractRpcConfig implements RpcConfig, BeanDefinitionRegistryPostProcessor, DisposableBean {

    private static final Logger logger = Logger.getLogger(ClientRpcConfig.class);
    /** aware by Spring */
    public static ApplicationContext applicationContext;
    /** the classpath for scanning the remote service interfaces */
    protected String[] itfPaths; // set by client
    /** load balancing */
    protected ServerSelector serverSelector; //default Random way; can be set by user
    /** regular subscription task to update the local cache */
    protected ScheduleSubscribeTimerTask cacheRefreshTask;
    protected long synSubscriptSeconds = 30;  // can be set by client

    public static AtomicInteger numRpcServiceNeed = new AtomicInteger();
    public static AtomicInteger numRpcRequestDone = new AtomicInteger();


    public ClientRpcConfig(RegisterCenterConfig registerCenter, ServiceSubscriber subscriber) {
        this.registerCenter = registerCenter;
        if(RpcStatus.class.isAssignableFrom(registerCenter.getClass())){
            registerCenterObserver.setStates(registerCenter);
        }
        this.serviceSubscriber = subscriber;
    }


    /**
     * offline RPC
     * when user does not call the method of Spring IoC's registerShutHook(),
     * this way of code block will work
     *
     * @see #destroy()
     */
    {
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            destroy();
        }));
    }

    /**
     * to customize BDs for remote services;
     * [this way]: regarding local itfPaths : needs client create package for services;
     * only the class marked with @RpcReference can be rpc-ed using proxy;
     * sometimes just the form of property declaration  --> written in xxController : consider next TODO
     *
     * @param registry
     * @throws BeansException
     */
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        RpcItfScanner itfScanner = new RpcItfScanner(registry);
        itfScanner.scan(itfPaths);
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {}

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ClientRpcConfig.applicationContext = applicationContext;
    }

    /**
     * start ioc and schedule tasks such as subscribing service lists regularly from register center
     *
     * @param event
     */
    public void onApplicationEvent(ContextRefreshedEvent event) {
        checkSocket();
        checkSubscriber();
        logger.info("============================ { " + numRpcServiceNeed + " } RPC service we need");
        if(numRpcServiceNeed.get() > 0 && !ServerInfo.isInit.get()){
            try {
                logger.info("============================ begin to initialize local caches after contextRefreshedEvent... ");
                ServerInfo.refresh();
                ServerInfo.isInit.compareAndSet(false,true);
            } catch (AppException e) {
                logger.fatal("=============== fail to initialize the local cache!\n" +
                        "=============== fail to start Spring ioc, since: \n" + e.getMessage());
                ((GenericApplicationContext) applicationContext).close();
                return;
            }
        }

        startDefaultTimerTasks();

        isIocStarted = true;
        logger.info("============= Spring ioc started, and default timer tasks begin to run =============");
    }


    @Override
    public void checkSocket() {
        if (socketConfig == null) {
            socketConfig = new ClientNettySocketConfig();  // default here
            socketObserver.setStates(socketConfig);
        }  // socket part; in case of null
        socketConfig.init();
        logger.info("====== init client's RPC socket successful! ====== ");
    }




    @Override
    protected void doStartDefaultTimerTasks() {
        if(cacheRefreshTask == null){
            cacheRefreshTask = new ScheduleSubscribeTimerTask();
        }
        addTimerTask(cacheRefreshTask, synSubscriptSeconds*3, synSubscriptSeconds, TimeUnit.SECONDS);
    }


    /**
     * close RPC Gracefully;
     *
     * in this project, there are several ways :
     * 1. by JVM shutdownHook registry :
     *       way of code block during the object instantiation of {@code ClientRpcConfig}
     * 2. by relying on Spring :
     *       based on the logic flow of doClose() :
     *             way1:   use ApplicationListener<ContextClosedEvent>'s onApplicationEvent() {@link GracefulShutdownListener}
     *             way2:   implements LifeCycle's stop()
     *             way3:   implements Disposable's destroy() {@link #destroy()}
     *
     * including:
     *          timer
     *          socket module: such as boss/worker groups' close...
     *          registry module
     *          local caches
     *          others like DataBase (this project does not have)
     */
    public void destroy(){
        synchronized (offlineOnce) {
            if (!offlineOnce) {
                logger.info("===== start to execute the method { destroy() } =====");
                try {
                    closeTimer();
                } catch (InterruptedException e) {
                    logger.warn("Interrupted unexpectedly during shutdown process");
                }
                disconnect();  // disconnect
                if (socketConfig != null) {
                    socketConfig.close();  //socket
                }
                registerCenter.close();
                ServerInfo.clear();

                offlineOnce = true;
                logger.info("===== method { destroy() } called by shutdownHook ended =====");
            }
        }
    }


    private void disconnect() {
        for (String server : ServerInfo.serverChannelMap.keySet()) {
            Channel ch = ServerInfo.serverChannelMap.get(server);
            if(ch != null && ch.isOpen()){
                ch.close();  //say goodbye
            }
        }
    }



    //--------------------------------------------------------------------
    //                        get() and set()
    //     if user wants to customize/acquire some configuration
    //--------------------------------------------------------------------

    public ServerSelector getServerSelector() {
        return serverSelector;
    }

    public void setServerSelector(ServerSelector serverSelector) {
        this.serverSelector = serverSelector;
    }

    public ScheduledThreadPoolExecutor getTimer() {
        return timer;
    }

    public String[] getItfPaths() {
        return itfPaths;
    }

    public void setItfPaths(String... itfPaths) {
        this.itfPaths = itfPaths;
    }

    public long getSynSubscriptSeconds() {
        return synSubscriptSeconds;
    }

    public void setSynSubscriptSeconds(long synSubscriptSeconds) {
        this.synSubscriptSeconds = synSubscriptSeconds;
    }

    public void setSocketObservePeriod(int socketObservePeriod) {
        this.socketObservePeriod = socketObservePeriod;
    }

    public ScheduleSubscribeTimerTask getCacheRefreshTask() {
        return cacheRefreshTask;
    }

    public void setCacheRefreshTask(ScheduleSubscribeTimerTask cacheRefreshTask) {
        this.cacheRefreshTask = cacheRefreshTask;
    }

}

