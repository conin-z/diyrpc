package com.rpc.consumer.subscriber;

import com.rpc.registerconfig.RedisRegisterCenterConfig;
import com.rpc.consumer.ServerInfo;
import com.rpc.utils.Constant;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.Set;

/**
 * associated with one 'RpcInterfaceProxyFactoryBean<T>' --> Class<T> one serviceItf;
 * for client, refer service
 *
 * @user KyZhang
 * @date
 */
public class RedisServiceSubscriber implements ServiceSubscriber {

    private static final Logger logger = Logger.getLogger(RedisServiceSubscriber.class);

    private static JedisPool pool;

    public RedisServiceSubscriber() {
        pool = RedisRegisterCenterConfig.getJedisPool();
        init(); // such as: to listen to some redis channels
    }


    /**
     * here client can get aches regarding all servers or the services of a certain server
     *
     * @param key
     * @return
     */
    @Override
    public Set<String> getServiceList(String key){
        checkPool();
        Set<String> set;
        try(Jedis jedis = pool.getResource()){
            set = jedis.smembers(key);
        }
        return set;
    }


    @Override
    public void init(){
        checkPool();
        new Thread(() -> {
            try(Jedis jedis = pool.getResource()){
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        String server = message;
                        switch (channel){
                            case Constant.ONLINE:
                                logger.debug("============ received ONLINE message of server {" + message + "} from channel {" + channel + "} ==============");
                                ServerInfo.addServer(server);
                                break;
                            case Constant.OFFLINE:
                                logger.debug("============ received OFFLINE message of server {" + message + "} from channel {" + channel + "} ==============");
                                ServerInfo.removeServer(server);
                        }
                    }
                }, Constant.SUBSCRIPTION_CHANNEL_PATTERN);
            }
        }).start();
    }

    /**
     * here client can remove element from the redis set with key's name $key$
     *
     * @param key
     * @param eleName
     * @return
     */
    public long removeElement(String key, String eleName){
        checkPool();
        long res;
        try(Jedis jedis = pool.getResource()) {
            res = jedis.srem(key, eleName);
        }
        return res;
    }


    private void checkPool() {
        if (pool == null || pool.isClosed()) {
            throw new IllegalArgumentException("error for jedis pool");
        }
    }


}
