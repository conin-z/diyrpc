package com.rpc.nettyhandler;

import com.rpc.message.RequestImpl;
import com.rpc.utils.JsonUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.log4j.Logger;

/**
 * be used in conjunction with io.netty.handler.codec.string.StringEncoder
 *
 * @user KyZhang
 * @date
 */
@Deprecated
@ChannelHandler.Sharable
public class ClientJsonEncoder extends ChannelOutboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger(ClientJsonEncoder.class);
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        RequestImpl request = (RequestImpl) msg;
        String json = JsonUtil.pojoToJson(request);
        logger.debug("send package to server: " + request + " --> " + json);
        ctx.write(json, promise);
    }

}
