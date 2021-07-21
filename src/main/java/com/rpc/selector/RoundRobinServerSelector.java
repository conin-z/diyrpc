package com.rpc.selector;

import com.rpc.message.RequestImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * RoundRobin; in order
 *
 * @user KyZhang
 * @date
 */
public class RoundRobinServerSelector extends AbstractSelector{
    /**
     * 'index' indicates the number of servers currently selected
     * increments each time invoked
     */
    private int index = 0;

    @Override
    public String doSelect(Set<String> serverListForItfClass, RequestImpl request){
        List<String> list = new ArrayList<>(serverListForItfClass);
        if(index >= list.size()){
            index %= list.size();
        }
        return list.get(index++);
    }
}
