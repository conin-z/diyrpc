package com.rpc.consumer.proxy;

import com.rpc.consumer.ClientRpcConfig;
import com.rpc.consumer.ServerInfo;
import com.rpc.selector.RandomServerSelector;
import com.rpc.selector.ServerSelector;
import com.rpc.utils.Constant;
import com.rpc.exception.AppException;
import com.rpc.message.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
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

    public Object invoke(Object proxy, Method method, Object[] args) throws InterruptedException, AppException {
        ClientRpcConfig consumer = ClientRpcConfig.applicationContext.getBean(ClientRpcConfig.class);
        ServerSelector selector = consumer.getServerSelector();
        if (selector == null) {
            selector = new RandomServerSelector();  // here default
            consumer.setServerSelector(selector);
        }
        // new request
        RequestImpl request = new RequestImpl(MessageType.SERVER);
        //request.setRequestId(String.valueOf(System.currentTimeMillis()));
        request.setRequestId(UUID.randomUUID().toString());  //
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

                flag = send(selector, request);

                if(!flag){
                    logger.warn("=== send error, will select server candidate again to send");
                    num--;
                }
                if(num < 0){
                    throw new AppException("==== fail to call the method: " + method.getName() + "() ====");
                }
            } while (!flag);   // select again

            // get result
            try {
                response = responses.take();  // retrieves and removes the head of this queue
            } catch (InterruptedException e) {
                logger.error("==== fail to get results of the method: " + method.getName() + "() ====");
                return null;
            }

        } finally {
            ServerInfo.msgTransferMap.remove(request.getRequestId());   // get response
            ClientRpcConfig.numRpcRequestDone.incrementAndGet();
        }

        // several types for response (ERROR, OK)
        if(response.getResponseStatus() == ResponseStatus.ERROR){
            logger.error(response.getErrorMessage());
            return null;
        }

        if(response.isArray())
            return response.returnObj(response.getResultType());
        // transfer into correct matching format of result
        return response.returnObj();

    }

    /**
     *
     * @param selector
     * @param request
     * @return
     *     false: no provider for this service or the provider selected is invalid
     */
    private boolean send(ServerSelector selector, RequestImpl request) throws InterruptedException {
        String itfName = request.getItfName();
        Set<String> serverSet = ServerInfo.itfServersMap.get(itfName);
        if(serverSet != null && serverSet.size() > 0) {
            String selectedServerInfo = selector.select(serverSet, request);
            Channel channel = ServerInfo.serverChannelMap.get(selectedServerInfo);

            //could combine with RocketMQ ---> having retry ?
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

                logger.debug("== socket channel {" + channel + "} is active now");

                num = Constant.REQUEST_RETRY_TIMES;
                ChannelFuture future;
                do {
                    future = channel.writeAndFlush(request).sync();     // send out
                    if (num < 0) {
                        logger.error("== send request error! will close this server candidate");
                        throw new AppException();
                    } else if (num != Constant.REQUEST_RETRY_TIMES) {
                        logger.warn("== fail for the future of channel {" + channel + "}! will send again!");
                    }
                    num--;
                } while (!future.isSuccess());

            } catch (AppException e) {
                ServerInfo.removeServer(selectedServerInfo);
                return false;
            }
            return true;
        }
        logger.warn("=== no provider for this service {" + itfName + "} now! ===");
        return false;
    }

}
