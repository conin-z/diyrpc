package com.rpc.socket;

import com.rpc.socket.nettyhandler.KryoDecoder;
import com.rpc.socket.nettyhandler.KryoEncoder;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public abstract class AbstractNettySocketConfig implements SocketConfig{

    protected static LoggingHandler loggingHandler;
    protected LogLevel loggingLevel = LogLevel.DEBUG;


    protected long readerIdleTime = 5l;
    protected long writerIdleTime = 0l;
    protected long allIdleTime = 0l;
    protected TimeUnit idelStateUnit = TimeUnit.MINUTES;
    protected int maxFrameLength = Integer.MAX_VALUE;
    protected int lengthFieldOffset = 0;
    protected int lengthFieldLength = 4;
    protected int lengthAdjustment = 0;
    protected int initialBytesToStrip = 4;


    @Override
    public void init(){
        if (loggingHandler == null) {
            loggingHandler = new LoggingHandler(loggingLevel);
        }
    }


    /**
     * if want to use other serializer/deserializer, can override this method
     *
     * @param serviceHandler
     * @return
     */
    protected ChannelInitializer<SocketChannel> customizeChannelInitializer(ChannelHandler serviceHandler){
        return new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                //*** log ,extends ChannelDuplexHandler
                pipeline.addLast(loggingHandler);
                //*** monitoring timeout, extends ChannelDuplexHandler
                pipeline.addLast(new IdleStateHandler(readerIdleTime, writerIdleTime, allIdleTime, idelStateUnit));
                //*** Sticking/unpacking issues，use DelimiterBasedFrameDecoder
                pipeline.addLast(new LengthFieldBasedFrameDecoder(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip));
                pipeline.addLast(new LengthFieldPrepender(lengthFieldLength));
                //*** resolve message (object serializer) and service call
                //--------- scheme 1 ---------
                //                        pipeline.addLast(new StringDecoder());   //{ByteBuf} into {String}   //in
                //                        pipeline.addLast(new StringEncoder());  //{String} into {ByteBuf}  //out
                //                        pipeline.addLast(new ServerJsonDecoder()); //{Json String} into {RequestImpl} //in
                //                        pipeline.addLast(new ServerJsonEncoder());  //{RequestImpl} into a {Json String} //out
                //--------- scheme 2 ---------
                //                        pipeline.addLast(new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
                //                        pipeline.addLast(new ObjectEncoder());
                //--------- scheme 3 ---------
                pipeline.addLast(new KryoDecoder());  // ByteBuf --> RequestImpl
                pipeline.addLast(new KryoEncoder());  // ResponseImpl --> byte[] -- ByteBuf

                pipeline.addLast(serviceHandler);
            }
        };

    }


    @Override
    public boolean connect(final String ip, final int port){
        return true;
    }



    public static LoggingHandler getLoggingHandler() {
        return loggingHandler;
    }

    public static void setLoggingHandler(LoggingHandler loggingHandler) {
        AbstractNettySocketConfig.loggingHandler = loggingHandler;
    }

    public LogLevel getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(LogLevel loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public long getReaderIdleTime() {
        return readerIdleTime;
    }

    public void setReaderIdleTime(long readerIdleTime) {
        this.readerIdleTime = readerIdleTime;
    }

    public long getWriterIdleTime() {
        return writerIdleTime;
    }

    public void setWriterIdleTime(long writerIdleTime) {
        this.writerIdleTime = writerIdleTime;
    }

    public long getAllIdleTime() {
        return allIdleTime;
    }

    public void setAllIdleTime(long allIdleTime) {
        this.allIdleTime = allIdleTime;
    }

    public TimeUnit getIdelStateUnit() {
        return idelStateUnit;
    }

    public void setIdelStateUnit(TimeUnit idelStateUnit) {
        this.idelStateUnit = idelStateUnit;
    }

    public int getMaxFrameLength() {
        return maxFrameLength;
    }

    public void setMaxFrameLength(int maxFrameLength) {
        this.maxFrameLength = maxFrameLength;
    }

    public int getLengthFieldOffset() {
        return lengthFieldOffset;
    }

    public void setLengthFieldOffset(int lengthFieldOffset) {
        this.lengthFieldOffset = lengthFieldOffset;
    }

    public int getLengthFieldLength() {
        return lengthFieldLength;
    }

    public void setLengthFieldLength(int lengthFieldLength) {
        this.lengthFieldLength = lengthFieldLength;
    }

    public int getLengthAdjustment() {
        return lengthAdjustment;
    }

    public void setLengthAdjustment(int lengthAdjustment) {
        this.lengthAdjustment = lengthAdjustment;
    }

    public int getInitialBytesToStrip() {
        return initialBytesToStrip;
    }

    public void setInitialBytesToStrip(int initialBytesToStrip) {
        this.initialBytesToStrip = initialBytesToStrip;
    }


}
