package com.cxf.nettyserverdtails;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by cxf on 2018/12/4.
 * websocket
 */
//扩展SimpleChannelInboundHandler以处理FullHttpRequest消息
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final String wsUri;
    private static final File INDEX;

    //初始化了一些什么？建了一个文件
    static {
        URL location = HttpRequestHandler.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            String path = location.toURI() + "index.html";
            path = !path.contains("file:") ? path : path.substring(5);
            INDEX = new File(path);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to locate index.html", e);
        }
    }
    public HttpRequestHandler(String wsUri) {
        this.wsUri = wsUri;
    }
    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        //如果请求了WebSocket协议升级，则增加引用计数（调用 retain()方法），并将它传递给下一个ChannelInboundHandler
        //之所以需要调用 retain()方法，是因为调用 channelRead()方法完成之后，它将调用 FullHttpRequest 对象上的
        // release()方法以释放它的资源。
        if (wsUri.equalsIgnoreCase(request.uri())) {
            ctx.fireChannelRead(request.retain());
        } else {
            //处理 100 Continue请求以符合 HTTP 1.1 规范
            //如果客户端发送了 HTTP 1.1 的 HTTP 头信息 Expect: 100-continue，那么 HttpRequestHandler 将会发送一个 100 Continue 响应。

            //  HTTP/1.1 协议里设计 100 (Continue) HTTP 状态码的的目的是，在客户端发送 Request Message 之前，HTTP/1.1 协议允许客户端先
            //  判定服务器是否愿意接受客户端发来的消息主体（基于 Request Headers）。即， Client 和 Server 在 Post （较大）数据之前，允许双方
            // “握手”，如果匹配上了，Client 才开始发送（较大）数据。这么做的原因是，如果客户端直接发送请求数据，但是服务器又将该请求拒绝的话，这种
            // 行为将带来很大的资源开销。
            if (HttpHeaders.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }
            //读取index.html
            RandomAccessFile file = new RandomAccessFile(INDEX, "r");
            HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
            response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
            boolean keepAlive = HttpHeaders.isKeepAlive(request);

            // 如果请求了keep-alive，则添加所需要的 HTTP 头信息
            // http 1.0中默认是关闭的，需要在http头加入"Connection: Keep-Alive"，才能启用Keep-Alive；http 1.1中默认启用Keep-Alive，
            // 如果加入"Connection: close "，才关闭。目前大部分浏览器都是用http1.1协议，也就是说默认都会发起Keep-Alive的连接请求了，所
            // 以是否能完成一个完整的Keep-Alive连接就看服务器设置情况。
            if (keepAlive) {
                response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, file.length());
                response.headers().set( HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            }
            // 将 HttpResponse写到客户端
            // 在该 HTTP 头信息被设置之后，HttpRequestHandler 将会写回一个 HttpResponse 给客户端。
            // 这不是一个 FullHttpResponse，因为它只是响应的第一个部分。此外，这里也不会调用
            // writeAndFlush()方法，在结束的时候才会调用
            ctx.write(response);
            // 将 index.html写到客户端
            // 如果不需要加密和压缩，那么可以通过将index.html的内容存储到DefaultFileRegion中来达到最
            // 佳效率。这将会利用零拷贝特性来进行内容的传输。为此，你可以检查一下，是否有SslHandler存在
            // 于在ChannelPipeline 中。否则，你可以使用 ChunkedNioFile。
            if (ctx.pipeline().get(SslHandler.class) == null) {
                ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));
            } else {
                ctx.write(new ChunkedNioFile(file.getChannel()));
            }
            //写 LastHttpContent并冲刷至客户端
            ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

            //如果没有请求keep-alive，则在写操作完成后关闭 Channel
            //HttpRequestHandler 将写一个LastHttpContent来标记响应的结束。如果没有请求keep-alive，那么
            //HttpRequestHandler 将会添加一个ChannelFutureListener到最后一次写出动作的ChannelFuture，
            //并关闭该连接。在这里，你将调用 writeAndFlush() 方法以冲刷所有之前写入的消息。
            if (!keepAlive) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}