package com.rpc.provider.registry;

import com.rpc.registerconfig.RedisRegisterCenterConfig;
import com.rpc.provider.RpcServerConfiguration;
import com.rpc.utils.Constant;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;




/**
 * for server; export service
 * associated with this RedisRegisterCenterConfig
 *
 * @user KyZhang
 * @date
 */
public class RedisServiceRegistry implements ServiceRegistry{

    private static final Logger logger = Logger.getLogger(RedisServiceRegistry.class);

    private JedisPool pool;
    private RpcServerConfiguration server;

    public RedisServiceRegistry() {
        this.pool = RedisRegisterCenterConfig.getJedisPool();
    }


    @Override
    public void keepAlive(int seconds) {
        checkPool();
        try(Jedis jedis = pool.getResource()) {
            Long expire = jedis.expire(Constant.LOCAL_ADDRESS, seconds+1);
            if(expire > 0){
                logger.debug("====== server {" + Constant.LOCAL_ADDRESS + "} send expire command successful!");
            } else {
                logger.warn("== server {" + Constant.LOCAL_ADDRESS + "} send expire command error!");
            }
        }
    }


    @Override
    public void lclAddressToRegisterCenter() {
        checkPool();
        try(Jedis jedis = pool.getResource()) {
            this.server = RpcServerConfiguration.applicationContext.getBean(RpcServerConfiguration.class);
            long num = jedis.sadd(Constant.SERVER_LIST_NAME, Constant.LOCAL_ADDRESS);
            /* ONLINE msg to listener */
            Long listenerNum = jedis.publish(Constant.ONLINE, Constant.LOCAL_ADDRESS);
            logger.debug("================= send ONLINE msg to listeners of number { " + listenerNum + " } =================");
            if(null != server && !server.isRegistered()){
                server.setRegistered(true);
            }
            if(num > 0){
                logger.debug("register server of { " + Constant.LOCAL_ADDRESS + " } to redis key named {" + Constant.SERVER_LIST_NAME + " }");
            }else {
                logger.warn("element already a member for redis set");
            }
        }

    }


    @Override
    public void serviceToRegisterCenter(Class<?> clz)  {
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
                    if (this.server == null) {
                        server = RpcServerConfiguration.applicationContext.getBean(RpcServerConfiguration.class);
                    }
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
    public long removeElement(String key, String eleName) {
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
    public long deleteKey(String key) {
        checkPool();
        long res;
        try(Jedis resource = pool.getResource()) {
            res = resource.del(key);
            /* OFFLINE msg to listener */
            Long listenerNum = resource.publish(Constant.OFFLINE, key);
            logger.debug("================ send OFFLINE msg to listeners of number { " + listenerNum + " }  ===============");
        }
        return res;
    }


    private void checkPool() {
        if (pool == null || pool.isClosed()) {
            throw new IllegalArgumentException("error for jedis pool");
        }
    }


}
