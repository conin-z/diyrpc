package com.rpc.socket.nettyhandler;

import com.rpc.exception.AppException;
import com.rpc.utils.KryoUtil;
import io.netty.buffer.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @user KyZhang
 * @date
 */
public class KryoDecoder extends ByteToMessageDecoder {

    /**
     * @see KryoUtil#readFromByteBuf(ByteBuf)
     * @param ctx
     * @param in
     * @param out
     * @throws AppException
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws AppException {
        if (in == null) {
            out.add(null);
        }
        if (in.readableBytes() > 0) {
            Object o = KryoUtil.readFromByteBuf(in);
            out.add(o);
        }else{
            out.add(null);
            throw new AppException("no byte read!");
        }
    }
}
