package com.rpc.socket.nettyhandler;

import com.rpc.provider.ServerRpcConfig;
import com.rpc.provider.registry.ServiceRegistry;
import com.rpc.utils.Constant;
import com.rpc.message.*;
import com.rpc.utils.JsonUtil;
import com.rpc.utils.StringUtil;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;


/**
 * @user KyZhang
 * @date
 */
@ChannelHandler.Sharable
public class InvokeHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = Logger.getLogger(InvokeHandler.class);


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // here we can do some work on channel monitoring

    }


    /**
     * message process and wrap result-related information into a instance of {@link ResponseImpl} to send
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RequestImpl request = (RequestImpl)msg;
        String requestId = request.getRequestId();
        String clientIp = ctx.channel().remoteAddress().toString();

        if(request.getMessageType() == MessageType.HEARTBEAT){
            logger.info("===== received heartbeat successful ======");   //ping
            ResponseImpl response = new ResponseImpl(requestId, request.getMessageType(), ResponseStatus.OK);
            ctx.channel().writeAndFlush(response);  //pong  "i'm alive"
        }else if(request.getMessageType() == MessageType.SERVER){
            NettyRequestInfo.requestNumOnHandling.incrementAndGet();
            NettyRequestInfo.requestMapOnHandling.put(requestId, clientIp);
            process(ctx, request);
        }

    }


    /**
     * invoke regarding message of MessageType.SERVER
     *
     * @param ctx
     * @param request
     */
    private void process(ChannelHandlerContext ctx, RequestImpl request) {
        try {
            Class<?> clz = Class.forName(request.getItfName());
            Object bean = ServerRpcConfig.applicationContext.getBean(clz);
            Method method = clz.getMethod(request.getMethodName(), request.getParaClassTypes());
            Object result = method.invoke(bean, request.getArgs());
            Class<?> resultClass = result.getClass();

            // new response
            ResponseImpl response = new ResponseImpl(request.getRequestId(), MessageType.SERVER, ResponseStatus.OK);
            logger.debug("======== new response created =======");
            response.setRequestClz(clz);
            response.setServerName(Constant.LOCAL_ADDRESS);

            if (result == null || result instanceof Void) {
                response.setResultType(Void.class);
            }else{
                response.setArray(resultClass.isArray());
                response.setCollection(Collection.class.isAssignableFrom(resultClass));
                response.setContent(JsonUtil.pojoToJson(result));
                if(response.isArray()){
                    response.setArrayType(resultClass);
                    response.setResultType(resultClass.getComponentType());
                }else if(response.isCollection()){
                    Collection collection = (Collection)result;
                    response.setCollectionType(resultClass);
                    for (Object obj : collection){
                        response.setResultType(obj.getClass());
                        break;
                    }
                }else {
                    response.setResultType(resultClass);
                }
            }
            logger.debug(" ======= new response_result send now =======");

            ctx.channel().writeAndFlush(response).addListener((ChannelFutureListener) future -> {
                String client = future.channel().remoteAddress().toString();
                String requestId = response.getRequestId();
                if(future.isSuccess()){
                    NettyRequestInfo.requestMapHandled.put(requestId, client);
                    logger.debug(" ======= send response successful! =======");
                }else {
                    if(future.isCancelled()){
                        logger.warn("=== request-handling is cancelled ===");
                    }else {
                        NettyRequestInfo.requestMapFailed.put(requestId, client);
                        logger.warn("=== fail for sending response!");
                        // say bye to notify consumers to update cache
                        if (future.channel().isOpen()) {
                            ResponseImpl resp = new ResponseImpl(requestId, MessageType.DISCONNECT, ResponseStatus.ERROR);   //say bye
                            resp.setContent(ctx.channel().localAddress() + " will disconnect with you");
                            resp.setServerName(Constant.LOCAL_ADDRESS);
                            future.channel().writeAndFlush(resp);
                            future.channel().close();   // like ChannelFutureListener.CLOSE_ON_FAILURE
                        }
                    }
                    // publish msg
                    ServiceRegistry registry = ServerRpcConfig.applicationContext.getBean(ServerRpcConfig.class).getServiceRegistry();
                    registry.deleteKey(Constant.LOCAL_ADDRESS);
                }
                NettyRequestInfo.requestMapOnHandling.remove(requestId);
                NettyRequestInfo.requestNumOnHandling.decrementAndGet();
            });
        } catch (ClassNotFoundException e) {
            logger.error("== service {" + request.getItfName() + "} not found error!", e);
            ResponseImpl response = new ResponseImpl(request.getRequestId(), MessageType.WARN, ResponseStatus.ERROR);
            response.setErrorMessage("service {" + request.getItfName() + "} not found error!");
            response.setServerName(Constant.LOCAL_ADDRESS);
            ctx.channel().writeAndFlush(response);
        } catch (NoSuchMethodException e) {
            logger.error("== method {" + request.getMethodName() + "} not found error!", e);
            ResponseImpl response = new ResponseImpl(request.getRequestId(), MessageType.WARN, ResponseStatus.ERROR);
            response.setErrorMessage("== method {" + request.getMethodName() + "} not found error!");
            response.setServerName(Constant.LOCAL_ADDRESS);
            ctx.channel().writeAndFlush(response);
        } catch (IllegalAccessException e) {
            logger.error("== IllegalAccess!", e);
            ResponseImpl response = new ResponseImpl(request.getRequestId(), MessageType.WARN, ResponseStatus.ERROR);
            response.setErrorMessage("== IllegalAccess!");
            response.setServerName(Constant.LOCAL_ADDRESS);
            ctx.channel().writeAndFlush(response);
        } catch (InvocationTargetException e) {
            logger.error("== method {" + request.getMethodName() + "} invocation error!", e);
            ResponseImpl response = new ResponseImpl(request.getRequestId(), MessageType.WARN, ResponseStatus.ERROR);
            response.setErrorMessage("== method {" + request.getMethodName() + "} invocation error!");
            response.setServerName(Constant.LOCAL_ADDRESS);
            ctx.channel().writeAndFlush(response);
        }
    }



    /**
     * no goodbye
     *
     * @param ctx
     * @param t
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable t){
        if(t instanceof RejectedExecutionException){
            logger.error("==== server busy error! channel: " + ctx.channel());
            ResponseImpl response = new ResponseImpl("null", MessageType.WARN, ResponseStatus.ERROR);
            response.setErrorMessage("==== system busy, please try again later!");
            ctx.channel().writeAndFlush(response);
        }
        ctx.fireExceptionCaught(t);
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent) evt;
            IdleState idleState = (event).state();
            String eventType = StringUtil.getIdleEventInfo(event);
            logger.debug(ctx.channel().remoteAddress() + "idle event: " +eventType);

            if(idleState == IdleState.READER_IDLE){
                Attribute<Object> attr = ctx.channel().attr(AttributeKey.valueOf(Constant.SOCKET_HEARTBEAT_TIME));
                if(attr.get() == null){
                    attr.set(new Integer(0));
                }
                Integer times = (Integer)attr.get();
                if(times++ <= Constant.IDLE_TIMES){
                    // keep heart "I'm alive"
                    ResponseImpl response = new ResponseImpl("null", MessageType.HEARTBEAT, ResponseStatus.OK);
                    ctx.channel().writeAndFlush(response);
                    attr.set(times);
                }else {
                    logger.debug(ctx.channel().remoteAddress() + " with idle event: " + eventType);
                    if (ctx.channel().isOpen()) {
                        // say bye to notify consumers, may send abortively
                        ResponseImpl response = new ResponseImpl("null", MessageType.DISCONNECT, ResponseStatus.OK);
                        response.setContent(ctx.channel().localAddress() + " will disconnect with you");
                        response.setServerName(Constant.LOCAL_ADDRESS);
                        ctx.channel().writeAndFlush (response);
                        // disconnect
                        ctx.channel().close();
                    }
                }
            }

            super.userEventTriggered(ctx, evt);

        }
    }




}
