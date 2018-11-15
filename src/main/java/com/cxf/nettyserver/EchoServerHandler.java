package com.cxf.nettyserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * 所有的nettyServer必须包含两个部分，
 *   一个部分是ChannelHandler------>该组件实现了服务器对从客户端接收的数据的处理，即它的业务逻辑
 *   第二个部分是引导------>这是配置服务器的启动代码。至少，它会将服务器绑定到它要监听连接请求的端口上
 *
 *   --------------------------------------------------------------------------------------
 *   因为你的 Echo 服务器会响应传入的消息，所以它需要实现 ChannelInboundHandler 接口，用来定义响应
 *   入站事件的方法。这个简单的应用程序只需要用到少量的这些方法，所以继承ChannelInboundHandlerAdapter
 *   类也就足够了，它提供了 ChannelInboundHandler 的默认实现
 *
 *   ---------------------------------------------------------------------------------------
 *   通过使用作为参数传递到每个方法的 ChannelHandlerContext，事件可以被传递给当前
 *   ChannelHandler 链中的下一个 ChannelHandler。因为你有时会忽略那些不感兴趣的事件，所以 Netty
 *   提供了抽象基类 ChannelInboundHandlerAdapter 和 ChannelOutboundHandlerAdapter。通过调
 *   用 ChannelHandlerContext 上的对应方法，每个都提供了简单地将事件传递给下一个ChannelHandler
 *   的方法的实现。随后，你可以通过重写你所感兴趣的那些方法来扩展这些类
 *
 *   --------------------------------------------------------------------------------------
 *   鉴于出站操作和入站操作是不同的，你可能会想知道如果将两个类别的 ChannelHandler都混合添加到同一个
 *   ChannelPipeline 中会发生什么。虽然 ChannelInboundHandle 和ChannelOutboundHandle 都扩展
 *   自 ChannelHandler，但是 Netty 能区分 ChannelInboundHandler 实现和 ChannelOutboundHandler
 *   实现，并确保数据只会在具有相同定向类型的两个 ChannelHandler 之间传递。
 *
 *   --------------------------------------------------------------------------------------
 *   当ChannelHandler被添加到ChannelPipeline时，它将会被分配一个ChannelHandlerContext，其代表了
 *   ChannelHandler和ChannelPipeline 之间的绑定。虽然这个对象可以被用于获取底层的 Channel，但是它
 *   主要还是被用于写出站数据。在 Netty中，有两种发送消息的方式。你可以直接写到 Channel中，也可以写到和
 *   ChannelHandler相关联的ChannelHandlerContext对象中。前一种方式将会导致消息从ChannelPipeline
 *   的尾端开始流动，而后者将导致消息从 ChannelPipeline 中的下一个 ChannelHandler 开始流动====>这个很重要
 *   ----------------------------------------------------------------------------------------
 *
 *   Netty 以适配器类的形式提供了大量默认的 ChannelHandler 实现，其旨在简化应用程序处理逻辑的开发过程。
 *   1.ChannelHandlerAdapter
 *   2.ChannelInboundHandlerAdapter
 *   3.ChannelOutboundHandlerAdapter
 *   4.ChannelDuplexHandler
 *   -----------------------------------------------------------------------------------------
 *   但是，正如有用来简化ChannelHandler 的创建的适配器类一样，所有由 Netty 提供的编码器/解码器适配器类都实现了
 *   ChannelOutboundHandler 或者 ChannelInboundHandler 接口
 *
 *   ---------------------------------------------------------------------------------------------
 *  利用一个 ChannelHandler 来接收解码消息,要创建一个这样的 ChannelHandler，你只需要扩展基类
 *  SimpleChannelInboundHandler<T>，其中 T 是你要处理的消息的 Java 类型 。在这个
 *  ChannelHandler 中，你将需要重写基类的一个或者多个方法，并且获取一个到ChannelHandlerContext
 *  的引用，这个引用将作为输入参数传递给 ChannelHandler 的所有方法
 *
 *  --------------------------------------------------------------------------------------------
 *
 *
 *
 */
@ChannelHandler.Sharable //===>标示一个ChannelHandler 可以被多个 Channel 安全地共享
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 对于每个传入的消息都要调用
     * @param ctx
     * @param msg
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        System.out.println("Server received: " + in.toString(CharsetUtil.UTF_8));
        //将接收到的消息写给发送者，而不冲刷出站消息
        ctx.write(in);
    }

    /**
     * 通知ChannelInboundHandler最后一次对channelRead()的调用是当前批量读取中的最后一条消息
     * @param ctx
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        //将未决消息冲刷到远程节点，并且关闭该 Channel
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 在读取操作期间，有异常抛出时会调用
     *
     * 每个 Channel 都拥有一个与之相关联的 ChannelPipeline，其持有一个ChannelHandler的实例链。
     * 在默认的情况下，ChannelHandler 会把对它的方法的调用转发给链中的下一个 ChannelHandler。
     * 因此，如果 exceptionCaught()方法没有被该链中的某处实现，那么所接收的异常将会被传递到
     * ChannelPipeline 的尾端并被记录。为此，你的应用程序应该提供至少有一个实现了exceptionCaught()
     * 方法的 ChannelHandler。
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        //关闭Channel
        ctx.close();
    }
}
