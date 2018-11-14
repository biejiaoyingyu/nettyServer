package com.cxf.nettyserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * 引导：
 *  1.绑定到服务器将在其上监听并接受传入连接请求的端口
 *  2.配置 Channel，以将有关的入站消息通知给 EchoServerHandler 实例
 */
public class EchoServer {

    private final int port;
    public EchoServer(int port) {
        this.port = port;
    }
    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.err.println("Usage: " + EchoServer.class.getSimpleName() + " <port>");
        }
        //设置端口值（如果端口参数的格式不正确，则抛出一个NumberFormatException）
        int port = Integer.parseInt(args[0]);
        //调用服务器的 start()方法
        new EchoServer(port).start();
    }

    public void start() throws Exception {

        final EchoServerHandler serverHandler = new EchoServerHandler();
        //创建EventLoopGroup
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            //创建ServerBootstrap
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class)//指定所使用的 NIO 传输 Channel
                    .localAddress(new InetSocketAddress(port))//使用指定的端口设置套接字地址
                    //当一个新的连接被接受时，一个新的子 Channel 将会被创建，而 ChannelInitializer
                    // 将会把一个你的EchoServerHandler 的实例添加到该 Channel 的 ChannelPipeline
                    // 中。正如我们之前所解释的，这个 ChannelHandler 将会收到有关入站消息的通知
                    .childHandler(new ChannelInitializer<SocketChannel>()//添加一个EchoServerHandler 到子Channel的ChannelPipeline
                    {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(serverHandler);//EchoServerHandler被标注为@Shareable，所以我们可以总是使用同样的实例
                        }
                    });

            ChannelFuture f = b.bind().sync();//异步地绑定服务器；调用 sync()方法阻塞 等待直到绑定完成
            f.channel().closeFuture().sync();//获取 Channel的CloseFuture，并且阻塞当前线程直到它完成
        } finally {
            group.shutdownGracefully().sync();//关闭 EventLoopGroup释放资源
        }
    }

}
