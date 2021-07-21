package com.rpc.selector;

import com.rpc.message.RequestImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @user KyZhang
 * @date
 */
public abstract class AbstractSelector implements ServerSelector{

    @Override
    public String select(Set<String> serverListForItfClass, RequestImpl request) {
        return doSelect(serverListForItfClass, request);
    }

    public abstract String doSelect(Set<String> serverListForItfClass, RequestImpl request);

}
