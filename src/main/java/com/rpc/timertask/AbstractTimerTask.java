package com.rpc.timertask;


/**
 * just to collect timer logic at this level
 *
 * @user KyZhang
 * @date
 */
public abstract class AbstractTimerTask implements Runnable {

    @Override
    public abstract void run();

}
