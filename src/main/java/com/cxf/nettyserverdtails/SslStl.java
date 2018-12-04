package com.cxf.nettyserverdtails;


import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

/**
 * Java 提供了 javax.net.ssl 包，它的 SSLContext 和 SSLEngine
 * 类使得实现解密和加密相当简单直接。Netty 通过一个名为 SslHandler 的 ChannelHandler
 * 实现利用了这个 API，其中 SslHandler 在内部使用 SSLEngine 来完成实际的工作
 *
 *
 */
public class SslStl {

    /**
     * SslHandler 具有一些有用的方法:
     * 在握手阶段，两个节点将相互验证并且商定一种加密方式。你可以通过配置 SslHandler 来修改它的行为，
     * 或者在 SSL/TLS握手一旦完成之后提供通知，握手阶段完成之后，所有的数据都将会被加密。SSL/TLS 握手将会被自动执行
     *
     *
     *
     * ==================================================================================================
     *
     setHandshakeTimeout (long,TimeUnit)
     setHandshakeTimeoutMillis (long)
     getHandshakeTimeoutMillis()
     设置和获取超时时间，超时之后，握手ChannelFuture 将会被通知失败

     setCloseNotifyTimeout (long,TimeUnit)
     setCloseNotifyTimeoutMillis (long)
     getCloseNotifyTimeoutMillis()
     设置和获取超时时间，超时之后，将会触发一个关闭通知并关闭连接。这也将会导致通知该 ChannelFuture 失败

     handshakeFuture() 返回一个在握手完成后将会得到通知的ChannelFuture。如果握手先前已经执行过了，则返回
     一个包含了先前的握手结果的 ChannelFuture

     close()
     close(ChannelPromise)
     close(ChannelHandlerContext,ChannelPromise)发送 close_notify 以请求关闭并销毁底层的 SslEngine
     */
     class SslChannelInitializer extends ChannelInitializer<Channel> {
        private final SslContext context;
        //后面的SslHandler会有用
        private final boolean startTls;
        // 传入要使用的SslContext
        // 如果设置为 true，第一个写入的消息将不会被加密（客户端应该设置为 true）==>为什么？==>后面的SslHandler会有用
        public SslChannelInitializer(SslContext context, boolean startTls) {
            this.context = context;
            this.startTls = startTls;
        }
        @Override
        protected void initChannel(Channel ch) throws Exception {
            //对于每个SslHandler实例，都使用Channel的ByteBufAllocator从SslContext获取一个新的 SSLEngine
            SSLEngine engine = context.newEngine(ch.alloc());
            //将 SslHandler 作为第一个ChannelHandler 添加到 ChannelPipeline 中
            //在大多数情况下，SslHandler 将是 ChannelPipeline 中的第一个 ChannelHandler。
            //这确保了只有在所有其他的 ChannelHandler 将它们的逻辑应用到数据之后，才会进行加密。
            ch.pipeline().addFirst("ssl", new SslHandler(engine, startTls));
        }
    }

}
