package com.rpc.consumer;

import com.rpc.consumer.proxy.RpcItfScanner;
import com.rpc.consumer.subscriber.RedisServiceSubscriber;
import com.rpc.registerconfig.RedisRegisterCenterConfig;
import com.rpc.registerconfig.RegisterCenterConfig;
import com.rpc.management.RpcConfiguration;
import com.rpc.socket.NettySocketConfig;
import com.rpc.socket.SocketConfig;
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

import java.util.*;


/**
 * entrance for client;
 * associated with one client;
 * associated with one 'ServerInfo';
 *
 * @user KyZhang
 * @date
 */
public class RpcClientConfiguration implements RpcConfiguration, BeanDefinitionRegistryPostProcessor, DisposableBean {

    private static final Logger logger = Logger.getLogger(RpcClientConfiguration.class);

    public static ApplicationContext ioc;

    /* ----- ------services cache-related------------- */
    //*** [key attribute]   ~~ ~ ~~ [2-level cache] ~~ ~ ~~
    private Map<String, List<String>> itfServersMap; //aware  :  all servers and their containing services
    //*** [key attribute]   ~~ ~ ~~ [2-level cache] ~~ ~ ~~
    private Set<String> serversList;   //aware:  lists this server lies in
    //*** [assist attribute]
    private Map<String, Set<String>> servicesNameMap; // all servers and their available services in register center

    private String[] itfPaths; //for scan service interfaces   --by consumer set

    /* ---------------netty-related--------------- */
    private SocketConfig socketConfig;  //default netty
    private int port;
    private ServerSelector serverSelector; //default Random way; // load balancing  //can be set by user

    /* --------------- redis-related--------------- */
    private RegisterCenterConfig registerCenter;  // --can set by client
    private ServiceSubscriber serviceSubscriber;
    private Timer timer; // keep
    private TimerTask timerTask;
    private long synSubscriptSeconds = 30;  // --by client set

    private volatile Boolean isIocStarted = false;
    private volatile Boolean isDone = false;

    public RpcClientConfiguration(RegisterCenterConfig registerCenter, ServiceSubscriber subscriber) {
        this.registerCenter = registerCenter;
        this.serviceSubscriber = subscriber;
    }


    /**
     *  offline for rpc service
     *  if user not call the method of ioc's registerShutHook()
     */
    {
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            //logger.debug("======= shutdownHook for client execute...");
            destroy();
            //logger.debug("======= shutdownHook for client execute end");
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
        ioc = applicationContext;
    }

    /**
     * start!  and keep schedule subscribe lists from redis;
     *
     * @param event
     */
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if(!ServerInfo.isInit){
            try {
                ServerInfo.refresh();
                ServerInfo.isInit = true;
            } catch (AppException e) {
                logger.warn("=============== fail to start Spring ioc, since: \n" + e.getMessage());
                ((GenericApplicationContext)ioc).close();
                return;
            }
        }
        if (this.timer == null) {
            timer = new Timer(true); //Daemon thread like jvm gc
        }
        if (this.timerTask == null) {
            timerTask = new ScheduleSubscribeTimerTask();
        }
        timer.scheduleAtFixedRate(timerTask, synSubscriptSeconds*1000, synSubscriptSeconds*1000);
        logger.debug("=============== Spring ioc started, and the timer task begin to run ===============");
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
        synchronized (this.isDone) {
            if (!this.isDone) {
                logger.debug("===== start to execute the method of destroy() =====");
                if (timer != null) {
                    timer.cancel();
                }
                disconnect();  // disconnect
                if (socketConfig != null) {
                    socketConfig.close();  //socket
                }
                registerCenter.close();
                ServerInfo.clear();
                this.isDone = true;
                logger.debug("===== the method of destroy() ended =====");
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


    public void checkSocket(){
        if (this.socketConfig == null) {
            socketConfig = new NettySocketConfig();  // default here
        }  // socket part; in case of null
    }

    public void checkSubscriber() {
        if(this.serviceSubscriber == null ||  this.registerCenter == null){
            registerCenter = new RedisRegisterCenterConfig();  // keep consistent with serviceSubscriber
            serviceSubscriber = new RedisServiceSubscriber();
        } // in case of null
    }

    /**
     * local service-related information (caches) provided for consumer
     * if user wants to acquire the current state of local service-related caches
     *
     * @return
     */
    public Map<String, Set<String>> getItfServersMap() {
        return ServerInfo.itfServersMap;
    }
    public Set<String> getServersList() {
        return ServerInfo.serversList;
    }
    public Map<String, Set<String>> getServicesNameMap() {
        return ServerInfo.servicesNameMap;
    }


    /* ---- if user wants to customize/acquire some configuration ---- */
    public ServerSelector getServerSelector() {
        return serverSelector;
    }

    public void setServerSelector(ServerSelector serverSelector) {
        this.serverSelector = serverSelector;
    }

    public SocketConfig getSocketConfig() {
        return socketConfig;
    }

    public void setSocketConfig(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public RegisterCenterConfig getRegisterCenter() {
        return registerCenter;
    }

    public void setRegisterCenter(RegisterCenterConfig registerCenter) {
        this.registerCenter = registerCenter;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public boolean isIocStarted() {
        return isIocStarted;
    }

    public String[] getItfPaths() {
        return itfPaths;
    }

    public void setItfPaths(String... itfPaths) {
        this.itfPaths = itfPaths;
    }

    public ServiceSubscriber getServiceSubscriber() {
        return serviceSubscriber;
    }

    public void setServiceSubscriber(ServiceSubscriber serviceSubscriber) {
        this.serviceSubscriber = serviceSubscriber;
    }

    public long getSynSubscriptSeconds() {
        return synSubscriptSeconds;
    }

    public void setSynSubscriptSeconds(long synSubscriptSeconds) {
        this.synSubscriptSeconds = synSubscriptSeconds;
    }

}

