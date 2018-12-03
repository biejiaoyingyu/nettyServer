package com.cxf.nettyserverdtails;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.List;

/**
 * 将字节解码为消息——ByteToMessageDecoder 和 ReplayingDecoder；
 * 将一种消息类型解码为另一种——MessageToMessageDecoder。
 * 因为解码器是负责将入站数据从一种格式转换到另一种格式的，所以知道 Netty 的解码器实现了
 * ChannelInboundHandler 也不会让你感到意外
 *
 * 什么时候会用到解码器呢？很简单：每当需要为 ChannelPipeline 中的下一个 ChannelInboundHandler 转换入站数据时会用到。
 */
public class NettyCoder {

    /**
     * decode(ChannelHandlerContext ctx,ByteBuf in,List<Object> out)
     这是你必须实现的唯一抽象方法。decode()方法被调用时将会传
     入一个包含了传入数据的 ByteBuf，以及一个用来添加解码消息
     的 List。对这个方法的调用将会重复进行，直到确定没有新的元
     素被添加到该 List，或者该 ByteBuf 中没有更多可读取的字节
     时为止。然后，如果该 List 不为空，那么它的内容将会被传递给
     ChannelPipeline 中的下一个 ChannelInboundHandler

     decodeLast(ChannelHandlerContext ctx,ByteBuf in,List<Object> out)
     Netty提供的这个默认实现只是简单地调用了decode()方法。
     当Channel的状态变为非活动时，这个方法将会被调用一次。
     可以重写该方法以提供特殊的处理 ====>比如用来产生一个 LastHttpContent 消息。不懂
     */
    public void byteToMessageDecoder(){

    }

