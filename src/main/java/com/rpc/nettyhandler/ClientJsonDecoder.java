package com.rpc.nettyhandler;

import com.rpc.message.ResponseImpl;
import com.rpc.utils.JsonUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;

/**
 * be used in conjunction with io.netty.handler.codec.string.StringDecoder
 *
 * @user KyZhang
 * @date
 */
@Deprecated
@ChannelHandler.Sharable
public class ClientJsonDecoder extends ChannelInboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger(ClientJsonDecoder.class);
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String json = (String) msg;
        ResponseImpl response = JsonUtil.jsonToPojo(json, ResponseImpl.class);
        logger.debug("received a json package from server: " + json + " --> "+response);
        ctx.fireChannelRead(response);
    }
}
