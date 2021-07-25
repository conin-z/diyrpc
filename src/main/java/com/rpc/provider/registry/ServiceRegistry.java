package com.rpc.provider.registry;


import com.rpc.exception.AppException;

/**
 * open to
 *
 * @user KyZhang
 * @date
 */
public interface ServiceRegistry {

    void keepAlive(int seconds);

    void serviceToRegisterCenter(Class<?> serviceItf);

    void lclAddressToRegisterCenter();

    long deleteKey(String key);

    long removeElement(String key, String eleName);

}
