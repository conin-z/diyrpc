package com.rpc.provider;

import com.rpc.annotation.RpcService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;

import com.rpc.exception.AppException;
import com.rpc.management.RpcConfiguration;
import com.rpc.registerconfig.*;
import com.rpc.provider.registry.RedisServiceRegistry;
import com.rpc.provider.registry.ServiceRegistry;
import com.rpc.socket.NettySocketConfig;
import com.rpc.socket.SocketConfig;
import com.rpc.utils.Constant;

import com.rpc.timertask.ExpireTimerTask;
import org.apache.log4j.Logger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * entrance for server
 *
 * @user KyZhang
 * @date
 */
public class RpcServerConfiguration implements RpcConfiguration, BeanPostProcessor, InitializingBean {

    private static final Logger logger = Logger.getLogger(RpcServerConfiguration.class);

    public static ApplicationContext applicationContext;

    private SocketConfig socketConfig;
    private int port;  //--by server set

    private volatile Boolean isRegistered = false;  // there is one service registered
    private RegisterCenterConfig registerCenter;  // --by server set
    private Timer timer;   // keep heartbeats with register center
    private ExpireTimerTask timerTask;

    private ServiceRegistry serviceRegistry;

    private volatile Boolean isIocStarted = false;
    public volatile Boolean isOffline = false;


    public RpcServerConfiguration() {
    }

    public RpcServerConfiguration(RegisterCenterConfig registerCenter, ServiceRegistry registry) {
        /* [RegisterCenterConfig] and [ServiceRegistry], these two are used together */
        this.registerCenter = registerCenter;
        this.serviceRegistry = registry;
    }



    /**
     * when entire server has shutdown:
     *    netty module shutdown : groups' close;
     *    registry module shutdown : remove self;
     *    others like : Timer, DataBase(here not);
     */
    {
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            destroy();
        }));

    }


    //get info (could like spring to implements itf_aware)
    public SocketConfig getSocketConfig() {
        return socketConfig;
    }
    public int getPort() {
        return port;
    }
    public RegisterCenterConfig getRegisterCenter() {
        return registerCenter;
    }
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }
    public boolean isRegistered() { return isRegistered; }
    public boolean isIocStarted() { return isIocStarted; }
    public Timer getTimer() { return timer; }
    public ExpireTimerTask getTimerTask() { return timerTask; }

    /* ---- if user wants to customize some configuration ---- */
    public void setRegisterCenter(RegisterCenterConfig registerCenter) {
        this.registerCenter = registerCenter;
    }
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }
    public void setSocketConfig(SocketConfig socketConfig) { this.socketConfig = socketConfig; }
    public void setPort(int port) { this.port = port; }
    public void setTimer(Timer timer) { this.timer = timer; }
    public void setTimerTask(ExpireTimerTask timerTask) { this.timerTask = timerTask; }
    public void setRegistered(boolean registered) { isRegistered = registered; }



    /**
     * listener process
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        serverRegister();
        isIocStarted = true;
        logger.debug("=============== server application started! ===============");
    }

    /**
     * ioc aware
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RpcServerConfiguration.applicationContext = applicationContext;
    }


    /**
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }


    /**
     * open serverSocket and init config when 'this rpcConfig bean' initializing
     * (in ioc 'refresh()' , single --> once)
     */
    @Override
    public void afterPropertiesSet() throws AppException {
        socketInit();
    }


    /**
     * register service to registerCenter
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clz = bean.getClass();
        /* with @RpcService marked */
        if(clz.isAnnotationPresent(RpcService.class)){
            /* [RedisRegisterCenterConfig] and [RedisServiceRegistry], these two are used together */
            if(this.registerCenter == null || this.serviceRegistry == null){
                registerCenter = new RedisRegisterCenterConfig();   // default
                serviceRegistry = new RedisServiceRegistry(); // default: Redis way
            }
            serviceRegistry.serviceToRegisterCenter(clz);
        }
        return bean;
    }



    private void socketInit() throws AppException {
        if (socketConfig == null) {
            socketConfig = new NettySocketConfig(); // if not set by user, use default: NettyConfig
        }

        try {
            Constant.LOCAL_ADDRESS  = InetAddress.getLocalHost().getHostAddress() + Constant.IP_PORT_GAP + port;
        } catch (UnknownHostException e) {
           logger.error("find server address error!");
           throw new AppException("======= find server address error! =======");
        }

        socketConfig.serverInit(port);
        logger.debug("====== init server socket successful! ====== ");
    }


    /**
     *  server online [after the remote services' register]
     */
    private void serverRegister(){
        try {

            serviceRegistry.lclAddressToRegisterCenter();

            if (this.timer == null) {
                timer = new Timer(true);
            }
            int expireSeconds = registerCenter.getExpireSeconds();
            if(this.timerTask == null){
                timerTask = new ExpireTimerTask(serviceRegistry, expireSeconds);
            }
            timer.scheduleAtFixedRate(timerTask, expireSeconds*1000, expireSeconds*1000);

            logger.debug("====== register server successful! ======");
        } catch (Exception e) {
            logger.error(e.getMessage());   // pool fail
        }

    }


    /**
     *  close RPC
     */
    @Override
    public void destroy() {
        synchronized (this.isOffline) {
            if (!this.isOffline) {
                logger.debug("===== start to execute the method of destroy() =====");
                if (this.timer != null) {
                    timer.cancel();   //stop timer, such as the task of sending heartbeat with redis
                }
                if (this.registerCenter != null) {  // service offline
                    if (serviceRegistry != null) {
                        offlineFromRegisterCenter();
                    }
                    registerCenter.close();
                }
                try {
                    Thread.sleep(Constant.WAIT_REFER_UPDATE);  //allow time for the consumer to update the service-related cache
                } catch (InterruptedException e) {
                    logger.warn("Interrupted unexpectedly during shutdown process!");
                }
                if (this.socketConfig != null) {
                    socketConfig.close(); //netty's graceful shutdown, including msg flush, sockets release ..
                }
                this.isOffline = true;
                logger.debug("===== the method of destroy() ended =====");
            }
        }
    }

    private void offlineFromRegisterCenter() {
        serviceRegistry.deleteKey(Constant.LOCAL_ADDRESS);  //  --DEL
        logger.debug("===== deleted services of redis set {" + Constant.LOCAL_ADDRESS + "}");
        //server's all services remove
        serviceRegistry.removeElement(Constant.SERVER_LIST_NAME, Constant.LOCAL_ADDRESS); // --SREM
        logger.debug("===== removed server from redis set {" + Constant.SERVER_LIST_NAME + "}");
    }



}