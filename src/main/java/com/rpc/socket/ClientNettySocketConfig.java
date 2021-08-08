package com.rpc.socket;

import com.rpc.consumer.ClientRpcConfig;
import com.rpc.consumer.ServerInfo;
import com.rpc.exception.AppException;
import com.rpc.management.RpcCriterion;
import com.rpc.socket.nettyhandler.ResponseHandler;
import com.rpc.utils.Constant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.log4j.Logger;

import java.util.Date;


public class ClientNettySocketConfig extends AbstractNettySocketConfig implements SocketConfig{

    private static final Logger logger = Logger.getLogger(ClientNettySocketConfig.class);
    private static final ThreadLocal<Integer> retryLocal = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return Constant.CHANNEL_RETRY_TIMES;
        }
    };

    private NioEventLoopGroup worker;
    private Bootstrap bootstrap;


    //  for consumer
    protected void doOtherInit() {
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
            String server = ip + Constant.IP_PORT_GAP + port;
            try {
                ChannelFuture future = bootstrap.connect(ip, port).addListener((ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        logger.debug("=== connection done  ===");
                    } else if (retryLocal.get() == 0) {
                        throw new InterruptedException("== number of retries has run out ==");
                    } else {
                        int order = Constant.CHANNEL_RETRY_TIMES - retryLocal.get() + 1;
                        System.err.println(new Date() + ": fail for connection, " + order + "-th to retry");
                        retryLocal.set(retryLocal.get() - 1);
                        connect(ip, port);
                    }
                }).sync();
                ServerInfo.serverChannelMap.put(server, future.channel());

            } catch (InterruptedException e) {
                logger.warn(e.getMessage());
                throw new AppException("== fail to connect with server {" + server +"} !");
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
     * to show the states of RPC requests and socket connection
     */
    @Override
    public void show() {
        String info = "========= RPC requests BEING HANDLING count {" + ServerInfo.msgTransferMap.keySet().size() + "} :\n" +
                ServerInfo.msgTransferMap.keySet() +
                "\n========= RPC requests HANDLED count :\n" +
                ClientRpcConfig.numRpcRequestDone +
                "\n=================================          =================================\n" +
                ServerInfo.getSocketStatus();
        logger.info(info);
    }

    @Override
    public void alter(RpcCriterion condition, Object input) {

    }

}
