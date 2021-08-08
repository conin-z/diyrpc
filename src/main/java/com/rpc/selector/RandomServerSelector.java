package com.rpc.selector;

import com.rpc.message.RequestImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @user KyZhang
 * @date
 */
public class RandomServerSelector extends AbstractSelector{

    @Override
    protected void prepare() {

    }


    @Override
    protected String doSelect(Set<String> serverListForItfClass, RequestImpl request){
        List<String> list = new ArrayList<>(serverListForItfClass);
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

}
