package com.rpc.selector;

import com.rpc.message.RequestImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RoundRobin; in order
 *
 * @user KyZhang
 * @date
 */
public class RoundRobinServerSelector extends AbstractSelector{
    /**
     * 'index' indicates the number of servers currently selected
     * increments each time when invoked
     */
    private AtomicInteger index = new AtomicInteger();

    @Override
    public String doSelect(Set<String> serverListForItfClass, RequestImpl request){
        List<String> list = new ArrayList<>(serverListForItfClass);
        if(index.get() >= list.size()){
            index.compareAndSet(list.size(), index.get() % list.size());
        }
        return list.get(index.addAndGet(1));
    }
}
