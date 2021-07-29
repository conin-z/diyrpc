package com.rpc.consumer;

import com.rpc.management.RpcCriterion;
import com.rpc.management.RpcStatus;
import com.rpc.socket.SocketConfig;
import com.rpc.consumer.subscriber.ServiceSubscriber;
import com.rpc.timertask.StatusObserver;
import com.rpc.utils.Constant;
import com.rpc.exception.AppException;
import com.rpc.message.ResponseImpl;
import com.rpc.utils.StringUtil;
import io.netty.channel.Channel;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * contains some service-related caches: socket connection cache, response saved queues, etc.
 * and some corresponding operation
 *
 * @user KyZhang
 * @date
 */
public final class ServerInfo implements RpcStatus {

    private static final Logger logger = Logger.getLogger(ServerInfo.class);

    public volatile static Boolean isRefreshed = false;
    public static AtomicBoolean isInit = new AtomicBoolean(); //open once
    private static AtomicInteger countForRetry = new AtomicInteger();

    /** [key attribute]  // [1-level cache]  */
    public volatile static Map<String, Set<String>> itfServersMap = new ConcurrentHashMap(); // serviceName<-K,V->servers containing service
    public volatile static Map<String, Boolean> isCandidatesSavedForItfMap = new ConcurrentHashMap();  // the keySet is the list saving remote services
    /** [key attribute]  // [2-level cache]  */
    public volatile static Set<String> serversList = new CopyOnWriteArraySet<>();   // aware:  redis set this server lies in : all servers in register center
    private volatile static Set<String> serversListOld;  // for refresh
    public volatile static Map<String, Set<String>> servicesNameMap = new ConcurrentHashMap<String, Set<String>>(); // aware:  serverName<-K,V->services server contains
    /** [key attribute]  // [2-level cache for io connection]  */
    public volatile static Map<String, Channel> serverChannelMap = new ConcurrentHashMap(); // serverName<-K,V->channel

    /** request<-K,V->responses queue  --> could extend: use rabbitmq here with MQConfig [TODO]   */
    public volatile static Map<String, SynchronousQueue<ResponseImpl>> msgTransferMap = new ConcurrentHashMap<String, SynchronousQueue<ResponseImpl>>(); // requestID<-K,V->responses



    /**
     * refresh local caches
     *
     * @throws AppException   there is no service provider in register center
     */
    public static void refresh() throws AppException{
        Set<String> servers;
        if (!isRefreshed) {
            do {
                ClientRpcConfig consumer = ClientRpcConfig.applicationContext.getBean(ClientRpcConfig.class);
                ServiceSubscriber subscriber = consumer.getServiceSubscriber();
                servers = subscriber.getServiceList(Constant.SERVER_LIST_NAME);
                if (servers != null && servers.size() > 0) {
                    synchronized (isRefreshed) {
                        if (!isRefreshed) {
                            serversListOld = new HashSet<>(serversList);  // init: size == 0
                            serversList.clear(); // reentrantLock
                            serversList.addAll(servers);
                            doRefresh();
                            isRefreshed = true; // get servers --> init
                        }
                    }
                    return;
                } else {
                   countForRetry.addAndGet(1);
                    if (countForRetry.get() >= Constant.SUBSCRIBE_TIMES) {
                        countForRetry.compareAndSet(Constant.SUBSCRIBE_TIMES,0);
                        throw new AppException("============== no service provider in register center! ============");
                    }
                }
            } while (true);  // 3 times (may by multiple threads concurrently) to try to update
        }

    }


    private static void doRefresh() {
        // some servers offline
        for (String server : serversListOld) {
            if (!serversList.contains(server)) {
                removeServer(server);
            }
        }
        /*
         * some servers online (some server will restart)
         * for restarted server: consider timestamp to mark or directly think they are all new
         */
        for (String server : serversList) {
            serversList.remove(server); // here remove in advance; if valid, will re-add in addServer();
            addServer(server);  // if invalid, remove from register center
        }

        logger.info("================================= update local Service_Candidate_Caches successful! ");
        logger.info(getCacheStatus());
    }


