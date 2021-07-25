package com.rpc.socket.nettyhandler;

import com.rpc.utils.KryoUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


/**
 * @user KyZhang
 * @date
 */
public class KryoEncoder extends MessageToByteEncoder {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) {
        byte[] bytes = KryoUtil.writeToByteArray(msg);
        out.writeBytes(bytes);
    }

}
