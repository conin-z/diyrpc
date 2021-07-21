package com.rpc.utils;

import com.rpc.config.RedisRegisterCenterConfig;
import com.rpc.config.RpcServerConfiguration;
import com.rpc.constant.Constant;
import com.rpc.exception.AppException;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.net.InetAddress;



/**
 * for server; export service
 *
 * @user KyZhang
 * @date
 */
public class RedisServiceRegistry{

    private static final Logger logger = Logger.getLogger(RedisServiceRegistry.class);

    private static RedisRegisterCenterConfig registerCenter;
    private static RpcServerConfiguration server;
    private static JedisPool pool;


    public static RpcServerConfiguration getServer() {
        return server;
    }

    public static void setRegisterCenter(RedisRegisterCenterConfig registerCenter) { RedisServiceRegistry.registerCenter = registerCenter; }

    public static void setServer(RpcServerConfiguration server) {
        RedisServiceRegistry.server = server;
    }

    public static void serviceToRegisterCenter(Class<?> clz) throws AppException {
        Class<?>[] interfaces = clz.getInterfaces();

        if (interfaces != null && interfaces.length == 1){    //single itf
            Class<?> itf = interfaces[0];
            String infName = itf.getName();

            if (pool == null) {
                pool = registerCenter.getJedisPool();
                if (pool == null){
                    logger.error("error for jedis pool");
                    throw new AppException("error for jedis pool");
                }
            }
            Jedis resource = pool.getResource();
            try {
                //login in
                Long num = resource.sadd(Constant.LOCAL_ADDRESS, infName);
                if(num > 0){
                    logger.debug("service { " + infName + " } of server is published!");
                    if(!server.isRegistered()){
                        server.setRegistered(true);
                    }
                }else {
                    logger.error("fail for publishing service { " + infName + " } of server...");
                }
            } finally {
                if(resource != null)
                    resource.close();
            }
        }else {
            logger.warn("service {" + clz.getName() + "} has more one interface or no interface opened!");
        }
    }

    
    public static void lclAddressToRegisterCenter() throws AppException {

        pool = registerCenter.getJedisPool();

        if (pool == null){
            logger.error("error for jedis pool");
            throw new AppException("error for jedis pool");
        }
        
        try(Jedis jedis = pool.getResource()) {
            String lclAddress = InetAddress.getLocalHost().getHostAddress() + Constant.IP_PORT_GAP + server.getPort();  //server info register
            long num = jedis.sadd(Constant.REDIS_SERVER_LIST, lclAddress);
            Constant.LOCAL_ADDRESS = lclAddress;
            if(num > 0){
                logger.debug("register server of { " + lclAddress + " } to redis key named {" + Constant.REDIS_SERVER_LIST + " }");
            }else {
                logger.error("element already a member for redis set!");
            }

        } catch (Exception e) {
            logger.error("find server address error!");
        }

    }

    /**
     * here server can remove element from the redis set with key's name $key$
     *
     * @param jedis
     * @param key
     * @param eleName
     * @return
     */
    public static long removeElement(Jedis jedis, String key, String eleName){
        return jedis.srem(key, eleName);
    }

    /**
     * delete one key from redis
     *
     * @param jedis
     * @param key
     * @return
     */
    public static long deleteKey(Jedis jedis, String key){
        return jedis.del(key);
    }


}
