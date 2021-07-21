package com.rpc.proxy;

import com.rpc.config.*;
import com.rpc.config.RpcClientConfiguration;
import com.rpc.utils.RedisListSubscription;
import com.rpc.config.RedisRegisterCenterConfig;
import com.rpc.constant.Constant;
import com.rpc.exception.AppException;
import com.rpc.message.ServerInfo;
import com.rpc.utils.StringUtil;
import org.springframework.beans.factory.FactoryBean;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Proxy;

import java.util.HashSet;
import java.util.Set;

/**
 * associated with one Class<T> one serviceItf
 * associated with one 'RedisListSubscription'
 * 
 * @user KyZhang
 * @date
 */
public class RpcInterfaceProxyFactoryBean<T> implements FactoryBean {

    private RpcClientConfiguration consumer;
    private Class<T> itfClass;
    // as a temporary [value] for cache to collect related server info for itfClass [key]
    private Set<String> serverListForItf = new HashSet<>();

    // used by client  @Bean  or <Bean />  to declare this bean in Spring
    public RpcInterfaceProxyFactoryBean(Class<T> itfClass) {
        this.itfClass = itfClass;
    }

    public T getObject() throws Exception {
        consumer = RpcClientConfiguration.ioc.getBean(RpcClientConfiguration.class);
        SocketConfig nettyConfig = consumer.getSocketConfig();
        RedisRegisterCenterConfig registerCenter = consumer.getRegisterCenter();

        if (nettyConfig == null) {
            nettyConfig = new NettyConfig();
            consumer.setSocketConfig(nettyConfig);
        }  // netty part; in case of null

        if (RedisRegisterCenterConfig.getJedisPool() == null) {
            if(registerCenter == null){
                registerCenter = new RedisRegisterCenterConfig();
                consumer.setRegisterCenter(registerCenter);
            } // in case of null
        }  // redis part

        if(!ServerInfo.itfServersMapState.containsKey(itfClass.getName())){
            ServerInfo.itfServersMapState.put(itfClass.getName(), false);
        } // add new items regarding Services

        try {
            getServerListAndConnectAll(nettyConfig); //some caches involved
        } catch (AppException e) {
            e.printStackTrace();
        }

        if(serverListForItf.size() == 0){
            throw new AppException("=== {" + itfClass.getName() + "} has no implements in register center!");
        }

        Object o = Proxy.newProxyInstance(itfClass.getClassLoader(), new Class[]{itfClass}, new ClientProxyInvocation(itfClass));
        return (T)o;

    }

    /**
     * -- ServerInfo.serversList.addAll()  [ 1-level cache];
     * -- for(){  ServerInfo.serverChannels.put() }   [ 1-level cache];
     * -- for(){  ServerInfo.itfServersMap.put() }   [ 2-level cache]
     *
     * @param nettyConfig
     * @throws AppException
     */
    private void getServerListAndConnectAll(SocketConfig nettyConfig) throws AppException {
        Jedis jedis = RedisRegisterCenterConfig.getJedisPool().getResource();   //?
        try {
            // [all rpc-related interfaces, once]   --> cache for providers; later may be updated
            synchronized (ServerInfo.isInit){
                // when a certain itfClass created, servers get
                if (!ServerInfo.isInit) {
                    int num = 0;
                    boolean flag = true;
                    do {
                        Set<String> servers = RedisListSubscription.getRegisterList(jedis, Constant.REDIS_SERVER_LIST);  // redis set
                        if(servers != null && servers.size() > 0){
                            num = 0;
                            ServerInfo.isInit = true; // get servers --> init
                            //  ~~ ~ ~~ [1-level cache] ~~ ~ ~~
                            ServerInfo.serversList.addAll(servers);
                            flag = false;
                        }
                        if(servers == null || servers.size() == 0){
                            num++;
                            if(num > Constant.SUBSCRIBE_TIMES){
                                throw new AppException("no service provider for "+ itfClass.getSimpleName() +" in register center!");
                            }
                        }

                    } while (flag);  //  //update  3 times to try update
                } // once execute
            }

            String itfName = itfClass.getName();
            // [one itf, once] --> caches for service interfaces; later may be updated when the 1-level update
            // to speed up: may the Service set be prototype
            if (!ServerInfo.itfServersMapState.get(itfName)) {  //
                for(String server : ServerInfo.serversList){
                    Set<String> serviceItfs = ServerInfo.servicesNameMap.get(server);
                    if(serviceItfs == null){
                        // retry
                        serviceItfs = RedisListSubscription.getRegisterList(jedis, server);
                        if (serviceItfs == null || serviceItfs.size() == 0) {
                            // jedis.srem(Constant.REDIS_SERVER_LIST, server);
                            // no available service for this server
                            // invalid provider; remove from registry center by consumer
                            RedisListSubscription.removeElement(jedis, Constant.REDIS_SERVER_LIST, server);
                        }
                        ServerInfo.servicesNameMap.put(server, serviceItfs);  //additional info updates
                    }

                    // serverListForItf.add     ~~ ~ ~~ [2-level cache] ~~ ~ ~~
                    // connect(nettyConfig, server, serviceItfs);
                    if(serviceItfs != null && serviceItfs.contains(itfName)){
                        String[] ipPort = StringUtil.resolveIpPortFromString(server);
                        if(ipPort != null && ipPort.length >= 2){
                            nettyConfig.connectAndSave(ipPort[0], ipPort[1], serverListForItf);
                        }
                    }

                }
                ServerInfo.itfServersMapState.put(itfName, true);
            }
            ServerInfo.itfServersMap.put(itfName, serverListForItf);  //save to ServerInfo  (may be updated)

        } finally {
            jedis.close();
        }

    }


    public Class<?> getObjectType() {
        return itfClass;
    }

    public boolean isSingleton() {
        return true;
    }


}
