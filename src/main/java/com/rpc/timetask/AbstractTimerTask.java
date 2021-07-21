package com.rpc.timetask;


import java.util.TimerTask;

/**
 * just to collect timer logic at this level
 *
 * @user KyZhang
 * @date
 */
public abstract class AbstractTimerTask extends TimerTask {

    @Override
    public abstract void run();

}
