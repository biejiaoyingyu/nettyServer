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
 */
@ChannelHandler.Sharable //===>标示一个ChannelHandler 可以被多个 Channel 安全地共享
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
