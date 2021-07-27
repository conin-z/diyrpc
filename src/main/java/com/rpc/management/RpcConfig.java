package com.rpc.management;

import com.rpc.exception.AppException;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.concurrent.TimeUnit;


/**
 * @user KyZhang
 * @date
 */
public interface RpcConfig extends ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>{

    void destroy();

    void startDefaultTimerTasks();
    void addTimerTask(Runnable task, long delay, long period, TimeUnit unit);
    void closeTimer() throws InterruptedException;

    void checkSocket() throws AppException;
    void checkRegistry() throws AppException;
    void checkSubscriber() throws AppException;

}
