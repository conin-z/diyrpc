package com.rpc.socket;

import com.rpc.socket.nettyhandler.InvokeHandler;
import com.rpc.socket.nettyhandler.KryoDecoder;
import com.rpc.socket.nettyhandler.KryoEncoder;
import com.rpc.socket.nettyhandler.ResponseHandler;
import com.rpc.utils.Constant;
import com.rpc.consumer.ServerInfo;
import com.rpc.exception.AppException;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * @user KyZhang
 * @date
 */
public class NettySocketConfig implements SocketConfig{

    private static final Logger logger = Logger.getLogger(NettySocketConfig.class);

    //could add MQConfig
    private Bootstrap bootstrap;
    private ServerBootstrap serverBootstrap;
    private NioEventLoopGroup boss;
    private NioEventLoopGroup worker;


    /**
     * for server
     *
     * @param port  : set
     */
    public void serverInit(final int port) {
        if(this.boss == null){
            this.boss = new NioEventLoopGroup(Constant.NETTY_BOSS_GROUP);
        }
        if (this.worker == null) {
            this.worker = new NioEventLoopGroup(Constant.NETTY_WORKER_GROUP);
        }

        if (serverBootstrap == null) {
            serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(boss, worker).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG,512)
                    .childOption(ChannelOption.TCP_NODELAY,true).childOption(ChannelOption.SO_KEEPALIVE,true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            //*** log ,extends ChannelDuplexHandler
                            pipeline.addLast(new LoggingHandler());
                            //*** monitoring timeout, extends ChannelDuplexHandler
                            pipeline.addLast(new IdleStateHandler(5l,0,0, TimeUnit.MINUTES));
                            //*** Sticking/unpacking issues，use DelimiterBasedFrameDecoder
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));
                            pipeline.addLast(new LengthFieldPrepender(4));
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

                            pipeline.addLast(new InvokeHandler());


                        }
                    });
        }

        try {
            serverBootstrap.bind(port).sync();
            logger.debug("start socket server successful! port: " + port);
        } catch (InterruptedException e) {
            logger.error("start socket server error! port: " + port, e);
        }

    }


    /**
     * for consumer
     *
     * @param ip   : server's ip  (resolved from list)
     * @param port   : server's port  (resolved)
     */
    public void clientInit(String ip,int port) throws AppException {
        if (this.worker == null) {
            this.worker = new NioEventLoopGroup(Constant.NETTY_WORKER_GROUP);
        }
        if (bootstrap == null) {
            bootstrap = new Bootstrap();
            bootstrap.group(this.worker).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY,true).option(ChannelOption.SO_KEEPALIVE,true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS,1000*20)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            //*** log ,extends ChannelDuplexHandler
                            pipeline.addLast(new LoggingHandler());
                            //*** monitoring timeout, extends ChannelDuplexHandler
                            pipeline.addLast(new IdleStateHandler(0,0,1l, TimeUnit.MINUTES));
                            //*** Sticking/unpacking issues，use DelimiterBasedFrameDecoder
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));
                            pipeline.addLast(new LengthFieldPrepender(4));
                            //*** resolve message (object serializer) and response process
                //--------- scheme 1 ---------
    //                        pipeline.addLast(new StringDecoder());   //{ByteBuf} into {String}   //in
    //                        pipeline.addLast(new StringEncoder());  //{String} into {ByteBuf}  //out
    //                        pipeline.addLast(new ServerJsonDecoder()); //{Json String} into {RequestImpl} //in
    //                        pipeline.addLast(new ServerJsonEncoder());  //{RequestImpl} into a {Json String} //out
                //--------- scheme 2 ---------
    //                        pipeline.addLast(new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
    //                        pipeline.addLast(new ObjectEncoder());
                //--------- scheme 3 ---------
                            pipeline.addLast(new KryoDecoder());
                            pipeline.addLast(new KryoEncoder());

                            pipeline.addLast(new ResponseHandler());
                        }
                    });
        }

        String servInfo = ip + Constant.IP_PORT_GAP + port;
        try {
            ChannelFuture future = bootstrap.connect(ip, port).sync();  // may fail
            // for this client may several rpc to do; one client one ServerInfo.class
            ServerInfo.serverChannelMap.put(servInfo, future.channel());
        } catch (InterruptedException e) {
            logger.warn(e.getMessage());
            throw new AppException("== fail to connect with server {" + servInfo +"} !");
        }

    }

    /**
     * connect and save into channel cache
     *
     * @param ip
     * @param port
     */
    public boolean connect(String ip, String port){

        try {
            clientInit(ip, Integer.parseInt(port));
            return true;
        } catch (AppException e) {
            logger.warn(e.getMessage());
            return false;
        }

    }


    public void close(){
        if(boss != null){
            boss.shutdownGracefully();
        }
        if(worker != null){
            worker.shutdownGracefully();
        }
    }


}
