package com.rpc.management;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;


/**
 * @user KyZhang
 * @date
 */
public interface RpcConfiguration extends ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>{

    void setApplicationContext(ApplicationContext applicationContext) throws BeansException;
    void onApplicationEvent(ContextRefreshedEvent event);
    void destroy();

}
