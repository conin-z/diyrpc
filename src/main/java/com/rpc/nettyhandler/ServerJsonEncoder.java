package com.rpc.nettyhandler;

import com.rpc.message.RequestImpl;
import com.rpc.message.ResponseImpl;
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
public class ServerJsonEncoder extends ChannelOutboundHandlerAdapter {

    private static final Logger logger = Logger.getLogger(ServerJsonEncoder.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ResponseImpl response = (ResponseImpl) msg;
        String json = JsonUtil.pojoToJson(response);
        logger.debug("send package to client: " + response + " --> " + json);
        ctx.write(json, promise);
    }

}
