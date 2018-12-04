package com.cxf.nettyserverdtails;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * Created by cxf on 2018/12/4.
 */
public class ChatServerInitializer extends ChannelInitializer<Channel> {
    private final ChannelGroup group;
    public ChatServerInitializer(ChannelGroup group) {
        this.group = group;
    }
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        //将字节解码为HttpRequest、HttpContent和LastHttpContent。并将HttpRequest、HttpContent 和 LastHttpContent 编码为字节
        pipeline.addLast(new HttpServerCodec());
        //写入一个文件的内容
        pipeline.addLast(new ChunkedWriteHandler());
        //将一个 HttpMessage 和跟随它的多个 HttpContent 聚合为单个FullHttpRequest或者FullHttpResponse（取决于它是被用来处理请求
        //还是响应）。安装了这个之后，ChannelPipeline 中的下一个 ChannelHandler 将只会收到完整的 HTTP 请求或响应
        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
        //处理 FullHttpRequest（那些不发送到/ws URI 的请求）
        pipeline.addLast(new HttpRequestHandler("/ws"));
        //按照 WebSocket 规范的要求，处理WebSocket升级握手、PingWebSocketFrame、PongWebSocketFrame 和 CloseWebSocketFrame
        //Netty 的 WebSocketServerProtocolHandler 处理了所有委托管理的WebSocket帧类型以及升级握手本身。如果握手成功，那么所需的
        //ChannelHandler 将会被添加到ChannelPipeline中，而那些不再需要的 ChannelHandler 则将会被移除。


        //当 WebSocket 协议升级完成之后，WebSocketServerProtocolHandler 将会把 HttpRequestDecoder 替换为 WebSocketFrameDecoder，
        // 把 HttpResponseEncoder 替换为WebSocketFrameEncoder。为了性能最大化，它将移除任何不再被 WebSocket 连接所需要的 ChannelHandler。

        //处理 TextWebSocketFrame 和握手完成事件
        pipeline.addLast(new TextWebSocketFrameHandler(group));
    }
}