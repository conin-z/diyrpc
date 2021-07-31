package com.rpc.management;

import com.rpc.consumer.subscriber.RedisServiceSubscriber;
import com.rpc.consumer.subscriber.ServiceSubscriber;
import com.rpc.provider.registry.RedisServiceRegistry;
import com.rpc.provider.registry.ServiceRegistry;
import com.rpc.registerconfig.RedisRegisterCenterConfig;
import com.rpc.registerconfig.RegisterCenterConfig;
import com.rpc.socket.SocketConfig;
import com.rpc.timertask.DefaultRpcStatus;
import com.rpc.timertask.StatusObserver;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class AbstractRpcConfig implements RpcConfig {

    /** --------------- socket-related -------------- */
    protected SocketConfig socketConfig;

    /** --------------- redis-related --------------- */
    protected RegisterCenterConfig registerCenter;
    protected ServiceSubscriber serviceSubscriber; // used for rpc consumer
    protected ServiceRegistry serviceRegistry; // used for rpc provider
    protected volatile Boolean isRegistered = false;

    /** ---------- scheduled timer-related ---------- */
    protected int corePoolSizeForConcurrentTimer = 5;
    protected static ScheduledThreadPoolExecutor timer;
    protected StatusObserver socketObserver =  new StatusObserver(new DefaultRpcStatus());
    protected StatusObserver registerCenterObserver = new StatusObserver(new DefaultRpcStatus());
    protected long socketObservePeriod = 60l;
    protected long socketObserveDelay = 60l;
    protected long centerObservePeriod = 60l;
    protected long centerObserveDelay = 60l;

    protected volatile Boolean isIocStarted = false;
    protected volatile Boolean offlineOnce = false;


    @Override
    public void checkRegistry() {
        if(this.registerCenter == null || this.serviceRegistry == null){
            registerCenter = new RedisRegisterCenterConfig();   // default
            registerCenterObserver.setStates(registerCenter);
            serviceRegistry = new RedisServiceRegistry(); // default: Redis way
        }
    }


    @Override
    public void checkSubscriber() {
        if(this.serviceSubscriber == null ||  this.registerCenter == null){
            registerCenter = new RedisRegisterCenterConfig();  // keep consistent with serviceSubscriber
            registerCenterObserver.setStates(registerCenter);
            serviceSubscriber = new RedisServiceSubscriber();
        } // in case of null
    }


    public final void startDefaultTimerTasks() {
        timer = new ScheduledThreadPoolExecutor(corePoolSizeForConcurrentTimer);
        doStartDefaultTimerTasks();
        addTimerTask(registerCenterObserver, centerObserveDelay, centerObservePeriod, TimeUnit.SECONDS);
        addTimerTask(socketObserver, socketObserveDelay, socketObservePeriod, TimeUnit.SECONDS);
        addTimerTask(new StatusObserver(new DefaultRpcStatus()), socketObserveDelay*2, socketObservePeriod*2, TimeUnit.SECONDS);
    }


    protected abstract void doStartDefaultTimerTasks();


    /**
     * user to add some timer tasks
     *
     * @param task
     * @param delay
     * @param period
     * @param unit
     */
    public void addTimerTask(Runnable task, long delay, long period, TimeUnit unit) {
        timer.scheduleAtFixedRate(task, delay, period, unit);
    }


    public void closeTimer() throws InterruptedException {
        if (this.timer != null) {
            timer.shutdown();
            boolean isClosed;
            do {
                isClosed = timer.awaitTermination(1, TimeUnit.HOURS);
            } while(!isClosed);
        }
    }




    //--------------------------------------------------------------------
    //                        get() and set()
    //     if user wants to customize/acquire some configuration
    //--------------------------------------------------------------------

    public boolean isIocStarted() {
        return isIocStarted;
    }

    public SocketConfig getSocketConfig() {
        return socketConfig;
    }

    public void setSocketConfig(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
        socketObserver.setStates(socketConfig);
    }

    public RegisterCenterConfig getRegisterCenter() {
        return registerCenter;
    }

    public void setRegisterCenter(RegisterCenterConfig registerCenter) {
        this.registerCenter = registerCenter;
        registerCenterObserver.setStates(registerCenter);
    }

    public void setServiceSubscriber(ServiceSubscriber serviceSubscriber) {
        this.serviceSubscriber = serviceSubscriber;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public ServiceSubscriber getServiceSubscriber() {
        return serviceSubscriber;
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public Boolean getRegistered() {
        return isRegistered;
    }

    public Boolean getIocStarted() {
        return isIocStarted;
    }

    public int getCorePoolSizeForConcurrentTimer() {
        return corePoolSizeForConcurrentTimer;
    }

    public void setCorePoolSizeForConcurrentTimer(int corePoolSizeForConcurrentTimer) {
        this.corePoolSizeForConcurrentTimer = corePoolSizeForConcurrentTimer;
    }

    public StatusObserver getSocketObserver() {
        return socketObserver;
    }

    public void setSocketObserver(StatusObserver socketObserver) {
        this.socketObserver = socketObserver;
    }

    public StatusObserver getRegisterCenterObserver() {
        return registerCenterObserver;
    }

    public void setRegisterCenterObserver(StatusObserver registerCenterObserver) {
        this.registerCenterObserver = registerCenterObserver;
    }

    public long getSocketObservePeriod() {
        return socketObservePeriod;
    }

    public void setSocketObservePeriod(long socketObservePeriod) {
        this.socketObservePeriod = socketObservePeriod;
    }

    public long getSocketObserveDelay() {
        return socketObserveDelay;
    }

    public void setSocketObserveDelay(long socketObserveDelay) {
        this.socketObserveDelay = socketObserveDelay;
    }

    public long getCenterObservePeriod() {
        return centerObservePeriod;
    }

    public void setCenterObservePeriod(long centerObservePeriod) {
        this.centerObservePeriod = centerObservePeriod;
    }

    public long getCenterObserveDelay() {
        return centerObserveDelay;
    }

    public void setCenterObserveDelay(long centerObserveDelay) {
        this.centerObserveDelay = centerObserveDelay;
    }
}
