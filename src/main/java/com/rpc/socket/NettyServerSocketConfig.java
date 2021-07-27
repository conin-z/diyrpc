package com.rpc.socket;

import com.rpc.provider.ServerRpcConfig;
import com.rpc.socket.nettyhandler.NettyRequestInfo;
import com.rpc.socket.nettyhandler.InvokeHandler;
import com.rpc.utils.Constant;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.log4j.Logger;


public class NettyServerSocketConfig extends AbstractNettySocketConfig implements SocketConfig{

    private static final Logger logger = Logger.getLogger(NettyServerSocketConfig.class);

    //could add MQConfig
    private NioEventLoopGroup worker;
    private ServerBootstrap serverBootstrap;
    private NioEventLoopGroup boss;
    private int port;

    public NettyServerSocketConfig(int port) {
        this.port = port;
    }

    /**
     * for server
     *
     */
    public void init() {
        super.init();
        worker = new NioEventLoopGroup(Constant.NETTY_WORKER_GROUP);
        boss = new NioEventLoopGroup(Constant.NETTY_BOSS_GROUP);
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(boss, worker).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG,512)
                .childOption(ChannelOption.TCP_NODELAY,true).childOption(ChannelOption.SO_KEEPALIVE,true)
                .childHandler(customizeChannelInitializer(new InvokeHandler()));

        try {
            serverBootstrap.bind(port).sync();
            logger.info("start socket server successful! port: " + port);
        } catch (InterruptedException e) {
            logger.error("start socket server error! port: " + port, e);
        }

    }



    public void close(){
        if(worker != null){
            worker.shutdownGracefully();
        }
        if(boss != null){
            boss.shutdownGracefully();
        }
    }



    /**
     * with RpcObserver
     * to show the status of RPC requests BEING HANDLING
     */
    @Override
    public void show() {
        long timeGap = (System.currentTimeMillis() - ServerRpcConfig.onlineMoment);
        int numOnHandling = NettyRequestInfo.getRequestNumOnHandling();
        String listOnHandling = NettyRequestInfo.getClientListOnHandling();
        int numHandled = NettyRequestInfo.getRequestNumHandled();
        int numFailed = NettyRequestInfo.getRequestNumFailed();
        String qps = "average QPS :\n" + timeGap / (numOnHandling + numHandled + numFailed);
        String info = qps +
                "\n========= requests BEING HANDLING count : {" + numOnHandling + "}\n" +
                "corresponding clients are :\n" + listOnHandling + "\n" +
                "========= requests HANDLED-SUCCESS count : {" + numHandled + "}\n" +
                "========= requests HANDLED-FAIL count : {" + numFailed + "}\n" +
                "=================================          =================================";
        logger.info(info);
    }

    @Override
    public void alter() {

    }

}
