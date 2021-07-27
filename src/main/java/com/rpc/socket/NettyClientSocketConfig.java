package com.rpc.socket;

import com.rpc.consumer.ClientRpcConfig;
import com.rpc.consumer.ServerInfo;
import com.rpc.exception.AppException;
import com.rpc.socket.nettyhandler.ResponseHandler;
import com.rpc.utils.Constant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.log4j.Logger;


public class NettyClientSocketConfig extends AbstractNettySocketConfig implements SocketConfig{

    private static final Logger logger = Logger.getLogger(NettyClientSocketConfig.class);

    private NioEventLoopGroup worker;
    private Bootstrap bootstrap;


    /**
     * for consumer
     *
     */
    public void init() {
        super.init();
        super.readerIdleTime = 0l;
        super.allIdleTime = 1l;
        worker = new NioEventLoopGroup(Constant.NETTY_WORKER_GROUP);
        bootstrap = new Bootstrap();
        bootstrap.group(worker).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY,true).option(ChannelOption.SO_KEEPALIVE,true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS,1000*20)
                .handler(customizeChannelInitializer(new ResponseHandler()));

    }


    /**
     * connect and save into channel cache
     *
     * @param ip   : server's ip  (resolved from list)
     * @param port   : server's port  (resolved)
     */
    public boolean connect(final String ip, final int port) {
        try {
            String servInfo = ip + Constant.IP_PORT_GAP + port;
            try {
                ChannelFuture future = bootstrap.connect(ip, port).sync();  // may fail
                // for this client may several rpc to do; one client one ServerInfo.class
                ServerInfo.serverChannelMap.put(servInfo, future.channel());
            } catch (InterruptedException e) {
                logger.warn(e.getMessage());
                throw new AppException("== fail to connect with server {" + servInfo +"} !");
            }
            return true;
        } catch (AppException e) {
            logger.warn(e.getMessage());
            return false;
        }
    }


    public void close(){
        if(worker != null){
            worker.shutdownGracefully();
        }
    }



    /**
     * with RpcObserver
     * to show the status of RPC requests
     */
    @Override
    public void show() {
        String info = "========= RPC requests BEING HANDLING count {" + ServerInfo.msgTransferMap.keySet().size() + "} :\n" +
                ServerInfo.msgTransferMap.keySet() +
                "\n========= RPC requests HANDLED count :\n" +
                ClientRpcConfig.numRpcRequestDone +
                "\n=================================          =================================";
        logger.info(info);
    }

    @Override
    public void alter() {

    }

}
