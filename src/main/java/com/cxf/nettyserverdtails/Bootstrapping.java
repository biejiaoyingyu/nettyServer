package com.cxf.nettyserverdtails;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;

import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

public class Bootstrapping {

    /**
     * 也就是说，服务器致力于使用一个父 Channel 来接受来自客户端的连接，并创建子Channel
     * 以用于它们之间的通信；而客户端将最可能只需要一个单独的、没有父Channel 的 Channel
     * 来用于所有的网络交互。
     * <p>
     * 这种定义能链式调用？
     * public abstract class AbstractBootstrap <B extends AbstractBootstrap<B,C>,C extends Channel>
     * public class Bootstrap extends AbstractBootstrap<Bootstrap,Channel>
     * public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap,ServerChannel>
     * <p>
     * Bootstrap channel(Class<? extends C>)
     * Bootstrap channelFactory(ChannelFactory<? extends C>)
     * channel()方法指定了Channel的实现类。如果该实现类没提供默认的构造函数，可以通过调用channelFactory()
     * 方法来指定一个工厂类，它将会被bind()方法调用;这里指默认的无参构造函数，因为内部使用了反射来实现Channel
     * 的创建。
     * =============================================================================================
     * ChannelFuture connect() 连接到远程节点并返回一个 ChannelFuture，其将 会在连接操作完成后接收到通知
     * ChannelFuture bind() 绑定 Channel 并返回一个 ChannelFuture，其将会在绑定操作完成后接收到通知，在
     * 那之后必须调用 Channel.connect()方法来建立连接
     * <p>
     * =============================================================================================
     * <T> Bootstrap option(ChannelOption<T> option,T value)设置 ChannelOption，其将被应用到每个新创建
     * 的Channel 的 ChannelConfig。这些选项将会通过bind()或者 connect()方法设置到 Channel，不管哪个先被调
     * 用。这个方法在 Channel 已经被创建后再调用将不会有任何的效果。支持的 ChannelOption取决于使用的 Channel
     * 类型。
     * <p>
     * =============================================================================================
     * <p>
     * <T> Bootstrap attr(Attribute<T> key, T value)指定新创建的 Channel 的属性值。这些属性值是通过bind()
     * 或者 connect()方法设置到Channel 的，具体取决于谁最先被调用。这个方法在 Channel 被创建后将不会有任何的效
     * 果。
     */
    public void bootstrappingClient() {

        EventLoopGroup group = new NioEventLoopGroup();
        //创建一个Bootstrap 类的实例以创建和连接新的客户端 Channel
        Bootstrap bootstrap = new Bootstrap();
        //设置 EventLoopGroup，提供用于处理 Channel 事件的 EventLoop
        bootstrap.group(group)
                //指定要使用的 Channel 实现
                .channel(NioSocketChannel.class)
                //设置用于 Channel 事件和数据的ChannelInboundHandler
                .handler(new SimpleChannelInboundHandler<ByteBuf>() {
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
                        System.out.println("Received data");
                    }
                });
        //连接到远程主机
        //在引导的过程中，在调用 bind()或者 connect()方法之前，必须调用以下方法来设置所需的组件：
        // group()；
        // channel()或者 channelFactory()；
        // handler()。
        ChannelFuture future = bootstrap.connect(new InetSocketAddress("www.manning.com", 80));
        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    System.out.println("Connection established");
                } else {
                    System.err.println("Connection attempt failed");
                    channelFuture.cause().printStackTrace();
                }
            }
        });
    }


    /**
     * childAttr() 将属性设置给已经被接受的子 Channel
     * childHandler 设置将被添加到已被接受的子 Channel 的 ChannelPipeline 中的 ChannelHandler。handler()方法
     * 和 childHandler()方法之间的区别是：前者所添加的 ChannelHandler 由接受子 Channel 的 ServerChannel 处理，
     * 而childHandler()方法所添加的 ChannelHandler 将由已被接受的子 Channel处理，其代表一个绑定到远程节点的套接字
     */
    public void bootstrappingServer() {

        EventLoopGroup group = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(group)
                //指定要使用的Channel 实现
                .channel(NioServerSocketChannel.class)
                //设置用于处理已被接受的子Channel的I/O及数 据的 ChannelInboundHandler
                .handler(new SimpleChannelInboundHandler<ByteBuf>() {
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
                        System.out.println("Received data");
                    }
                });
        //通过配置好的ServerBootstrap 的实例绑定该 Channel
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(8080));
        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    System.out.println("Connection established");
                } else {
                    System.err.println("Connection attempt failed");
                    channelFuture.cause().printStackTrace();
                }
            }
        });

    }

    /**
     * 假设你的服务器正在处理一个客户端的请求，这个请求需要它充当第三方系统的客户端。当
     * 一个应用程序（如一个代理服务器）必须要和组织现有的系统（如 Web 服务或者数据库）集成
     * 时，就可能发生这种情况。在这种情况下，将需要从已经被接受的子 Channel 中引导一个客户
     * 端 Channel
     */
    public void bootstrappingByChannel() {
        //创建 ServerBootstrap 以创建ServerSocketChannel，并绑定它
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap
            // 设置 EventLoopGroup，其将提供用以处理 Channel 事件的 EventLoop
            .group(new NioEventLoopGroup(), new NioEventLoopGroup())
            //指定要使用的Channel 实现
            .channel(NioServerSocketChannel.class)
            //设置用于处理已被接受的子 Channel 的 I/O 和数据的 ChannelInboundHandler
            .childHandler(new SimpleChannelInboundHandler<ByteBuf>() {

                ChannelFuture connectFuture;

                @Override
                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                    //创建一个 Bootstrap类的实例以连接 到远程主机
                    Bootstrap bootstrap = new Bootstrap();
                    //指定 Channel的实现
                    bootstrap.channel(NioSocketChannel.class).handler(
                            //为入站 I/O 设置ChannelInboundHandler
                            new SimpleChannelInboundHandler<ByteBuf>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
                                    System.out.println("Received data");
                                }
                            });
                    //使用与分配给已被接受的子 Channel相同的 EventLoop
                    bootstrap.group(ctx.channel().eventLoop());
                    //连接到远程节点
                    connectFuture = bootstrap.connect(new InetSocketAddress("www.manning.com", 80));
                }

                //当连接完成时，执行一些数据操作（如代理）
                @Override
                protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
                    if (connectFuture.isDone()) {
                        // do something with the data
                        //把服务器读到的数据发送给另一个服务器，相当于一个代理吧
                        //特别要注意这点
                    }
                }
            });
        //通过配置好的ServerBootstrap 绑定该 ServerSocketChannel
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(8080));
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    System.out.println("Server bound");
                } else {
                    System.err.println("Bind attempt failed");
                    channelFuture.cause().printStackTrace();
                }
            }
        });
    }

    /**
     * 在引导过程中添加多个handler
     * 所有我们展示过的代码示例中，我们都在引导的过程中调用了 handler()或者 childHandler()方法来添加单个的
     * ChannelHandler。这对于简单的应用程序来说可能已经足够了,了，但是它不能满足更加复杂的需求。例如，一个必
     * 须要支持多种协议的应用程序将会有很多的ChannelHandler，而不会是一个庞大而又笨重的类
     *
     * 你可以根据需要，通过在 ChannelPipeline 中将它们链接在一起来部署尽可能多的 ChannelHandler。但是，如
     * 果在引导的过程中你只能设置一个 ChannelHandler，那么你应该怎么做到这一点呢？
     *
     * 正是针对于这个用例，Netty 提供了一个特殊的 ChannelInboundHandlerAdapter 子类：
     * public abstract class ChannelInitializer<C extends Channel> extends ChannelInboundHandlerAdapter
     * {
     *     protected abstract void initChannel(C ch) throws Exception;
     * }
     * 这个方法提供了一种将多个 ChannelHandler 添加到一个 ChannelPipeline 中的简便方法。你只需要简单地向
     * Bootstrap 或 ServerBootstrap 的实例提供你的 ChannelInitializer 实现即可，并且一旦 Channel 被注
     * 册到了它的 EventLoop 之后，就会调用你的initChannel()版本。在该方法返回之后，ChannelInitializer 的
     * 实例将会从 ChannelPipeline 中移除它自己。
     */
    public void multiHandler(){
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                //注册一个 ChannelInitializerImpl 的实例来设置 ChannelPipeline
                .childHandler(new ChannelInitializerImpl());
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(8080));

    }

    //用以设置 ChannelPipeline 的自定义ChannelInitializerImpl 实现
    //在大部分的场景下，如果你不需要使用只存在于SocketChannel上的方法，使用ChannelInitializer<Channel>就可以了，
    //否则你可以使用ChannelInitializer<SocketChannel>，其中SocketChannel 扩展了Channel。
    final class ChannelInitializerImpl extends ChannelInitializer<Channel> {
        //将所需的ChannelHandler添加到ChannelPipeline
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new HttpClientCodec());
            pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
        }
    }


    /**
     * 在每个 Channel 创建时都手动配置它可能会变得相当乏味。幸运的是，你不必这样做。相
     * 反，你可以使用 option()方法来将 ChannelOption 应用到引导。你所提供的值将会被自动
     * 应用到引导所创建的所有 Channel。可用的 ChannelOption 包括了底层连接的详细信息，如
     * keep-alive 或者超时属性以及缓冲区设置。
     * Netty 应用程序通常与组织的专有软件集成在一起，而像 Channel 这样的组件可能甚至会在
     * 正常的 Netty 生命周期之外被使用。在某些常用的属性和数据不可用时，Netty 提供了
     * AttributeMap 抽象（一个由 Channel 和引导类提供的集合）以及 AttributeKey<T>（一
     * 个用于插入和获取属性值的泛型类）。使用这些工具，便可以安全地将任何类型的数据项与客户
     * 端和服务器 Channel（包含 ServerChannel 的子 Channel）相关联了
     *
     * 考虑一个用于跟踪用户和 Channel 之间的关系的服务器应用程序。这可以通过将用
     * 户的 ID 存储为 Channel 的一个属性来完成。类似的技术可以被用来基于用户的 ID 将消息路由
     * 给用户，或者关闭活动较少的 Channel。
     */
    public void channelOption(){
        //创建一个 AttributeKey以标识该属性,向管道传递一个额外的属性
        final AttributeKey<Integer> id = AttributeKey.newInstance("ID");
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new SimpleChannelInboundHandler<ByteBuf>() {
                    @Override
                    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
                        //使用 AttributeKey 检索属性以及它的值
                        Integer idValue = ctx.channel().attr(id).get();
                        // do something with the idValue
                    }
                    @Override
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
                        System.out.println("Received data");
                    }
                });
        //设置 ChannelOption，其将在 connect()或者bind()方法被调用时被设置到已经创建的 Channel 上===>没有看懂？？？
        bootstrap.option(ChannelOption.SO_KEEPALIVE,true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        //存储该id 属性
        bootstrap.attr(id, 123456);
        ChannelFuture future = bootstrap.connect(new InetSocketAddress("www.manning.com", 80));
        future.syncUninterruptibly();

    }

    /**
     * 优雅停机
     * 最重要的是，你需要关闭 EventLoopGroup，它将处理任何挂起的事件和任务，并且随后
     * 释放所有活动的线程。这就是调用 EventLoopGroup.shutdownGracefully()方法的作用。
     * 这个方法调用将会返回一个 Future，这个 Future 将在关闭完成时接收到通知。需要注意的是，
     * shutdownGracefully()方法也是一个异步的操作，所以你需要阻塞等待直到它完成，或者向
     * 所返回的 Future 注册一个监听器以在关闭完成时获得通知
     */
    public void closeNetty(){
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class);

        //shutdownGracefully()方法将释放所有的资源，并且关闭所有的当前正在使用中的 Channel
        Future<?> future = group.shutdownGracefully();
        // block until the group has shutdown
        future.syncUninterruptibly();

        //或者，你也可以在调用EventLoopGroup.shutdownGracefully()方法之前，显式地在所有活动的
        //Channel上调用Channel.close()方法。但是在任何情况下，都请记得关闭EventLoopGroup本身。

    }

}