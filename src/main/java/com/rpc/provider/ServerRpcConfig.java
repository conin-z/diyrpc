package com.rpc.provider;

import com.rpc.annotation.RpcService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.rpc.exception.AppException;
import com.rpc.management.AbstractRpcConfig;
import com.rpc.management.RpcConfig;
import com.rpc.management.RpcStatus;
import com.rpc.registerconfig.*;
import com.rpc.provider.registry.ServiceRegistry;
import com.rpc.socket.NettyServerSocketConfig;
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
public class ServerRpcConfig extends AbstractRpcConfig implements RpcConfig, BeanPostProcessor, InitializingBean {

    private static final Logger logger = Logger.getLogger(ServerRpcConfig.class);

    public static ApplicationContext applicationContext;
    private int port;

    private ExpireTimerTask expireTimerTask;

    private static AtomicInteger numRpcServiceProvided = new AtomicInteger();

    public volatile static long onlineMoment;


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

    public ServerRpcConfig(final int port) { this.port = port; }

    public ServerRpcConfig(RegisterCenterConfig registerCenter, ServiceRegistry registry, final int port) {
        this(port);
        /* RegisterCenterConfig and ServiceRegistry, these two are used together */
        this.registerCenter = registerCenter;
        this.serviceRegistry = registry;
        if(RpcStatus.class.isAssignableFrom(registerCenter.getClass())){
            registerCenterObserver.setStatus(registerCenter);
        }
    }


    /**
     * ioc aware
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ServerRpcConfig.applicationContext = applicationContext;
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
            checkRegistry();
            serviceRegistry.serviceToRegisterCenter(clz);
            numRpcServiceProvided.incrementAndGet();
        }
        return bean;
    }



    /**
     * open serverSocket and init config when 'this rpcConfig bean' initializing
     * (in ioc 'refresh()' , single --> once)
     */
    @Override
    public void afterPropertiesSet() throws AppException {
        checkSocket();
    }


    @Override
    public void checkSocket() throws AppException {
        try {
            Constant.LOCAL_ADDRESS  = InetAddress.getLocalHost().getHostAddress() + Constant.IP_PORT_GAP + port;
        } catch (UnknownHostException e) {
           logger.error("find server address error!");
           throw new AppException("======= find server address error! =======");
        }
        if (this.socketConfig == null) {
            socketConfig = new NettyServerSocketConfig(port); // if not set by user, use default: NettyConfig
            socketObserver.setStatus(socketConfig);
        }
        socketConfig.init();
        logger.info("====== init server's RPC socket successful! ====== ");
    }


    /**
     * server online [after the remote services' register]
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        isIocStarted = true;
        logger.info("==================== server application context started! ====================");
        if(numRpcServiceProvided.get() == 0){ // no service with @RpcService / no service to provide for consumers -> do not register
            logger.warn("=== no RPC service to provide for consumers -> do not go to register ===");
            return;
        }
        serviceRegistry.lclAddressToRegisterCenter();
        isRegistered = true;  // all remote services and server itself have been registered
        onlineMoment = System.currentTimeMillis();
        logger.info("============= register server successful! providing { " + numRpcServiceProvided + " } RPC service APIs ======");
        startDefaultTimerTasks();
        logger.info("============= server's default timer tasks begin to run ============");
    }


    @Override
    public void startDefaultTimerTasks() {
        int expireSeconds = registerCenter.getExpireSeconds();
        if(expireTimerTask == null){
            expireTimerTask = new ExpireTimerTask(expireSeconds);
        }
        addTimerTask(expireTimerTask, expireSeconds, expireSeconds, TimeUnit.SECONDS);
        super.startDefaultTimerTasks();
    }



    /**
     *  close RPC
     */
    @Override
    public void destroy() {
        synchronized (offlineOnce) {
            if (!offlineOnce) {
                logger.info("===== start to execute the method { destroy() } =====");
                try {
                    closeTimer();
                    if (registerCenter != null) {  // services offline
                        if (serviceRegistry != null) {
                            offlineFromRegisterCenter();
                            Thread.sleep(Constant.WAIT_REFER_UPDATE);  //allow time for the consumer to update the service-related cache
                        }
                        registerCenter.close();
                    }
                } catch (InterruptedException e) {
                    logger.warn("Interrupted unexpectedly during shutdown process");
                }
                if (socketConfig != null) {
                    socketConfig.close(); //io socket's graceful shutdown, including msg flush, sockets release ..
                }

                offlineOnce = true;
                logger.info("===== method { destroy() } called by shutdownHook ended =====");
            }
        }
    }


    private void offlineFromRegisterCenter() {
        serviceRegistry.deleteKey(Constant.LOCAL_ADDRESS);
        logger.info("===== deleted services of redis set {" + Constant.LOCAL_ADDRESS + "}");
        //server's all services remove
        serviceRegistry.removeElement(Constant.SERVER_LIST_NAME, Constant.LOCAL_ADDRESS);
        logger.info("===== removed server from redis set {" + Constant.SERVER_LIST_NAME + "}");
    }



    //get info (could like spring to implements itf_aware)
    public boolean isRegistered() { return isRegistered; }
    public ExpireTimerTask getExpireTimerTask() { return expireTimerTask; }
    public int getPort() { return port; }

    /* ---- if user wants to customize some configuration ---- */
    public void setExpireTimerTask(ExpireTimerTask expireTimerTask) { this.expireTimerTask = expireTimerTask; }
    public void setPort(int port) { this.port = port; }
}