package com.rpc.utils;

import redis.clients.jedis.Jedis;

import java.util.Set;

/**
 * associated with one 'RpcInterfaceProxyFactoryBean<T>' --> Class<T> one serviceItf;
 * for client, refer service
 *
 * @user KyZhang
 * @date
 */
public class RedisListSubscription {

    /**
     * here client can get aches regarding all servers or the services of a certain server
     *
     * @param jedis
     * @param key
     * @return
     */
    public static Set<String> getRegisterList(Jedis jedis,String key){
        return jedis.smembers(key);
    }

    /**
     * here client can remove element from the redis set with key's name $key$
     *
     * @param jedis
     * @param key
     * @param eleName
     * @return
     */
    public static long removeElement(Jedis jedis, String key, String eleName){
        return jedis.srem(key, eleName);
    }

}
