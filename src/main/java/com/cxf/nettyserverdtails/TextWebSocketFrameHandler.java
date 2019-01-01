package com.cxf.nettyserverdtails;

/**
 * Created by cxf on 2018/12/4.
 */

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * 由 IETF 发布的 WebSocket RFC，定义了 6 种帧，Netty 为它们每种都提供了一个 POJO 实现
 *
 * BinaryWebSocketFrame --> 包含了二进制数据
 * TextWebSocketFrame --> 包含了文本数据
 * ContinuationWebSocketFrame --> 包含属于上一个BinaryWebSocketFrame或TextWebSocketFrame 的文本数据或者二进制数据
 * CloseWebSocketFrame --> 表示一个 CLOSE 请求，包含一个关闭的状态码和关闭的原因
 * PingWebSocketFrame --> 请求传输一个 PongWebSocketFrame
 * PongWebSocketFrame -->作为一个对于 PingWebSocketFrame 的响应被发送
 */

/**
 * 扩展 SimpleChannelInboundHandler，并处理 TextWebSocketFrame 消息
 */
public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private final ChannelGroup group;
    public TextWebSocketFrameHandler(ChannelGroup group) {
        this.group = group;
    }
    //重写 userEventTriggered()方法以处理自定义事件
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            //如果该事件表示握手成功，则从该ChannelPipeline中移除HttpRequestHandler，因为将不会接收到任何HTTP消息了???
            ctx.pipeline().remove(HttpRequestHandler.class);
            //通知所有已经连接的WebSocket 客户端新 的客户端已经连接上了
            group.writeAndFlush(new TextWebSocketFrame("Client " + ctx.channel() + " joined"));
            //将新的WebSocketChannel添加到ChannelGroup中，以便它可以接收到所有的消息
            group.add(ctx.channel());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    //增加消息的引用计数，并将它写到 ChannelGroup 中所有 已经连接的客户端
    @Override
    public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 和之前一样，对于 retain()方法的调用是必需的，因为当 channelRead0()方法返回时，TextWebSocketFrame
        // 的引用计数将会被减少。由于所有的操作都是异步的，因此，writeAndFlush()方法可能会在 channelRead0()
        // 方法返回之后完成，而且它绝对不能访问一个已经失 效的引用
        group.writeAndFlush(msg.retain());
    }
}

