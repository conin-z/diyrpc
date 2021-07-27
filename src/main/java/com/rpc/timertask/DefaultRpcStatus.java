package com.rpc.timertask;

import com.rpc.management.RpcStatus;
import org.apache.log4j.Logger;

/**
 * OS-related information: memory usage, CPU, number of threads in the current process
 */
public class DefaultRpcStatus implements RpcStatus {


    private static final Logger logger = Logger.getLogger(DefaultRpcStatus.class);

    @Override
    public void show() {
        Runtime runtime = Runtime.getRuntime();
        String runtimeInfo = "processors={"+ runtime.availableProcessors() +"}, " +
                "freeMemory={"+ runtime.freeMemory() +"B}, " +
                "maxMemory={"+ runtime.maxMemory() +"B}, " +
                "totalMemory={"+ runtime.totalMemory() +"B}";
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        ThreadGroup topGroup = group;
        // get the root thread group
        while (group != null) {
            topGroup = group;
            group = group.getParent();
        }
        int slackSize = topGroup.activeCount() * 2;
        Thread[] slackThreads = new Thread[slackSize];
        int actualSize = topGroup.enumerate(slackThreads);
        Thread[] actualThreads = new Thread[actualSize];
        System.arraycopy(slackThreads, 0, actualThreads, 0, actualSize);
        StringBuilder threadInfo = new StringBuilder();
        for (Thread thread : actualThreads) {
            threadInfo.append("T_NAME : " + thread.getName() + "\n");
        }

        logger.info( "===============================  NOW IN THIS PROGRAM : "
                + "threads size is { " + actualThreads.length +" }, including \n" + threadInfo +
                "===============================  ABOUT THE SYSTEM : " + runtimeInfo + "\n");
    }

    @Override
    public void alter() {

    }

}
