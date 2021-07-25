package com.rpc.timertask;

import com.rpc.consumer.ServerInfo;
import com.rpc.exception.AppException;
import org.apache.log4j.Logger;


public class ScheduleSubscribeTimerTask extends AbstractTimerTask {

    private static final Logger logger = Logger.getLogger(ScheduleSubscribeTimerTask.class);

    public ScheduleSubscribeTimerTask(){
    }

    @Override
    public void run() {
        ServerInfo.isRefreshed = false;
        try {
            ServerInfo.refresh();
        } catch (AppException e) {
            logger.warn(e.getMessage());
        }
    }


}
