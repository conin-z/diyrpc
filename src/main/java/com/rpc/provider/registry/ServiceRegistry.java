package com.rpc.provider.registry;



/**
 * for server; export service-related information to register center
 *
 * @user KyZhang
 * @date
 */
public interface ServiceRegistry {


    /**
     * maintain the online status with register center
     * (to set/keep keys' life with register center)
     *
     * @param seconds
     */
    void keepAlive(int seconds);


    /**
     * register a service-interface with register center
     *
     * @param serviceItf
     */
    void serviceToRegisterCenter(Class<?> serviceItf);


    /**
     * register the ip-port information of server with register center
     */
    void lclAddressToRegisterCenter();


    /**
     * offline all the service-interfaces from register center
     *
     * @param key
     * @return
     */
    long deleteKey(String key);


    /**
     * offline one service-interface or remove server information
     *
     * @param key
     * @param eleName
     * @return
     */
    long removeElement(String key, String eleName);

}
