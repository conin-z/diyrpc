package com.rpc.selector;

import com.rpc.message.RequestImpl;

import java.util.Set;

/**
 * @user KyZhang
 * @date
 */
public abstract class AbstractSelector implements ServerSelector{

    protected Set<String> serverListForItfClass;
    protected RequestImpl request;

    @Override
    public String select(Set<String> serverListForItfClass, RequestImpl request) {
        this.serverListForItfClass = serverListForItfClass;
        this.request = request;
        prepare();
        return doSelect(serverListForItfClass, request);
    }

    protected abstract void prepare();

    protected abstract String doSelect(Set<String> serverListForItfClass, RequestImpl request);

}
