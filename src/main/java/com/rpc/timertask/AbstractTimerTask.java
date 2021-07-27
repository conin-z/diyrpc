package com.rpc.timertask;


import java.util.TimerTask;

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
