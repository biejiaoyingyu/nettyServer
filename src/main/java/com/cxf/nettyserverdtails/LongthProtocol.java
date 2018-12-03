package com.cxf.nettyserverdtails;


import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 基于长度的协议通过将它的长度编码到帧的头部来定义帧，而不是使用特殊的分隔符来标记它的结束。
 *
 * FixedLengthFrameDecoder 提取在调用构造函数时指定的定长帧
 * LengthFieldBasedFrameDecoder 根据编码进帧头部中的长度值提取帧；该字段的偏移量以及长度在构造函数中指定
 */
public class LongthProtocol {

    //FixedLengthFrameDecoder其在构造时已经指定了帧长度为8字节。

    // --- 解码前 ---            ---解码后---
    //    32 字节流 ------> 提取 4（帧） * 8 （字节）


    //LengthFieldBasedFrameDecoder，它将从头部字段确定帧长，然后从数据流中提取指定的字节数

    //       --- 解码前 ---              ---解码后---
    //   长度(2字节,12)-实际内容         实际内容（12字节）
    //   0x000C "hello. world" ------> "hello. world"


    /**
     * 3 个构造参数分别为 maxFrameLength、lengthFieldOffset 和 lengthFieldLength 的构造函数
     */
    public class LengthBasedInitializer extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            //使用 LengthFieldBasedFrameDecoder 解码将帧长度编码到帧起始的前 8 个字节中的消息
            pipeline.addLast(new LengthFieldBasedFrameDecoder(64 * 1024, 0, 8));
            //添加 FrameHandler以处理每个帧
            pipeline.addLast(new FrameHandler());
        }
    }

    public static final class FrameHandler extends SimpleChannelInboundHandler<ByteBuf> {
        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            //处理帧的数据
            // Do something with the frame
        }
    }
}
