package com.rpc.proxy;

import com.rpc.config.RpcClientConfiguration;
import com.rpc.constant.Constant;
import com.rpc.exception.AppException;
import com.rpc.message.*;
import com.rpc.selector.ServerSelector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;

/**
 * @user KyZhang
 * @date
 */
public class ClientProxyInvocation implements InvocationHandler {

    private static final Logger logger = Logger.getLogger(ClientProxyInvocation.class);
    //target class
    private Class<?> itfClass;

    public ClientProxyInvocation(Class<?> itfClass) {
        this.itfClass = itfClass;
    }

    public ClientProxyInvocation() {
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
        ServerSelector selector = RpcClientConfiguration.ioc.getBean(RpcClientConfiguration.class).getServerSelector();
        // new request
        RequestImpl request = new RequestImpl(MessageType.SERVER);
        request.setRequestId(String.valueOf(System.currentTimeMillis()));
        request.setItf(itfClass);
        request.setItfName(itfClass.getName());
        request.setMethodName(method.getName());
        request.setResponseType(method.getReturnType().getName());
        request.setArgs(args);
        request.setParaClassTypes(method.getParameterTypes());

        SynchronousQueue<ResponseImpl> responses = new SynchronousQueue<ResponseImpl>();
        ServerInfo.msgTransferMap.put(request.getRequestId(), responses);
        ResponseImpl response;
        try {
            boolean flag;
            int num = Constant.SERVER_RESELECT_TIMES;
            do {

                if(num != Constant.SERVER_RESELECT_TIMES) {
                    logger.warn("== send error, will try again! may select other server to send");
                }

                flag = send(selector, request);

                if(num < 0){
                    throw new AppException("==== fail for calling the method:" + method.getName() + "====");
                }else num--;

            } while (!flag);   //select other server

            //async
            try {
                response = responses.take();  // Retrieves and removes the head of this queue
            } catch (InterruptedException e) {
                throw new AppException("==== fail for getting results of the method: " + method.getName() + " ====");
            }

        } finally {
            ServerInfo.msgTransferMap.remove(request.getRequestId());   // get response
        }

        // several types for response (ERROR OK)
        if(response.getResponseStatus() == ResponseStatus.ERROR){
            throw new AppException(response.getErrorMessage());
        }

        if(response.isArray())
            return response.returnObj(response.getResultType());
        return response.returnObj();

    }

    /**
     * false: select again, or send request
     *
     * @param selector
     * @param request
     * @return
     */
    private boolean send(ServerSelector selector, RequestImpl request) throws InterruptedException {
        String itfName = request.getItfName();
        Set<String> serverSet = ServerInfo.itfServersMap.get(itfName);

        if(serverSet != null && serverSet.size() > 0) {
            String selectedServerInfo = selector.select(serverSet, request);
            Channel channel = ServerInfo.serverChannelMap.get(selectedServerInfo);

            //could combine with RocketMQ ---> having retry   !!!!
            //here we diy using the method of self-polling
            try {
                if (channel == null) {
                    logger.warn("== no open channel for server {" + selectedServerInfo + "}! will close this server candidate");
                    throw new AppException();
                }

                int num = Constant.REQUEST_RETRY_TIMES;
                do {
                    Thread.sleep(10);
                    if (num < 0) {
                        logger.warn("=== The channel {" + channel + "} is not active! will close this server candidate ===");
                        throw new AppException();
                    } else if (num != Constant.REQUEST_RETRY_TIMES) {
                        logger.warn("== The channel {" + channel + "} is not active, will try again!");
                    }
                    num--;
                } while (!channel.isActive());

                logger.debug("== The channel {" + channel + "} is active now");

                num = Constant.REQUEST_RETRY_TIMES;
                ChannelFuture future;
                do {

                    future = channel.writeAndFlush(request).sync();     // send out

                    if (num < 0) {
                        logger.warn("== send request error! will close this server candidate");
                        throw new AppException();
                    } else if (num != Constant.REQUEST_RETRY_TIMES) {
                        logger.warn("== fail for the future of channel {" + channel + "}! will send again!");
                    }
                    num--;
                } while (!future.isSuccess());

            } catch (AppException e) {

                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
                ServerInfo.serverChannelMap.remove(selectedServerInfo);
                ServerInfo.itfServersMap.get(itfName).remove(selectedServerInfo);
                return false;
            }
            return true;
        }

        logger.warn("=== no provider for this service {"+itfName+"} ===");
        return false;
    }

}
