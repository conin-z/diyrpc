package com.rpc.management;

import com.rpc.exception.AppException;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;


/**
 * @user KyZhang
 * @date
 */
public interface RpcConfig extends ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>{

    void checkSocket() throws AppException;

    void checkRegistry() throws AppException;
    void checkSubscriber() throws AppException;

    void destroy();


}
