package com.rpc.nettyhandler;

import com.rpc.config.RpcServerConfiguration;
import com.rpc.constant.Constant;
import com.rpc.message.*;
import com.rpc.utils.JsonUtil;
import com.rpc.utils.StringUtil;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

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

    /**
     * message process and wrap into a instance of ResponseImpl.class for writing
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            RequestImpl request = (RequestImpl)msg;

            if(request.getMessageType() == MessageType.HEARTBEAT){
                logger.debug("===== received heartbeat successful ======");   //ping
                ResponseImpl response = new ResponseImpl(request.getRequestId(), ResponseStatus.OK);
                response.setMessageType(request.getMessageType());
                ctx.channel().writeAndFlush(response);  //pong  "i'm alive"
            }else if(request.getMessageType() == MessageType.SERVER){
                try {
                    process(ctx,request, RpcServerConfiguration.ioc);  //executor
                } catch (RejectedExecutionException re) {
                    logger.error("==== server busy error! channel: " + ctx.channel() + "\nRequestMassage: "+request, re);
                    ResponseImpl response = new ResponseImpl(request.getRequestId(), ResponseStatus.ERROR);
                    response.setErrorMessage("==== system busy, please try again later!");
                    ctx.channel().writeAndFlush(response);
                }
            }
    }

    /**
     * invoke regarding message of MessageType.SERVER
     *
     * @param ctx
     * @param request
     * @param ioc
     */
    private void process(ChannelHandlerContext ctx, RequestImpl request, ApplicationContext ioc) {
        try {
            Class<?> clz = Class.forName(request.getItfName());
            Object bean = ioc.getBean(clz);
            logger.debug("");
            Method method = clz.getMethod(request.getMethodName(), request.getParaClassTypes());

            Object result = method.invoke(bean, request.getArgs());
            Class<?> resultClass = result.getClass();
            // new response
            ResponseImpl response = new ResponseImpl(request.getRequestId(), ResponseStatus.OK);
            logger.debug("======== new response created =======");
            response.setRequestClz(clz);
            response.setMessageType(request.getMessageType());
            response.setServerName(Constant.LOCAL_ADDRESS);

            if (result == null || result instanceof Void) {
                response.setResultType(Void.class);
            }else{
                response.setArray(resultClass.isArray());
                response.setCollection(Collection.class.isAssignableFrom(resultClass));

                response.setContent( JsonUtil.pojoToJson(result));

                if(response.isArray()){
                    response.setArrayType(resultClass);
                    response.setResultType(resultClass.getComponentType());
                }else if(response.isCollection()){
                    Collection coll = (Collection)result;
                    response.setCollectionType(resultClass);
                    for (Object o : coll){
                        response.setResultType(o.getClass());
                        break;
                    }
                }else {
                    response.setResultType(resultClass);
                }
            }
            logger.debug(" ======= new response_result send now =======");
            ctx.channel().writeAndFlush(response).addListener((ChannelFutureListener) future -> {
                if(future.isSuccess()){
                    logger.debug(" ======= send response successful! =======");
                }else {
                    ResponseImpl response1 = new ResponseImpl(MessageType.DISCONNECT);   //say bye
                    response1.setContent(ctx.channel().localAddress() + " will disconnect with you");
                    response1.setServerName(Constant.LOCAL_ADDRESS);
                    future.channel().writeAndFlush(response1);
                    future.channel().close();   // like ChannelFutureListener.CLOSE_ON_FAILURE
                    logger.warn("=== fail for sending response!");
                }
            });
        } catch (ClassNotFoundException e) {
            logger.error("== service {" + request.getItfName() + "} not found error!", e);
            ResponseImpl response = new ResponseImpl(request.getRequestId(), ResponseStatus.ERROR);
            response.setErrorMessage("service {" + request.getItfName() + "} not found error!");
            response.setMessageType(MessageType.SERVER);
            response.setServerName(Constant.LOCAL_ADDRESS);
            ctx.channel().writeAndFlush(response);
        } catch (NoSuchMethodException e) {
            logger.error("== method {" + request.getMethodName() + "} not found error!", e);
            ResponseImpl response = new ResponseImpl(request.getRequestId(), ResponseStatus.ERROR);
            response.setErrorMessage("== method {" + request.getMethodName() + "} not found error!");
            response.setMessageType(MessageType.SERVER);
            response.setServerName(Constant.LOCAL_ADDRESS);
            ctx.channel().writeAndFlush(response);
        } catch (IllegalAccessException e) {
            logger.error("== IllegalAccess!", e);
            ResponseImpl response = new ResponseImpl(request.getRequestId(), ResponseStatus.ERROR);
            response.setErrorMessage("== IllegalAccess!");
            response.setMessageType(MessageType.SERVER);
            response.setServerName(Constant.LOCAL_ADDRESS);
            ctx.channel().writeAndFlush(response);
        } catch (InvocationTargetException e) {
            logger.error("== method {" + request.getMethodName() + "} invocation error!", e);
            ResponseImpl response = new ResponseImpl(request.getRequestId(), ResponseStatus.ERROR);
            response.setErrorMessage("== method {" + request.getMethodName() + "} invocation error!");
            response.setMessageType(MessageType.SERVER);
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
        logger.debug("==== will disconnect the channel with" + ctx.channel().remoteAddress() + "====");
        ctx.fireExceptionCaught(t);
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx,Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent) evt;
            IdleState idleState = (event).state();
            String eventType = StringUtil.getIdleEventInfo(event);
            logger.debug(ctx.channel().remoteAddress() + "idle event: " +eventType);

            if(idleState == IdleState.READER_IDLE){
                Attribute<Object> attr = ctx.channel().attr(AttributeKey.valueOf(Constant.NETTY_HEARTBEAT_TIME));
                if(attr.get() == null){
                    attr.set(new Integer(0));
                }
                Integer times = (Integer)attr.get();
                if(times++ <= Constant.IDLE_TIMES){
                    ResponseImpl response = new ResponseImpl(MessageType.HEARTBEAT);  //keep heart "i'm alive"
                    ctx.channel().writeAndFlush(response);
                    attr.set(times);
                }else {
                    logger.debug(ctx.channel().remoteAddress() + " with idle event: " + eventType);
                    ResponseImpl response = new ResponseImpl(MessageType.DISCONNECT); //say bye to warn consumer to update channel ache
                    response.setContent(ctx.channel().localAddress() + " will disconnect with you");
                    response.setServerName(Constant.LOCAL_ADDRESS);
                    ctx.channel().writeAndFlush (response);
                    ctx.channel().close();
                }
            }

            super.userEventTriggered(ctx, evt);

        }
    }




}
