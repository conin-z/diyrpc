package com.rpc.message;

import io.netty.channel.Channel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;

/**
 * metadata associated with one client;
 * contains some service-related [caches] , response saved queues
 *
 * @user KyZhang
 * @date
 */
public class ServerInfo {

    //open once
    public static Boolean isInit = false;

    /*** [key attribute]  // ~~ ~ [1-level cache] ~~ ~ */
    public static Map<String, Set<String>> itfServersMap = new ConcurrentHashMap(); // serviceName<-K,V->servers containing service
    public static Map<String, Boolean> itfServersMapState = new ConcurrentHashMap();
    /*** [key attribute]  // ~~ ~ [2-level cache] ~~ ~ */
    public static Set<String> serversList = new HashSet();   // aware:  redis set this server lies in : all servers in register center
    public static Map<String, Set<String>> servicesNameMap = new ConcurrentHashMap<String, Set<String>>(); // aware:  serverName<-K,V->services server contains
    /*** [key attribute]  // ~~ ~ [2-level cache for io connection] ~~ ~ */
    public static Map<String, Channel> serverChannelMap = new ConcurrentHashMap(); // serverName<-K,V->channel

    // request<-K,V->responses queue  --> [could extend] : use rabbitmq here with MQConfig ??
    public static Map<String, SynchronousQueue<ResponseImpl>> msgTransferMap = new ConcurrentHashMap<String, SynchronousQueue<ResponseImpl>>(); // requestID<-K,V->responses

    public static void clear(){
        serversList.clear();
        serverChannelMap.clear();
        itfServersMap.clear();
        itfServersMapState.clear();
        servicesNameMap.clear();
        msgTransferMap.clear();
    }

}
