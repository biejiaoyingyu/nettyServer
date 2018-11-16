package com.cxf.nettyserverdtails;

import com.cxf.nettyserver.EchoServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class EchoServer {
    private Channel channel;

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
                    .childHandler(new ChannelInitializer<SocketChannel>()//添加一个EchoServerHandler 到子Channel的ChannelPipeline
                    {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            /**
                             * 拿到channel引用，但是每个链接进来都会创建一个channel的，这一点需要明确
                             */
                            channel = ch;
                            ch.pipeline().addLast(serverHandler);//EchoServerHandler被标注为@Shareable，所以我们可以总是使用同样的实例
                        }
                    });

            ChannelFuture f = b.bind().sync();//异步地绑定服务器；调用 sync()方法阻塞 等待直到绑定完成
            f.channel().closeFuture().sync();//获取 Channel的CloseFuture，并且阻塞当前线程直到它完成
        } finally {
            group.shutdownGracefully().sync();//关闭 EventLoopGroup释放资源
        }
    }

    /**
     * 拿到channel引用，操作写数据并将其冲刷到远程节点这样的常规任务
     */
    public void writeAndFlush() {
        //创建持有要写数据的 ByteBuf
        ByteBuf buf = Unpooled.copiedBuffer("your data", CharsetUtil.UTF_8);
        //写数据并冲刷它
        ChannelFuture cf = channel.writeAndFlush(buf);
        //添加 ChannelFutureListener 以便在写操作完成后接收通知
        cf.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete (ChannelFuture future){
                //写操作完成，并且没有错误发生
                if (future.isSuccess()) {
                    System.out.println("Write successful");
                } else {
                    //记录错误
                    System.err.println("Write error");
                    future.cause().printStackTrace();
                }
            }

        });

    }

    /**
     * channel是线程安全的Netty 的 Channel 实现是线程安全的，因此你可以存储一个到 Channel 的引用，
     * 并且每当你需要向远程节点写数据时，都可以使用它，即使当时许多线程都在使用它。
     */
    public void threadSafeChannel(){
        final Channel channel0 =  channel;
        //创建持有要写数据的ByteBuf
        final ByteBuf buf = Unpooled.copiedBuffer("your data", CharsetUtil.UTF_8).retain();
        Runnable writer = new Runnable() {
            @Override
            public void run() {
                //创建将数据写到Channel的Runnable
                channel0.writeAndFlush(buf.duplicate());
            }
        };
        //获取到线程池Executor 的引用
        Executor executor = Executors.newCachedThreadPool();
        // write in one thread
        executor.execute(writer);
        // write in another thread
        executor.execute(writer);

    }
}
