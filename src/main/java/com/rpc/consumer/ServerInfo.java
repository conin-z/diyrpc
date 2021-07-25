package com.rpc.consumer;

import com.rpc.socket.SocketConfig;
import com.rpc.consumer.subscriber.ServiceSubscriber;
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
import java.util.concurrent.SynchronousQueue;

/**
 * contains some service-related [caches], socket connection cache, response saved queues;
 * and some corresponding operation
 *
 * @user KyZhang
 * @date
 */
public class ServerInfo {

    private static final Logger logger = Logger.getLogger(ServerInfo.class);

    public volatile static Boolean isInit = false;  //open once
    public volatile static Boolean isRefreshed = false;

    /*** [key attribute]  // ~~ ~ [1-level cache] ~~ ~ */
    public volatile static Map<String, Set<String>> itfServersMap = new ConcurrentHashMap(); // serviceName<-K,V->servers containing service
    public volatile static Map<String, Boolean> isCandidatesSavedForItfMap = new ConcurrentHashMap();  // the keySet is the list saving remote services
    /*** [key attribute]  // ~~ ~ [2-level cache] ~~ ~ */
    public volatile static Set<String> serversList = new HashSet();   // aware:  redis set this server lies in : all servers in register center
    private volatile static Set<String> serversListOld;  // for refresh
    public volatile static Map<String, Set<String>> servicesNameMap = new ConcurrentHashMap<String, Set<String>>(); // aware:  serverName<-K,V->services server contains
    /*** [key attribute]  // ~~ ~ [2-level cache for io connection] ~~ ~ */
    public volatile static Map<String, Channel> serverChannelMap = new ConcurrentHashMap(); // serverName<-K,V->channel

    // request<-K,V->responses queue  --> [could extend] : use rabbitmq here with MQConfig ??
    public volatile static Map<String, SynchronousQueue<ResponseImpl>> msgTransferMap = new ConcurrentHashMap<String, SynchronousQueue<ResponseImpl>>(); // requestID<-K,V->responses


    /**
     *
     * @throws AppException
     *              : there is no service provider in register center
     */
    public static void refresh() throws AppException{
        Set<String> servers;
        if (!isRefreshed) {
            synchronized (isRefreshed) {
                if (!isRefreshed) {
                    int num = 0;
                    do {
                        RpcClientConfiguration consumer = RpcClientConfiguration.ioc.getBean(RpcClientConfiguration.class);
                        consumer.checkSubscriber();
                        ServiceSubscriber subscriber = consumer.getServiceSubscriber();
                        servers = subscriber.getServiceList(Constant.SERVER_LIST_NAME);
                        if (servers != null && servers.size() > 0) {
                            serversListOld = new HashSet<>(serversList);  // begin: size == 0
                            serversList.clear();
                            serversList.addAll(servers);
                            doRefresh();
                            isRefreshed = true; // get servers --> init
                            return;
                        } else {
                            num++;
                            if (num > Constant.SUBSCRIBE_TIMES) {
                                throw new AppException("============== no service provider in register center! ============");
                            }
                        }
                    } while (true);  // update 3 times to try update
                }
            }
        }
    }


    private static void doRefresh() {
        /* some servers offline */
        for (String server : serversListOld) {
            if (!serversList.contains(server)) {
                removeServer(server);
            }
        }
        /* some servers online (some server will restart)
           for restarted server: consider timestamp to mark or directly think they are all new */
        for (String server : serversList) {
            addServer(server);  // if invalid, remove from register center
        }

        logger.debug("====== consumer {" + Constant.LOCAL_ADDRESS + "} update local Service_Candidate_Caches successful!");
        logger.debug("\n=========== remote services required and available candidates:\n " + ServerInfo.itfServersMap
                + "\n=========== online servers in register center:\n " + ServerInfo.serversList
                + "\n=========== online servers and their available services:\n " + ServerInfo.servicesNameMap);

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
            serverChannelMap.remove(server);  // since connection establishment is time-consuming
        }
        // the 1-level
        Set<String> itfSet = ServerInfo.isCandidatesSavedForItfMap.keySet(); // get the list of remote service
        for (String itf : itfSet) {
            ServerInfo.itfServersMap.get(itf).remove(server);
        }
    }


    /**
     * when refresh cache; or get ONLINE message from event publisher
     *
     * @param server
     */
    public static void addServer(String server) {
        RpcClientConfiguration consumer = RpcClientConfiguration.ioc.getBean(RpcClientConfiguration.class);
        consumer.checkSocket();
        consumer.checkSubscriber();

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
                boolean isOpen = socketConfig.connect(ipPort[0], ipPort[1]);
                if (isOpen) {
                    serversList.add(server);
                    servicesNameMap.put(server, services);
                    Set<String> itfSet = isCandidatesSavedForItfMap.keySet();
                    for (String itfName : itfSet) {
                        saveCandidateForItf(server, itfName);
                    }
                }else {
                    logger.warn("=== fail to connect service candidate {" + server + "} ===");
                    /* retry or discard */
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
            Set<String> serverListForItf = new HashSet<>();
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


}
