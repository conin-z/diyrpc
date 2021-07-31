package com.rpc.registerconfig;

import com.rpc.consumer.ClientRpcConfig;
import com.rpc.management.RpcCriterion;
import com.rpc.provider.ServerRpcConfig;
import org.apache.log4j.Logger;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.util.Properties;


/**
 * association with (as constructor parameter of) the instance of {@code RpcConfig};
 *
 * since coupled to Jedis, {@code RedisRegisterCenterConfig} is set to final now
 * TODO: will refactor later so as to be able to use Redisson and others [extensibility]
 *
 * @see ServerRpcConfig
 * @see ClientRpcConfig
 * @user KyZhang
 * @date
 */
public final class RedisRegisterCenterConfig implements RegisterCenterConfig {

    private static final Logger logger = Logger.getLogger(RedisRegisterCenterConfig.class);

    private String ip = "localhost";  // can be set; InetAddress.getLocalHost().getHostAddress()
    private int port = 6379;
    private String password = ""; // can be set
    private int timeout = 300;   // connect over time for one client
    private int idl;
    private int maxActive = 128;  // number of connection
    private int expireSeconds = 30; // default 30s; can be set
    private long maxWaitMillis = -1L;
    private boolean testOnReturn;
    private boolean testOnBorrow;

    private String jedisConfigPath;
    private static JedisPool pool;


    public RedisRegisterCenterConfig() {
        initJedisPool();
    }

    public RedisRegisterCenterConfig(String jedisConfigPath) {
        this.jedisConfigPath = jedisConfigPath;
        initJedisPool();
    }

    public static JedisPool getJedisPool() {
        return pool;
    }


    /**
     * services offline from redis
     */
    public void close(){
        if(null != pool && !pool.isClosed()){
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
     * initialize the jedis pool at the end of instantiation
     */
    private void initJedisPool() {
        JedisPoolConfig config = new JedisPoolConfig();

        if(this.jedisConfigPath == null) {
            config.setMaxTotal(maxActive);
            pool = new JedisPool(config, ip);  //by default info
        } else{
            try {
                resolve(jedisConfigPath);
                config.setMaxTotal(maxActive);
                config.setMaxIdle(idl);
                config.setMaxWaitMillis(maxWaitMillis);
                config.setTestOnBorrow(testOnBorrow);
                config.setTestOnReturn(testOnReturn);
                // instantiation for pool
                pool = new JedisPool(config, ip, port, timeout, password);
                logger.debug("====== redis initialization is successful! ======");

            } catch(Exception e){
                logger.error("== fail for redis connection! ", e);
            }
        }

    }


    @Override
    public void show() {
        String info = "================================= status of Jedis pool : \n" +
                "{ " + pool.getNumActive() + " } ACTIVE JEDIS INSTANCES\n" +
                "{ " + pool.getNumWaiters() + " } BLOCKED THREADS\n" +
                "{ " + pool.getNumIdle() + " } ACTIVE JEDIS INSTANCES\n" +
                "=================================          =================================";
        logger.info(info);
    }


    @Override
    public void alter(RpcCriterion condition, Object input) {

    }



    //--------------------------------------------------------------------
    //                        get() and set()
    //     if user wants to customize/acquire some configuration
    //--------------------------------------------------------------------

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

    public String getJedisConfigPath() {
        return jedisConfigPath;
    }

    public void setJedisConfigPath(String jedisConfigPath) {
        this.jedisConfigPath = jedisConfigPath;
    }



}
