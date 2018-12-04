package com.cxf.nettyserverdtails;


import io.netty.channel.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * 因为网络饱和的可能性，如何在异步框架中高效地写大块的数据是一个特殊的问题。由于写操作是
 * 非阻塞的，所以即使没有写出(写出)所有的数据，写操作也会在完成时返回并通知 ChannelFuture。当
 * 这种情况发生时，如果仍然不停地写入（写入），就有内存耗尽的风险。所以在写大型数据时，需要准备好
 * 处理到远程节点的连接是慢速连接的情况，这种情况会导致内存释放的延迟。让我们考虑下将一个
 * 文件内容写出到网络的情况。
 * 上面的话的意思是：因为netty是非阻塞的，所以上一次读到内存的数据还没有完全写出，但是会继续读入内存。
 * 这样会导致内存溢出。
 * --------------------------------------
 * NIO 的零拷贝特性，这种特性消除了将文件的内容从文件系统移动到网络栈的复制过程。所有的这
 * 一切都发生在 Netty 的核心中，所以应用程序所有需要做的就是使用一个 FileRegion 接口的实
 * 现，其在 Netty 的 API 文档中的定义是：“通过支持零拷贝的文件传输的 Channel 来发送的文
 * 件区域。
 */
public class BigData {


    /**
     * 这个示例只适用于文件内容的直接传输，不包括应用程序对数据的任何处理。
     * @throws FileNotFoundException
     */
    public void zeroCopy() throws FileNotFoundException {
        String file = null;
        //创建一个FileInputStream
        FileInputStream in = new FileInputStream(file);
        //以该文件的完整长度创建一个新的DefaultFileRegion
        FileRegion region = new DefaultFileRegion(in.getChannel(), 0, file.length());

        Channel channel = null;
        //发送该 DefaultFileRegion，并注册一个ChannelFutureListener
        channel
            .writeAndFlush(region)
            .addListener(
                t-> {
                    //处理失败
                    if (!t.isSuccess()) {
                        Throwable cause = t.cause();
                            // Do something
                    }
                }
            );
    }

    //在需要将数据从文件系统复制到用户内存中时，可以使用 ChunkedWriteHandler，它支持异步写大型数据流，而又不会导致大量的内存消耗
    //关键是 interface ChunkedInput<B>，其中类型参数 B 是 readChunk()方法返回的类型
    //ChunkedFile 从文件中逐块获取数据，当你的平台不支持零拷贝或者你需要转换数据时使用
    //ChunkedNioFile 和 ChunkedFile 类似，只是它使用了 FileChannel
    //ChunkedStream 从 InputStream 中逐块传输内容
    //ChunkedNioStream 从 ReadableByteChannel 中逐块传输内容


    public class ChunkedWriteHandlerInitializer extends ChannelInitializer<Channel> {
        private final File file;
        private final SslContext sslCtx;
        public ChunkedWriteHandlerInitializer(File file, SslContext sslCtx) {
            this.file = file;
            this.sslCtx = sslCtx;
        }
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            //将 SslHandler 添加到ChannelPipeline 中
            pipeline.addLast(new SslHandler(sslCtx.newEngine(ch.alloc())));
            //添加 ChunkedWriteHandler以处理作为 ChunkedInput 传入的数据
            pipeline.addLast(new ChunkedWriteHandler());
            //一旦连接建立，WriteStreamHandler 就开始写文件数据
            pipeline.addLast(new WriteStreamHandler());
        }
        public final class WriteStreamHandler extends ChannelInboundHandlerAdapter {

            //
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                super.channelActive(ctx);
                ctx.writeAndFlush(new ChunkedStream(new FileInputStream(file)));
            }
        }
    }
}
