package com.rpc.timertask;

import com.rpc.consumer.ServerInfo;
import com.rpc.exception.AppException;
import org.apache.log4j.Logger;

/**
 * the task of regular subscription from the central service list to update the local cache
 */
public class ScheduleSubscribeTimerTask extends AbstractTimerTask {

    private static final Logger logger = Logger.getLogger(ScheduleSubscribeTimerTask.class);

    public ScheduleSubscribeTimerTask(){
    }

    @Override
    public void run() {
        ServerInfo.isRefreshed = false;
        try {
            logger.debug("===== scheduled task for subscription is to begin... =====");
            ServerInfo.refresh();
        } catch (AppException e) {
            logger.warn(e.getMessage());
        }
    }


}
