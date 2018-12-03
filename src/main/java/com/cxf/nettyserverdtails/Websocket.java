package com.cxf.nettyserverdtails;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;

public class Websocket {


    //WebSocket

    /**
     * 但是我们还是应该提到，尽管最早的实现仅限于文本数据，但是现在已经不是问题了；
     * WebSocket 现在可以用于传输任意类型的数据，很像普通的套接字。
     * <p>
     * 要想向你的应用程序中添加对于WebSocket的支持，你需要将适当的客户端或者服务
     * 器WebSocket ChannelHandler 添加到 ChannelPipeline 中。这个类将处理由
     * WebSocket 定义的称为帧的特殊消息类型。
     *
     *
     * 要想为 WebSocket 添加安全性，只需要将 SslHandler 作为第一个 ChannelHandler 添加到ChannelPipeline 中
     */
    public class WebSocketServerInitializer extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ch.pipeline().addLast(
                    new HttpServerCodec(),
                    //为握手提供聚合的如果被请求 HttpRequest
                    new HttpObjectAggregator(65536),
                    //如果被请求 HttpRequest的端点是"/websocket"，则处理该升级握手
                    new WebSocketServerProtocolHandler("/websocket"),
                    //TextFrameHandler 处理TextWebSocketFrame
                    new TextFrameHandler(),
                    //BinaryFrameHandler 处理BinaryWebSocketFrame
                    new BinaryFrameHandler(),
                    //ContinuationFrameHandler 处理ContinuationWebSocketFrame
                    new ContinuationFrameHandler());
        }

    }

    public static final class TextFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
        @Override
        public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // Handle text frame
        }
    }

    public static final class BinaryFrameHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {
        @Override
        public void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame msg) throws Exception {
        // Handle binary frame
        }
    }

    public static final class ContinuationFrameHandler extends SimpleChannelInboundHandler<ContinuationWebSocketFrame> {
        @Override
        public void channelRead0(ChannelHandlerContext ctx, ContinuationWebSocketFrame msg) throws Exception {
        // Handle continuation frame
        }
    }


    // 检测空闲连接以及超时对于及时释放资源来说是至关重要的。由于这是一项常见的任务，
    // Netty 特地为它提供了几个 ChannelHandler 实现。

    // IdleStateHandler 当连接空闲时间太长时，将会触发一个 IdleStateEvent 事件。
    // 然后，你可以通过在你的ChannelInboundHandler 中重写 userEventTriggered()
    // 方法来处理该 IdleStateEvent 事件

    // ReadTimeoutHandler如果在指定的时间间隔内没有收到任何的入站数据，则抛出一个
    // ReadTimeoutException并关闭对应的Channel。可以通过重写你的ChannelHandler
    // 中的 exceptionCaught()方法来检测该 ReadTimeoutException

    // WriteTimeoutHandler 如果在指定的时间间隔内没有任何出站数据写入，则抛出一个
    // WriteTimeoutException并关闭对应的Channel。可以通过重写你的ChannelHandler
    // 的 exceptionCaught()方法检测该 WriteTimeoutException
    public class IdleStateHandlerInitializer extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            //IdleStateHandler 将在被触发时发送一 个IdleStateEvent 事件
            pipeline.addLast(new IdleStateHandler(0, 0, 60, TimeUnit.SECONDS));
            //将一个HeartbeatHandler添加到ChannelPipeline中
            pipeline.addLast(new HeartbeatHandler());
        }

    }

    //实现 userEventTriggered()方法以发送心跳消息
    public static final class HeartbeatHandler extends ChannelInboundHandlerAdapter {
        //发送到远程节点的心跳消息
        private static final ByteBuf HEARTBEAT_SEQUENCE
                = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("HEARTBEAT", CharsetUtil.ISO_8859_1));


        //如果连接超过 60 秒没有接收或者发送任何的数据，那么 IdleStateHandler 将会使用一个
        //IdleStateEvent 事件来调用 fireUserEventTriggered()方法。HeartbeatHandler 实现
        //了userEventTriggered()方法，如果这个方法检测到 IdleStateEvent 事件，它将会发送心
        //跳消息，并且添加一个将在发送操作失败时关闭该连接的 ChannelFutureListener 。
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

            if (evt instanceof IdleStateEvent) {
                //发送心跳消息，并在发送失败时关闭该连接
                ctx.writeAndFlush(HEARTBEAT_SEQUENCE.duplicate()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            } else {
                //不是IdleStateEvent事件，所以将它传递给下一个ChannelInboundHandler
                super.userEventTriggered(ctx, evt);
            }
        }
    }
}

