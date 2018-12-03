package com.cxf.nettyserverdtails;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

public class NettHttpHttps {

    //---------------------- FullHttpRequest-----------------------------
    // HttpRequest --- HttpContent --- HttpContent --- LastHttpContent---
    // HttpRequest -->Http请求的第一个部分，包含了http请求头信息
    // HttpContent -->HttpContent包含了数据，后面还可能跟着多个HttpContent部分
    // LastHttpContent -->LastHttpContent表示该请求的结束，可能含有尾随的请求头信息

    //---------------------- FullHttpResponse-----------------------------
    // HttpResponse --- HttpContent --- HttpContent --- LastHttpContent---
    // HttpResponse -->Http响应第一个部分，包含了http响应头信息
    // HttpContent -->HttpContent包含了数据，后面还可能跟着多个HttpContent部分
    // LastHttpContent -->LastHttpContent表示该请求的结束，可能含有尾随的响应头的信息

    //HttpRequestEncoder 将HttpRequest、HttpContent 和 LastHttpContent 消息编码为字节
    //HttpResponseEncoder 将HttpResponse、HttpContent 和LastHttpContent 消息编码为字节
    //HttpRequestDecoder 将字节解码为HttpRequest、HttpContent 和 LastHttpContent 消息
    //HttpResponseDecoder 将字节解码为HttpResponse、HttpContent 和LastHttpContent 消息


    /**
     *
     * 在 ChannelInitializer 将 ChannelHandler 安装到 ChannelPipeline 中之后，你
     便可以处理不同类型的 HttpObject 消息了。但是由于 HTTP 的请求和响应可能由许多部分组
     成，因此你需要聚合它们以形成完整的消息。为了消除这项繁琐的任务，Netty 提供了一个聚合
     器，它可以将多个消息部分合并为 FullHttpRequest 或者 FullHttpResponse 消息。通过
     这样的方式，你将总是看到完整的消息内容。

     */

    public class HttpPipelineInitializer extends ChannelInitializer<Channel> {
        private final boolean client;

        public HttpPipelineInitializer(boolean client) {
            this.client = client;
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            if (client) {
                //如果是客户端，则添加HttpResponseDecoder 以 处理来自服务器的响应
                pipeline.addLast("decoder", new HttpResponseDecoder());
                //如果是服务器，则添加 HttpResponseEncoder以向客户端发送响应
                pipeline.addLast("encoder", new HttpRequestEncoder());
            } else {
                //如果是服务器，则添加 HttpRequestDecoder以接收来自客户端的请求
                pipeline.addLast("decoder", new HttpRequestDecoder());
                //如果是服务器，则添加 HttpResponseEncoder以向客户端发送响应
                pipeline.addLast("encoder", new HttpResponseEncoder());
            }
        }
    }

    /**
     *
     由于消息分段需要被缓冲，直到可以转发一个完整的消息给下一个 ChannelInboundHandler，
     所以这个操作有轻微的开销。其所带来的好处便是你不必关心消息碎片了。引入这种自动聚合机制
     只不过是向 ChannelPipeline 中添加另外一个 ChannelHandler罢了。
     */
    public class HttpAggregatorInitializer extends ChannelInitializer<Channel> {

        private final boolean isClient;
        public HttpAggregatorInitializer(boolean isClient) {
            this.isClient = isClient;
        }
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();

            if (isClient) {
                //如果是客户端，则添加 HttpClientCodec
                pipeline.addLast("codec", new HttpClientCodec());
            } else {
                //如果是服务器，则添加 HttpServerCodec
                pipeline.addLast("codec", new HttpServerCodec());
            }
            //将最大的消息大小为 512 KB的 HttpObjectAggregator 添加 到 ChannelPipeline
            pipeline.addLast("aggregator", new HttpObjectAggregator(512 * 1024));
        }
    }

    /**
     * 当使用 HTTP 时，建议开启压缩功能以尽可能多地减小传输数据的大小。虽然压缩会带来一
     * 些 CPU 时钟周期上的开销，但是通常来说它都是一个好主意，特别是对于文本数据来说。
     *
     * Netty为压缩和解压缩提供了ChannelHandler实现，它们同时支持gzip和deflate编码
     *
     * 然而，需要注意的是，服务器没有义务压缩它所发送的数据。
     */
    public class HttpCompressionInitializer extends ChannelInitializer<Channel> {
        private final boolean isClient;
        public HttpCompressionInitializer(boolean isClient) {
            this.isClient = isClient;
        }
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            if (isClient) {
                //如果是客户端，则添加 HttpClientCodec
                pipeline.addLast("codec", new HttpClientCodec());
                //如果是客户端，则添加HttpContentDecompressor 以处理来自服务器的压缩内容
                pipeline.addLast("decompressor", new HttpContentDecompressor());
            } else {
                //如果是服务器，则添加 HttpServerCodec
                pipeline.addLast("codec", new HttpServerCodec());
                //如果是服务器，则添加HttpContentCompressor来压缩数据（如果客户端支持它）
                pipeline.addLast("compressor", new HttpContentCompressor());
            }
        }
    }


    //使用 HTTPS
    public class HttpsCodecInitializer extends ChannelInitializer<Channel> {
        private final SslContext context;
        private final boolean isClient;
        public HttpsCodecInitializer(SslContext context, boolean isClient) {
            this.context = context;
            this.isClient = isClient;
        }
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            SSLEngine engine = context.newEngine(ch.alloc());
            //将 SslHandler 添加到ChannelPipeline 中以 使用 HTTPS
            pipeline.addFirst("ssl", new SslHandler(engine));
            if (isClient) {
                //如果是客户端，则添加 HttpClientCodec
                pipeline.addLast("codec", new HttpClientCodec());
            } else {
                //如果是服务器，则添加 HttpServerCodec
                pipeline.addLast("codec", new HttpServerCodec());
            }
        }
    }




}
