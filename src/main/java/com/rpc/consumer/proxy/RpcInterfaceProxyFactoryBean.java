package com.rpc.consumer.proxy;

import com.rpc.consumer.RpcClientConfiguration;
import com.rpc.exception.AppException;
import com.rpc.consumer.ServerInfo;
import com.rpc.provider.RpcServerConfiguration;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Proxy;

/**
 * associated with one Class<T> one serviceItf
 * 
 * @user KyZhang
 * @date
 */
public class RpcInterfaceProxyFactoryBean<T> implements FactoryBean {

    private static final Logger logger = Logger.getLogger(RpcInterfaceProxyFactoryBean.class);

    private Class<T> itfClass;

    // used by client  @Bean  or <Bean />  to declare this bean in Spring
    public RpcInterfaceProxyFactoryBean(Class<T> itfClass) {
        this.itfClass = itfClass;
        if(!ServerInfo.isCandidatesSavedForItfMap.containsKey(itfClass.getName())){
            ServerInfo.isCandidatesSavedForItfMap.put(itfClass.getName(), false);
        } // here first collect remote services
    }

    /**
     *
     * @return
     * @throws AppException
     *      * case 1 : no service provider in register center
     *      * case 2 : no service provider for some remote service in register center
     */
    public T getObject() throws AppException{
        RpcClientConfiguration consumer = RpcClientConfiguration.ioc.getBean(RpcClientConfiguration.class);
        consumer.checkSocket();
        this.checkCandidatesForItf();
        consumer.checkSubscriber();

        Object o = Proxy.newProxyInstance(itfClass.getClassLoader(), new Class[]{itfClass}, new ClientProxyInvocation(itfClass));
        return (T)o;
    }


    private void checkCandidatesForItf() throws AppException {
        if(!ServerInfo.isInit){
            logger.debug("=============== begin to initialize local caches... ");
            ServerInfo.refresh();
            ServerInfo.isInit = true;
        }
        String itfName = itfClass.getName();
        if (!ServerInfo.isCandidatesSavedForItfMap.get(itfName)) {
            if(ServerInfo.itfServersMap.get(itfName).size() == 0){
                // try again or exception occurs
                throw new AppException("== no service provider for remote service {" + itfClass.getName() + "} in register center! ==");
            }else {
                ServerInfo.isCandidatesSavedForItfMap.put(itfName, true);
            }
        }
    }



    public Class<?> getObjectType() {
        return itfClass;
    }

    public boolean isSingleton() {
        return true;
    }


}
