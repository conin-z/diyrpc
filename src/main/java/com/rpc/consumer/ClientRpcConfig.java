package com.rpc.consumer;

import com.rpc.consumer.proxy.RpcItfScanner;
import com.rpc.management.AbstractRpcConfig;
import com.rpc.management.RpcStatus;
import com.rpc.registerconfig.RegisterCenterConfig;
import com.rpc.management.RpcConfig;
import com.rpc.socket.NettyClientSocketConfig;
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
 * entrance for client;
 * associated with one client;
 * associated with one 'ServerInfo';
 *
 * @user KyZhang
 * @date
 */
public class ClientRpcConfig extends AbstractRpcConfig implements RpcConfig, BeanDefinitionRegistryPostProcessor, DisposableBean {

    private static final Logger logger = Logger.getLogger(ClientRpcConfig.class);

    public static ApplicationContext applicationContext;

    private String[] itfPaths; //for scan service interfaces   --by consumer set
    private ServerSelector serverSelector; //default Random way; // load balancing  //can be set by user

    private ScheduleSubscribeTimerTask cacheRefreshTask;
    private long synSubscriptSeconds = 30;  // --by client set
    public static AtomicInteger numRpcServiceNeed = new AtomicInteger();
    public static AtomicInteger numRpcRequestDone = new AtomicInteger();


    public ClientRpcConfig(RegisterCenterConfig registerCenter, ServiceSubscriber subscriber) {
        this.registerCenter = registerCenter;
        if(RpcStatus.class.isAssignableFrom(registerCenter.getClass())){
            registerCenterObserver.setStatus(registerCenter);
        }
        this.serviceSubscriber = subscriber;
    }


    /**
     *  offline for rpc service
     *  if user not call the method of ioc's registerShutHook()
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
     * sometimes just the form of property declaration  --> written in xxController : consider next
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
     * start!  and keep schedule subscribe lists from redis;
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
            socketConfig = new NettyClientSocketConfig();  // default here
            socketObserver.setStatus(socketConfig);
        }  // socket part; in case of null
        socketConfig.init();
        logger.info("====== init client's RPC socket successful! ====== ");
    }




    @Override
    public void startDefaultTimerTasks() {
        if(cacheRefreshTask == null){
            cacheRefreshTask = new ScheduleSubscribeTimerTask();
        }
        addTimerTask(cacheRefreshTask, synSubscriptSeconds*3, synSubscriptSeconds, TimeUnit.SECONDS);
        super.startDefaultTimerTasks();
    }


    /**
     * close rpc when user has called the method of context's registerShutHook():
     *      1.hook registry  2. doClose :
     *                            way1:   ApplicationListener<ContextClosedEvent>'s onApplicationEvent()
     *                            way2:   LifeCycle's stop()
     *                            way3:   Disposable's destroy()
     *
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


    /* ------------------------- get and set ------------------------- */
    /* ---- if user wants to customize/acquire some configuration ---- */

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