    /**
     * 扩展 ByteToMessageDecoder 类，以将字节解码为特定的格式
     */
    public class ToIntegerDecoder extends ByteToMessageDecoder {
        @Override
        public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            //检查是否至少有4字节可读（一个 int 的字节长度）
            if (in.readableBytes() >= 4) {
                //从入站 ByteBuf 中读取一个 int，并将其添加到 解码消息的 List 中

                //一旦消息被编码或者解码，它就会被 ReferenceCountUtil.release(message)调用
                //自动释放。如果你需要保留引用以便稍后使用，那么你可以调用 ReferenceCountUtil.retain(message)
                //方法。这将会增加该引用计数，从而防止该消息被释放。
                out.add(in.readInt());
            }
        }
    }

    /**
     * 扩展 ReplayingDecoder<Void>以将字节解码为消息
     */
    public class ToIntegerDecoder2 extends ReplayingDecoder<Void> {
        //传入的 ByteBuf 是ReplayingDecoderByteBuf
        @Override
        public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            //从入站 ByteBuf 中读取一个 int，并将其添加到 解码消息的 List 中
            out.add(in.readInt());
        }
    }

    /**
     * 扩展了MessageToMessageDecoder<Integer>
     */
    public class IntegerToStringDecoder extends MessageToMessageDecoder<Integer> {
        //注意参数，没有ByteBuf
        @Override
        public void decode(ChannelHandlerContext ctx, Integer msg,List<Object> out) throws Exception {
            //将 Integer 消息转换为它的 String 表示，并将其添加到输出的 List 中
            out.add(String.valueOf(msg));
        }
    }


    /**
     * 你可以设置一个最大字节数的阈值，如果超出该阈值，则会导致抛出一
     * 个 TooLongFrameException（随后会被 ChannelHandler.exceptionCaught()方法捕
     * 获）。然后，如何处理该异常则完全取决于该解码器的用户。某些协议（如 HTTP）可能允许你
     * 返回一个特殊的响应。而在其他的情况下，唯一的选择可能就是关闭对应的连接
     */
    public class SafeByteToMessageDecoder extends ByteToMessageDecoder {
        private static final int MAX_FRAME_SIZE = 1024;
        @Override
        public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            int readable = in.readableBytes();
            //检查缓冲区中是否有超过 MAX_FRAME_SIZE 个字节
            if (readable > MAX_FRAME_SIZE) {
                //跳过所有的可读字节，抛出TooLongFrameException 并通知 ChannelHandler
                in.skipBytes(readable);
                throw new TooLongFrameException("Frame too big!");
            }
            // do something

        }
    }

    /**
     * encode(ChannelHandlerContext ctx,I msg,ByteBuf out)
     * encode()方法是你需要实现的唯一抽象方法。它被调用时将会传入要被该类编码为 ByteBuf 的（类型为 I 的）出站
     * 消息。该 ByteBuf 随后将会被转发给 ChannelPipeline中的下一个 ChannelOutboundHandler
     *
     * 你可能已经注意到了，这个类只有一个方法，而解码器有两个。原因是解码器通常需要在
     * 关闭之后产生最后一个消息（因此也就有了 decodeLast()方法）。这显然不适用于
     * 编码器的场景——在连接被关闭之后仍然产生一个消息是毫无意义的。
     */
    public  void messageToByteEncoder(){

    }

    /**
     * 扩展了MessageToByteEncoder
     */
    public class ShortToByteEncoder extends MessageToByteEncoder<Short> {
        @Override
        public void encode(ChannelHandlerContext ctx, Short msg, ByteBuf out) throws Exception {
            //将 Short 写入ByteBuf 中
            out.writeShort(msg);
        }
    }


    /**
     * encode(ChannelHandlerContext ctx,I msg,List<Object> out)
     * 这是你需要实现的唯一方法。每个通过 write()方法写入的消
     * 息都将会被传递给 encode()方法，以编码为一个或者多个出
     * 站消息。随后，这些出站消息将会被转发给 ChannelPipeline
     * 中的下一个 ChannelOutboundHandler
     */
    public  void messageToMessageEncoder(){

    }

    /**
     * 扩展了MessageToMessageEncoder
     */
    public class IntegerToStringEncoder extends MessageToMessageEncoder<Integer> {
        @Override
        public void encode(ChannelHandlerContext ctx, Integer msg,List<Object> out) throws Exception {
            //将 Integer 转换为 String，并将其添加到 List 中
            out.add(String.valueOf(msg));
        }
    }

    /**
     *
     *
     * 我们需要将字节解码为某种形式的消息，可能是 POJO，随后再次对它进行编码。ByteToMessageCodec
     * 将为我们处理好这一切，因为它结合了ByteToMessageDecoder 以及它的逆向——MessageToByteEncoder。
     *
     *
     *
     * decode(ChannelHandlerContext ctx,ByteBuf in,List<Object>)只要有字节可以被消费，这个方法就它将入站
     * ByteBuf 转换为指定的消息格式，并将其转发给ChannelPipeline 中的下一个ChannelInboundHandler
     *
     * decodeLast(ChannelHandlerContext ctx,ByteBuf in,List<Object> out)这个方法的默认实现委托给了decode()方法。
     * 它只会在Channel 的状态变为非活动时被调用一次。它可以被重写以实现特殊的处理
     *
     * encode(ChannelHandlerContext ctx,I msg,ByteBuf out)对于每个将被编码并写入出站 ByteBuf 的（类型为 I 的）
     * 消息来说，这个方法都将会被调用
     */
    public void byteToMessageCodec(){

    }

    /**
     * 通过使用 MessageToMessageCodec，我们可以在一个单个的类中实现该转换的往返过程
     *
     *
     * protected abstract decode(ChannelHandlerContext ctx,INBOUND_IN msg,List<Object> out)这个方法被
     * 调用时会被传入 INBOUND_IN 类型的消息。它将把它们解码为 OUTBOUND_IN 类型的消息，这些消息将被转发给
     * ChannelPipeline中的下一个 ChannelInboundHandler
     *
     *
     * protected abstract encode(ChannelHandlerContext ctx,OUTBOUND_IN msg,List<Object> out)对于每个
     * OUTBOUND_IN 类型的消息，这个方法都将会被调用。这些消息将会被编码为 INBOUND_IN 类型的消息，然后被转发给
     * ChannelPipeline 中的下一个ChannelOutboundHandler
     */
    public void messageToMessageCodec(){

    }


}

