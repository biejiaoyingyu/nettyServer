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
 *
 *  ---------------------------------------------------------
 *  一个 EventLoopGroup 包含一个或者多个 EventLoop；
 *  一个 EventLoop 在它的生命周期内只和一个 Thread 绑定；
 *  所有由 EventLoop 处理的 I/O 事件都将在它专有的 Thread 上被处理;
 *  一个 Channel 在它的生命周期内只注册于一个 EventLoop；
 *  一个 EventLoop 可能会被分配给一个或多个 Channel。
 *  ---------------------------------------------------------
 *  Netty 提供了ChannelFuture 接口，其 addListener()方法注册了一个
 *  ChannelFutureListener，以便在某个操作完成时（无论是否成功）得到通知
 *
 *  ---------------------------------------------------------
 *  Netty 的主要组件是 ChannelHandler，它充当了所有处理入站和出站数据的
 *  应用程序逻辑的容器.
 *  事实上，ChannelHandler 可专门用于几乎任何类型的动作，例如将数据从一种格
 *  式转换为另外一种格式，或者处理转换过程中所抛出的异常.
 *  ChannelInboundHandler 是一个你将会经常实现的子接口。这种类型的ChannelHandler
 *  接收入站事件和数据，这些数据随后将会被你的应用程序的业务逻辑所处理。当你要给连接的
 *  客户端发送响应时，也可以从 ChannelInboundHandler 冲刷数据。你的应用程序的业务
 *  逻辑通常驻留在一个或者多个 ChannelInboundHandler 中。
 *  ---------------------------------------------------------
 *
 *  ChannelPipeline 提供了 ChannelHandler 链的容器，并定义了用于在该链上传播入站
 *  和出站事件流的 API。当 Channel 被创建时，它会被自动地分配到它专属的 ChannelPipeline
 *  ChannelHandler 安装到 ChannelPipeline 中的过程如下所示:
 *  A.一个ChannelInitializer的实现被注册到了ServerBootstrap中
 *  B.ChannelInitializer.initChannel()方法被调用时，ChannelInitializer将在 ChannelPipeline 中安装一组自定义的 ChannelHandler；
 *  C.ChannelInitializer 将它自己从 ChannelPipeline 中移除。
 *
 *  ----------------------------------------------------
 *  ServerBootstrap 将绑定到一个口，因为服务器必须要监听连接，而Bootstrap则是由想要连接到远程节点的客户端应用程序所使用的
 *  引导一个客户端只需要一个 EventLoopGroup，但是一个erverBootstrap 则需要两个（也可以是同一个实例）????
 *
 *  ServerBootstrap第一组将只包含一个 ServerChannel，代表服务器自身的已绑定到某个本地端口的正在监听的套接字。(负责为传入连接请求创建
 *  Channel的EventLoop)而第二组将包含所有已创建的用来处理传入客户端连接（对于每个服务器已经接受的连接都有一个）的 Channel。
 */
public class EchoServer {

    private final int port;
    public EchoServer(int port) {
        this.port = port;
    }
    public static void main(String[] args) throws Exception {

//        if (args.length != 1) {
//            System.err.println("Usage: " + EchoServer.class.getSimpleName() + " <port>");
//        }
        //设置端口值（如果端口参数的格式不正确，则抛出一个NumberFormatException）
        int port = 1111;
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
