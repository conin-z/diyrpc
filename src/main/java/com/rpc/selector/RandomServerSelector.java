package com.rpc.selector;

import com.rpc.message.RequestImpl;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @user KyZhang
 * @date
 */
public class RandomServerSelector extends AbstractSelector{

    private SecureRandom random = new SecureRandom();

    @Override
    public String doSelect(Set<String> serverListForItfClass, RequestImpl request){
        List<String> list = new ArrayList<>(serverListForItfClass);
        int i = random.nextInt(list.size());
        return list.get(i);
    }
}
