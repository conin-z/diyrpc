package com.rpc.consumer.subscriber;

import com.rpc.provider.ServerRpcConfig;
import com.rpc.registerconfig.RedisRegisterCenterConfig;
import com.rpc.consumer.ServerInfo;
import com.rpc.utils.Constant;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.Set;

/**
 * for client, to subscribe the RPC service-related information from Redis
 *
 * @user KyZhang
 * @date
 */
public class RedisServiceSubscriber implements ServiceSubscriber {

    private static final Logger logger = Logger.getLogger(RedisServiceSubscriber.class);

    protected final JedisPool pool;

    public RedisServiceSubscriber(RedisRegisterCenterConfig centerConfig) {
        pool = centerConfig.getJedisPool();
        init();
    }


    /**
     * do some initialization, such as: to listen to some redis channels
     * subclasses can override this method to cancel the channel listening, or add other initialization work
     */
    protected void init(){
        checkPool();
        new Thread(() -> {
            try(Jedis jedis = pool.getResource()){
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        String server = message;
                        switch (channel){
                            case Constant.ONLINE:
                                logger.info("============ received ONLINE message of server {" + message + "} from channel {" + channel + "} ==============");
                                ServerInfo.addServer(server);
                                break;
                            case Constant.OFFLINE:
                                logger.info("============ received OFFLINE message of server {" + message + "} from channel {" + channel + "} ==============");
                                ServerInfo.removeServer(server);
                        }
                    }
                }, Constant.SUBSCRIPTION_CHANNEL_PATTERN);
            }
        }).start();
    }


    /**
     * here client can get aches regarding all servers or the services of a certain server
     *
     * @param key
     * @return
     */
    @Override
    public final Set<String> getServiceList(String key){
        checkPool();
        Set<String> set;
        try(Jedis jedis = pool.getResource()){
            set = jedis.smembers(key);
        }
        return set;
    }


    /**
     * here client can remove element from the redis set with key's name $key$
     *
     * @param key
     * @param eleName
     * @return
     */
    public final long removeElement(String key, String eleName){
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
