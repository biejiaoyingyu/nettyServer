package com.cxf.nettyserverdtails;


import io.netty.channel.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * 因为网络饱和的可能性，如何在异步框架中高效地写大块的数据是一个特殊的问题。由于写操作是
 * 非阻塞的，所以即使没有写出所有的数据，写操作也会在完成时返回并通知 ChannelFuture。当
 * 这种情况发生时，如果仍然不停地写入，就有内存耗尽的风险。所以在写大型数据时，需要准备好
 * 处理到远程节点的连接是慢速连接的情况，这种情况会导致内存释放的延迟。让我们考虑下将一个
 * 文件内容写出到网络的情况。
 *
 *
 *
 * ---------------
 * NIO 的零拷贝特性，这种特性消除了将文件的内容从文件系统移动到网络栈的复制过程。所有的这
 * 一切都发生在 Netty 的核心中，所以应用程序所有需要做的就是使用一个 FileRegion 接口的实
 * 现，其在 Netty 的 API 文档中的定义是：“通过支持零拷贝的文件传输的 Channel 来发送的文
 * 件区域。
 */
public class BigData {

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
                new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        //处理失败
                        if (!future.isSuccess()) {
                            Throwable cause = future.cause();
                                // Do something
                        }
                    }
                });

    }

}
