package com.rpc.config;

import com.rpc.annotations.RpcService;

import java.util.Timer;

import com.rpc.constant.Constant;
import com.rpc.exception.AppException;
import com.rpc.timetask.AbstractTimerTask;
import com.rpc.utils.RedisServiceRegistry;

import org.apache.log4j.Logger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import redis.clients.jedis.Jedis;

/**
 * entrance for server
 *
 * @user KyZhang
 * @date
 */
public class RpcServerConfiguration implements RpcConfiguration, BeanPostProcessor, InitializingBean {

    private static final Logger logger = Logger.getLogger(RpcServerConfiguration.class);

    public static ApplicationContext ioc;

    private SocketConfig socketConfig = new NettyConfig();
    private int port;  //--by server set

    private volatile boolean isRegistered;  // there is one service registered
    private RedisRegisterCenterConfig registerCenter;  // --by server set
    private Timer timer;   // keep heartbeats with redis
    private long synRedisSeconds; // default 30s , saved in RedisConfig // --can set by server

    private volatile boolean isIocStarted;


    public RpcServerConfiguration() {
    }

    public RpcServerConfiguration(RedisRegisterCenterConfig registerCenter) {
        this.registerCenter = registerCenter;
        RedisServiceRegistry.setRegisterCenter(registerCenter);
    }


    /**
     * when entire server has shutdown:
     *    netty module shutdown : groups' close;
     *    registry module shutdown : remove self;
     *    others like : Timer, DataBase(here not);
     */
    {
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            logger.debug("======= shutdownHook for server offline execute...");
            destroy();
            logger.debug("======= shutdownHook for server offline execute end");
        }));

    }


    //get info (could like spring to implements itf_aware)
    public SocketConfig getSocketConfig() {
        return socketConfig;
    }
    public int getPort() {
        return port;
    }
    public RedisRegisterCenterConfig getRegisterCenter() {
        return registerCenter;
    }
    public boolean isRegistered() { return isRegistered; }
    public boolean isIocStarted() { return isIocStarted; }
    public Timer getTimer() { return timer; }
    public long getSynRedisSeconds() { return synRedisSeconds; }

    //if user want to change parameters
    public void setRegisterCenter(RedisRegisterCenterConfig registerCenter) {
        this.registerCenter = registerCenter;
    }
    public void setSocketConfig(NettyConfig socketConfig) { this.socketConfig = socketConfig; }
    public void setPort(int port) { this.port = port; }
    public void setTimer(Timer timer) { this.timer = timer; }
    public void setRegistered(boolean registered) { isRegistered = registered; }
    public void setSynRedisSeconds(long synRedisSeconds) { this.synRedisSeconds = synRedisSeconds; }


    /**
     * listener process
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        isIocStarted = true;
        logger.debug("====== single beans registered into spring ioc; server application started! ======");
    }

    /**
     * ioc aware
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ioc = applicationContext;
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
     * register services with redis
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clz = bean.getClass();
        if(clz.isAnnotationPresent(RpcService.class)){   //with annotation @RpcService marked

            if (RedisServiceRegistry.getServer() == null) {
                RedisServiceRegistry.setServer(this); //aware
            }

            try {
                RedisServiceRegistry.serviceToRegisterCenter(clz);
            } catch (AppException e) {
                logger.error(e.getMessage(), e);
            }

            if (isRegistered) {
                if (timer == null) {
                    timer = new Timer(true);
                }
                synRedisSeconds = registerCenter.getExpireSeconds();
                timer.scheduleAtFixedRate(new ExpireTimerTask(registerCenter), synRedisSeconds*1000, synRedisSeconds*1000);
            }

        }
        return bean;
    }



    /**
     * open serverSocket and init config when 'this rpcConfig bean' initializing
     * (in ioc 'refresh()' , single --> once)
     */
    @Override
    public void afterPropertiesSet() {
        nettyInit();
        serverRegister();
    }


    private void nettyInit() {
        if (socketConfig == null) {
            socketConfig = new NettyConfig();
        }
        socketConfig.serverInit(port);
        logger.debug("====== init server socket successful! ====== ");
    }


    /**
     *  service online
     */
    private void serverRegister(){
        if(registerCenter == null){
            registerCenter = new RedisRegisterCenterConfig();   //
            RedisServiceRegistry.setRegisterCenter(registerCenter);
        }

        try {
            if (RedisServiceRegistry.getServer() == null) {
                RedisServiceRegistry.setServer(this);
            }
            RedisServiceRegistry.lclAddressToRegisterCenter();
            logger.debug("====== register server successful! ======");
        } catch (AppException e) {
            logger.debug(e.getMessage(), e);   // pool fail
        }

    }


    /**
     *  close RPC module
     */
    @Override
    public void destroy() {
        logger.debug("===== start to execute the method of destroy() =====");
        if (timer != null) {
            timer.cancel();   //stop timer, such as the task of sending heartbeat with redis
        }
        if (registerCenter != null) {
            registerCenter.offlineFromRedis();  //redis service offline
        }
        try {
            Thread.sleep(Constant.WAIT_REFER_UPDATE);  //allow time for the consumer to update the service-related cache
        } catch (InterruptedException e) {
            logger.warn("Interrupted unexpectedly during shutdown process!");
        }
        if (socketConfig != null) {
            socketConfig.close(); //netty's graceful shutdown, including msg flush, sockets release ..
        }
        logger.debug("===== the method of destroy() ended =====");
    }


    /**
     * customize the timer task
     */
    private static class ExpireTimerTask extends AbstractTimerTask {

        private RedisRegisterCenterConfig config;

        public ExpireTimerTask(RedisRegisterCenterConfig centerConfig){
            this.config = centerConfig;
        }

        @Override
        public void run() {
            try(Jedis jedis = config.getJedisPool().getResource()) {
                Long expire = jedis.expire(Constant.LOCAL_ADDRESS, config.getExpireSeconds());
                if(expire > 0){
                    logger.debug("====== server {"+Constant.LOCAL_ADDRESS +"} send expire command successful!");
                }
            } catch (Exception e) {
                logger.error("== server {"+Constant.LOCAL_ADDRESS +"} send expire command error!", e);
            }
        }

    }


}