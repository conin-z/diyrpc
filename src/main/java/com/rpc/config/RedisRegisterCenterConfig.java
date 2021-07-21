package com.rpc.config;

import com.rpc.utils.RedisServiceRegistry;
import com.rpc.constant.Constant;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.util.Properties;

/**
 * association with one instance of RpcConfiguration;
 * as formal parameter
 *
 * @user KyZhang
 * @date
 */
public class RedisRegisterCenterConfig implements RegisterCenterConfig {

    private static final Logger logger = Logger.getLogger(RedisRegisterCenterConfig.class);

    private String ip = "localhost";  //--by server set  //InetAddress.getLocalHost().getHostAddress()
    private int port = 6379;
    private String password = ""; // --by server set
    private int timeout = 300;   //connect over time for one client
    private int idl;
    private int maxActive = 128;  //number of connection
    private int expireSeconds = 30;
    private long maxWaitMillis = -1L;
    private boolean testOnReturn;
    private boolean testOnBorrow;

    private String jedisConfigPath;
    private static JedisPool pool;

    public RedisRegisterCenterConfig() {
        init(null);
    }

    public RedisRegisterCenterConfig(String jedisConfigPath) {
        this.jedisConfigPath = jedisConfigPath;
        init(jedisConfigPath);
    }

    public static JedisPool getJedisPool() {
        return pool;
    }

    /**
     * services offline from redis
     */
    public void offlineFromRedis(){
        if(null != pool && !pool.isClosed()){
            Jedis resource = pool.getResource();
            try {
                //server's all services remove     //  --DEL
                RedisServiceRegistry.deleteKey(resource, Constant.LOCAL_ADDRESS);
                logger.debug("===== deleted services from redis set {" + Constant.LOCAL_ADDRESS + "}");
                //  --SREM
                RedisServiceRegistry.removeElement(resource, Constant.REDIS_SERVER_LIST, Constant.LOCAL_ADDRESS);
                logger.debug("===== removed sever from redis set {" + Constant.REDIS_SERVER_LIST + "}");
            } finally {
                resource.close();
            }
            pool.close();
        }
    }


    /**
     * Open a way by .xml file to configure the jedisPool-related parameters
     *
     * @param configPath
     * @throws IOException
     */
    private void resolve(String configPath) throws IOException {
        Properties props = new Properties();
        props.load(RedisRegisterCenterConfig.class.getClassLoader().getResourceAsStream(configPath));
        // configuration for pool
        this.idl = Integer.valueOf(props.getProperty("jedis.pool.maxIdle"));
        this.maxActive = Integer.valueOf(props.getProperty("jedis.pool.maxActive"));
        this.maxWaitMillis = Long.valueOf(props.getProperty("jedis.pool.maxWait"));
        this.testOnBorrow = Boolean.valueOf(props.getProperty("jedis.pool.testOnBorrow"));
        this.testOnReturn = Boolean.valueOf(props.getProperty("jedis.pool.testOnReturn"));
        this.ip = props.getProperty("redis.ip");
        this.port = Integer.valueOf(props.getProperty("redis.port"));
        this.password = props.getProperty("redis.password");
        this.timeout = Integer.valueOf(props.getProperty("redis.timeout"));

    }


    /**
     * initialize the jedis pool
     *
     * @param configPath
     */
    public void init(String configPath) {
        JedisPoolConfig config = new JedisPoolConfig();

        if(configPath == null) {
            config.setMaxTotal(maxActive);
            pool = new JedisPool(config, ip);  //by default info
        } else{
            try {
                resolve(configPath);
                config.setMaxTotal(maxActive);
                config.setMaxIdle(idl);
                config.setMaxWaitMillis(maxWaitMillis);
                config.setTestOnBorrow(testOnBorrow);
                config.setTestOnReturn(testOnReturn);
                // instantiation for pool
                pool = new JedisPool(config, ip, port, timeout, password);
                logger.debug("====== redis connection success! ======");

            } catch(Exception e){
                logger.error("== fail for redis connection! ", e);
            }
        }

    }


    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getIdl() {
        return idl;
    }

    public void setIdl(int idl) {
        this.idl = idl;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public void setMaxWaitMillis(long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    public boolean isTestOnReturn() {
        return testOnReturn;
    }

    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public int getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(final int expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public long getTimerPeriod() {
        return 0;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

}
