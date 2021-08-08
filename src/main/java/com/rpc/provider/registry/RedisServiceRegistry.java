package com.rpc.provider.registry;

import com.rpc.registerconfig.RedisRegisterCenterConfig;
import com.rpc.provider.ServerRpcConfig;
import com.rpc.utils.Constant;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


/**
 * for server; export service-related information to Redis
 * associated with {@link RedisRegisterCenterConfig}
 *
 * @user KyZhang
 * @date
 */
public class RedisServiceRegistry implements ServiceRegistry{

    private static final Logger logger = Logger.getLogger(RedisServiceRegistry.class);

    protected final JedisPool pool;

    public RedisServiceRegistry(RedisRegisterCenterConfig centerConfig){
        pool = centerConfig.getJedisPool();
        init();
    }


    /**
     * do some initialization
     * subclasses can override this method to cancel the channel listening, or add other initialization work
     */
    protected void init(){

    }


    @Override
    public final void keepAlive(int seconds) {
        checkPool();
        try(Jedis jedis = pool.getResource()) {
            jedis.expire(Constant.LOCAL_ADDRESS, seconds + 1);
            logger.info("====== server { " + Constant.LOCAL_ADDRESS + " } send expire command successful!");
        }
    }


    @Override
    public final void lclAddressToRegisterCenter() {
        checkPool();
        try(Jedis jedis = pool.getResource()) {
            // ONLINE msg to listener
            Long listenerNum = jedis.publish(Constant.ONLINE, Constant.LOCAL_ADDRESS);
            logger.info("================= send ONLINE msg to listeners of number { " + listenerNum + " } =================");
            // register with center
            Long num = jedis.sadd(Constant.SERVER_LIST_NAME, Constant.LOCAL_ADDRESS);
            if(num > 0){
                logger.debug("register server of { " + Constant.LOCAL_ADDRESS + " } to redis key named {" + Constant.SERVER_LIST_NAME + " }");
            }else {
                logger.warn("element already a member of the redis set");
            }
        }

    }


    @Override
    public final void serviceToRegisterCenter(Class<?> clz)  {
        Class<?>[] interfaces = clz.getInterfaces();

        if (interfaces != null && interfaces.length == 1){    //single itf
            Class<?> itf = interfaces[0];
            String infName = itf.getName();

            checkPool();
            try(Jedis resource = pool.getResource()) {
                //login in
                Long num = resource.sadd(Constant.LOCAL_ADDRESS, infName);
                if(num > 0){
                    logger.debug("service { " + infName + " } of server is published!");
                }else {
                    logger.warn("service { " + infName + " } of server already published");
                }
            }

        }else {
            logger.warn("service {" + clz.getName() + "} has more one interface or no interface opened!");
        }
    }


    /**
     * here server can remove element from the redis set with key's name $key$
     *
     * @param key
     * @param eleName
     * @return
     */
    @Override
    public final long removeElement(String key, String eleName) {
        checkPool();
        long res;
        try(Jedis resource = pool.getResource()) {
            res = resource.srem(key, eleName);
        }
        return res;
    }


    /**
     * delete one key from redis
     *
     * @param key
     * @return
     */
    public final long deleteKey(String key) {
        checkPool();
        long res;
        try(Jedis resource = pool.getResource()) {
            // remove itself from register center
            res = resource.del(key);
            // OFFLINE msg to listener
            Long listenerNum = resource.publish(Constant.OFFLINE, key);
            logger.info("================ send OFFLINE msg to listeners of number { " + listenerNum + " }  ===============");
        }
        return res;
    }


    private void checkPool() {
        if (pool == null || pool.isClosed()) {
            throw new IllegalArgumentException("error for jedis pool");
        }
    }


}
