package com.rpc.nettyhandler;


import com.rpc.constant.Constant;
import com.rpc.message.MessageType;
import com.rpc.message.RequestImpl;
import com.rpc.message.ResponseImpl;
import com.rpc.message.ServerInfo;
import com.rpc.utils.StringUtil;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.apache.log4j.Logger;


import java.util.Map;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;

/**
 * @user KyZhang
 * @date
 */
@ChannelHandler.Sharable
public class ResponseHandler extends SimpleChannelInboundHandler<ResponseImpl> {

    private static final Logger logger = Logger.getLogger(ResponseHandler.class);

    protected void channelRead0(ChannelHandlerContext ctx, ResponseImpl msg) throws Exception {
        if (msg.getMessageType() == MessageType.DISCONNECT){  //received
            logger.debug(msg.getContent());
            /* to update cache or retry (reconnect) immediately
            here we update cache, could reconnect when refreshing the 1-level cache by scheduled task */
            ServerInfo.serversList.remove(msg.getServerName());  //
            ServerInfo.serverChannelMap.remove(msg.getServerName());
            Set<String> keySet = ServerInfo.itfServersMap.keySet();
            for (String itf : keySet) {
                ServerInfo.itfServersMap.get(itf).remove(msg.getServerName());
            }

        }else if(msg.getMessageType() == MessageType.SERVER){
            logger.debug("====== received response" + msg + " from server regarding request " + msg.getRequestId());
            /*
             * upload the msg to the MQ server  -->design APIs:
             *      1.MQConfig / xx.xml
             *      2.MQTask(event):  public abstract class AbstractKafkaPushTask { sendMessage(msg);
             *      3.MQConsumer(listener):  class NettyKafkaConsumerListener implements MessageListener<String, String>  //org.springframework.kafka.listener.MessageListener
             *
             */
            SynchronousQueue<ResponseImpl> responses = ServerInfo.msgTransferMap.get(msg.getRequestId());
            responses.put(msg);
        }

    }

    /**
     * -------------------here:
     * an IDleStateEvent event is published when the idle time (read or write) of connection is too long;
     * then, override the userEventTriggered() method in customized ChannelInboundHandler to handle this event.
     * --->
     *
     * @param ctx
     * @param evt
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if(evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent) evt;
            IdleState idleState = event.state();
            String eventType = StringUtil.getIdleEventInfo(event);
            logger.debug(ctx.channel().remoteAddress() + "idle event: " + eventType);

            if(idleState == IdleState.ALL_IDLE){
                Attribute<Object> attr = ctx.channel().attr(AttributeKey.valueOf(Constant.NETTY_HEARTBEAT_TIME));
                if(attr.get() == null){
                    attr.set(new Integer(0));
                }
                Integer times = (Integer)attr.get();
                if(times++ <= 3){
                    RequestImpl request = new RequestImpl(MessageType.HEARTBEAT);   //keep heart
                    ctx.channel().writeAndFlush(request);
                    attr.set(times);
                }else {
                    if (ServerInfo.serverChannelMap.containsValue(ctx.channel())) {
                        for (Map.Entry<String,Channel> entry : (ServerInfo.serverChannelMap).entrySet()) {
                            if (entry.getValue() == ctx.channel()){
                                ServerInfo.serverChannelMap.remove(entry.getKey());
                            }
                        }
                    }
                    logger.warn("connection with server {" + ctx.channel().remoteAddress() + "} will be closed");
                    if (ctx.channel().isOpen()) {
                        ctx.channel().close();  //say goodbye
                    }
                }
            }
            super.userEventTriggered(ctx, evt);
        }
    }

}