    /**
     * when refresh cache; or get OFFLINE message from event publisher
     *
     * @param server
     */
    public static void removeServer(String server) {
        serversList.remove(server);
        // the 2-level
        servicesNameMap.remove(server);
        if (serverChannelMap.get(server) != null) {
            if (serverChannelMap.get(server).isOpen()) {
                serverChannelMap.get(server).close();
            }
            serverChannelMap.remove(server);  // connection establishment is time-consuming
        }
        // the 1-level
        Set<String> itfSet = ServerInfo.isCandidatesSavedForItfMap.keySet(); // get the list of remote service
        for (String itf : itfSet) {
            itfServersMap.get(itf).remove(server);
        }
    }


    /**
     * when refreshing cache; or getting ONLINE message from event publisher;
     *
     * may not actually add this service candidate for some reason:
     *          1. invalid provider : no remote service provided
     *          2. cannot parse corresponding service information correctly
     *          3. fail to connect
     *
     * @param server
     */
    public static void addServer(String server) {
        ClientRpcConfig consumer = ClientRpcConfig.applicationContext.getBean(ClientRpcConfig.class);
        ServiceSubscriber serviceSubscriber = consumer.getServiceSubscriber();
        SocketConfig socketConfig = consumer.getSocketConfig();

        Set<String> services = serviceSubscriber.getServiceList(server);
        if (services == null || services.size() == 0) {  // if invalid provider
            serviceSubscriber.removeElement(Constant.SERVER_LIST_NAME, server);
            logger.warn("=== invalid service candidate {" + server + "} ===");
        }else {
            String[] ipPort = StringUtil.resolveIpPortFromString(server);
            if(ipPort != null && ipPort.length == 2){
                // connect and if ok, save into channelMap
                boolean isDone = socketConfig.connect(ipPort[0], Integer.parseInt(ipPort[1]));
                if (isDone) {
                    serversList.add(server);
                    servicesNameMap.put(server, services);
                    Set<String> itfSet = isCandidatesSavedForItfMap.keySet();
                    for (String itfName : itfSet) {
                        saveCandidateForItf(server, itfName);
                    }
                }else {
                    logger.warn("=== fail to connect service candidate {" + server + "} ===");
                    // retry or discard
                    serviceSubscriber.removeElement(Constant.SERVER_LIST_NAME, server); // here discard
                }
            } else {
                logger.warn("=== cannot parse correctly for the service provider {" + server + "} ===");
            }
        }

    }


    private static void saveCandidateForItf(String candidate, String itfName) {
        Set<String> services = servicesNameMap.get(candidate);
        if(!itfServersMap.containsKey(itfName)){
            Set<String> serverListForItf = new CopyOnWriteArraySet<>();
            itfServersMap.put(itfName, serverListForItf);
        }
        if(services.contains(itfName)){
            itfServersMap.get(itfName).add(candidate);
        }
    }


    public static void clear(){
        serversList.clear();
        serverChannelMap.clear(); // has already closed channels
        itfServersMap.clear();
        isCandidatesSavedForItfMap.clear();
        servicesNameMap.clear();
        msgTransferMap.clear();
    }


    /**
     * as callback in {@link StatusObserver}
     * to show the status of caches at scheduled time
     */
    @Override
    public void show() {
        logger.info(getCacheStatus());
    }


    /**
     * [TODO]
     *
     * @param condition   the trigger condition have to be met if want to change status
     * @param inputs    some of the parameters needed when altering
     */
    @Override
    public void alter(RpcCriterion condition, Object... inputs) {

    }


    /**
     *
     * @return  local rpc service-related information (caches) provided for consumer
     */
    private static String getCacheStatus() {
        return  "================================= { " + ClientRpcConfig.numRpcServiceNeed.get() + " } rpc service we need, and :\n" +
                "============= rpc services we need and available candidates :\n" + itfServersMap +
                "\n============= online servers in register center :\n" + serversList +
                "\n============= and their containing services :\n" + servicesNameMap +
                "\n=================================          =================================";

    }

}
