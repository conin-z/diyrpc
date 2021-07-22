package com.rpc.config;

import com.rpc.constant.Constant;
import com.rpc.message.ServerInfo;
import com.rpc.proxy.RpcItfScanner;
import com.rpc.selector.RandomServerSelector;
import com.rpc.selector.ServerSelector;
import com.rpc.timetask.AbstractTimerTask;
import com.rpc.utils.RedisListSubscription;
import com.rpc.utils.StringUtil;

import io.netty.channel.Channel;

import org.apache.log4j.Logger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import redis.clients.jedis.Jedis;

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

    /* ----- ------services ache-related------------- */
        //*** [key attribute]   ~~ ~ ~~ [2-level cache] ~~ ~ ~~
    private Map<String, List<String>> itfServersMap; //aware  :  all servers and their containing services
        //*** [key attribute]   ~~ ~ ~~ [2-level cache] ~~ ~ ~~
    private Set<String> serversList;   //aware:  lists this server lies in
        //*** [assist attribute]
    private Map<String, Set<String>> servicesNameMap; // all servers and their available services in register center

    private String[] itfPaths; //for scan service interfaces   --by consumer set

    /* ---------------netty-related--------------- */
    private SocketConfig socketConfig = new NettyConfig();  //default netty
    private int port;
    private ServerSelector serverSelector = new RandomServerSelector(); //default; //load balancing

    /* --------------- redis-related--------------- */
    private RedisRegisterCenterConfig registerCenter;  // --can set by client
    private Timer timer; // keep
    private long synSubscriptSeconds = 30;  // --by client set
    private String jedisConfigPath; // to locate jedisPool's config resource

    private volatile boolean isIocStarted;


    public RpcClientConfiguration() {
    }

    public RpcClientConfiguration(RedisRegisterCenterConfig registerCenter) { this.registerCenter = registerCenter; }


    /**
     *  offline for rpc service
     *  if user not call the method of ioc's registerShutHook()
     */
    {
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            logger.debug("======= shutdownHook for client execute...");
            destroy();
            logger.debug("======= shutdownHook for client execute end");
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
        if(!isIocStarted){
            logger.debug("====== ioc started, and the timer runs ======");
            timer = new Timer(true); //Daemon thread like jvm gc
            timer.scheduleAtFixedRate(new ScheduleSubscribeTimerTask(this), synSubscriptSeconds*1000, synSubscriptSeconds*1000);
            isIocStarted = true;
        }
    }




    /**
     * close rpc when user has called the method of context's registerShutHook():
     *      1.hook registry  2. doClose :
     *                            way1:   ApplicationListener<ContextClosedEvent>'s onApplicationEvent()
     *                            way2:   LifeCycle's stop()
     *                            way3:   Disposable's destroy()
     *
     * @throws Exception
     */
    public void destroy(){
        logger.debug("===== start to execute the method of destroy() =====");
        if (timer != null) {
            timer.cancel();
        }
        disconnect();  // disconnect
        if (socketConfig != null) {
            socketConfig.close();  //socket
        }
        ServerInfo.clear();
        logger.debug("===== the method of destroy() ended =====");
    }

    private void disconnect() {
        for (String server : ServerInfo.serverChannelMap.keySet()) {
            Channel ch = ServerInfo.serverChannelMap.get(server);
            if(ch != null && ch.isOpen()){
                ch.close();  //say goodbye
            }
        }
    }


    private static class ScheduleSubscribeTimerTask extends AbstractTimerTask {

        private RedisRegisterCenterConfig centerConfig;
        private SocketConfig socketConfig;

        public ScheduleSubscribeTimerTask() { }

        public ScheduleSubscribeTimerTask(RpcClientConfiguration config){
            this.centerConfig = config.registerCenter;
            this.socketConfig = config.socketConfig;
        }

        @Override
        public void run() {
            try (Jedis jedis = centerConfig.getJedisPool().getResource()) {
                synchronized (ServerInfo.isInit) {
                    Set<String> servers = RedisListSubscription.getRegisterList(jedis, Constant.REDIS_SERVER_LIST);
                    if (servers != null && servers.size() > 0) {

                        //update 1-level cache [ServerInfo.serversList]
                        Set<String> old = ServerInfo.serversList;
                        ServerInfo.serversList = servers;
                        logger.debug("====== consumer {" + Constant.LOCAL_ADDRESS + "} update serversList successful!");

                        //update cache [ServerInfo.servicesNameMap]
                        ServerInfo.servicesNameMap.clear();
                        for (String server : servers) {
                            Set<String> set = RedisListSubscription.getRegisterList(jedis, server);
                            ServerInfo.servicesNameMap.put(server, set);
                        }

                        // update cache [ServerInfo.itfServersMap]
                        Set<String> itfSet = ServerInfo.itfServersMap.keySet();  //local @RpcReference serviceItfs
                        // update cache [ServerInfo.serverChannelMap] & [ServerInfo.itfServersMap]
                        for (String itf : itfSet) {

                            // new servers online  !!!
                            for (String server : servers) {
                                if(!old.contains(server) && ServerInfo.servicesNameMap.get(server).contains(itf)){
                                    //ServerInfo.serverChannelMap.put(server, null);
                                    //connectAndSave (include itf--server)
                                    String[] ipPort = StringUtil.resolveIpPortFromString(server);
                                    if(ipPort != null){
                                        socketConfig.connectAndSave(ipPort[0], ipPort[1], ServerInfo.itfServersMap.get(itf));
                                    }
                                }
                            }
                            // some servers offline  !!!
                            for (String server : old) {
                                if(!servers.contains(server)){
                                    ServerInfo.serverChannelMap.remove(server);
                                    ServerInfo.itfServersMap.get(itf).remove(server);
                                }
                            }
                        }
                        logger.debug("===== local caches updated successful! ========");
                        logger.debug("=== online servers:\n " + ServerInfo.serversList);
                        logger.debug("=== online servers and their services:\n " + ServerInfo.servicesNameMap);
                        logger.debug("=== remote services we need and available servers:\n " + ServerInfo.itfServersMap);
                    }
                }
            } catch (Exception e) {
                logger.error("== fail for consumer {" + Constant.LOCAL_ADDRESS + "} update serversList!", e);
            }
        }
    }



    /**
     * service-related information (aches) provided for consumer
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


    public ServerSelector getServerSelector() {
        return serverSelector;
    }

    public void setServerSelector(ServerSelector serverSelector) {
        this.serverSelector = serverSelector;
    }

    public String getJedisConfigPath() {
        return jedisConfigPath;
    }

    public void setJedisConfigPath(String jedisConfigPath) {
        this.jedisConfigPath = jedisConfigPath;
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

    public RedisRegisterCenterConfig getRegisterCenter() {
        return registerCenter;
    }

    public void setRegisterCenter(RedisRegisterCenterConfig registerCenter) {
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

    public long getSynSubscriptSeconds() {
        return synSubscriptSeconds;
    }

    public void setSynSubscriptSeconds(long synSubscriptSeconds) {
        this.synSubscriptSeconds = synSubscriptSeconds;
    }

}
