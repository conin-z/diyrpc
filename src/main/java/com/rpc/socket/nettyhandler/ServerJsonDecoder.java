package com.rpc.socket.nettyhandler;

import com.rpc.message.RequestImpl;
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
public class ServerJsonDecoder extends ChannelInboundHandlerAdapter {

    private static final Logger logger = Logger.getLogger(ServerJsonDecoder.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String json = (String) msg;
        RequestImpl request = JsonUtil.jsonToPojo(json, RequestImpl.class);
        logger.debug("received a json package from client: " + json + " --> "+request);
        ctx.fireChannelRead(request);
    }
}
