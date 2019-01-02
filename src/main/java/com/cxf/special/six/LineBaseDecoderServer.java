package com.cxf.special.six;

import com.cxf.special.six.handler.BaseServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;

public class LineBaseDecoderServer {

    private int port;

    private LineBaseDecoderServer(int port) {
        this.port = port;
    }

    public void start() {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        //cpu数量*2
        EventLoopGroup work = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap()
                    .group(boss, work)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler())
                    .localAddress(new InetSocketAddress(port))
                    //给客户端用
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    //1.先读完一行
                                    //.addLast(new LineBasedFrameDecoder(2048))
                                    //如果减半--->io.netty.handler.codec.TooLongFrameException: frame length (1076) exceeds the allowed maximum (1024)
                                    //大概的意思是一行太长1024容不下
                                    //.addLast(new LineBasedFrameDecoder(2048))

                                    //2.固定截取字符数
                                    //.addLast(new FixedLengthFrameDecoder(14))

                                    //3.用DelimiterBasedFrameDecoder解决
                                    .addLast(new DelimiterBasedFrameDecoder(1024, Unpooled.copiedBuffer("$$__".getBytes())))
                                    //再解码
                                    .addLast(new StringDecoder())
                                    .addLast(new StringEncoder())
                                    .addLast(new BaseServerHandler());
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);


            ChannelFuture future = serverBootstrap.bind().sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            work.shutdownGracefully();
        }

    }
    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }
        new LineBaseDecoderServer(port).start();
    }

}
