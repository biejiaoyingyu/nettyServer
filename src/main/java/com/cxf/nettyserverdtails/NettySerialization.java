package com.cxf.nettyserverdtails;
import com.google.protobuf.MessageLite;
import io.netty.channel.*;
import io.netty.handler.codec.marshalling.MarshallerProvider;
import io.netty.handler.codec.marshalling.MarshallingDecoder;
import io.netty.handler.codec.marshalling.MarshallingEncoder;
import io.netty.handler.codec.marshalling.UnmarshallerProvider;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;

import java.io.Serializable;

/**
 * Created by cxf on 2018/12/4.
 */
public class NettySerialization {
    /**
      *  JDK 提供了 ObjectOutputStream 和 ObjectInputStream，用于通过网络对 POJO 的
         基本数据类型和图进行序列化和反序列化。该 API 并不复杂，而且可以被应用于任何实现了
         java.io.Serializable 接口的对象。但是它的性能也不是非常高效的。
         如果你的应用程序必须要和使用了ObjectOutputStream和ObjectInputStream的远
         程节点交互，并且兼容性也是你最关心的，那么JDK序列化将是正确的选择

         CompatibleObjectDecoder---> 和使用 JDK 序列化的非基于 Netty 的远程节点进行互操作的解码器
         CompatibleObjectEncoder---> 和使用 JDK 序列化的非基于 Netty 的远程节点进行互操作的编码器
         ObjectDecoder---> 构建于 JDK 序列化之上的使用自定义的序列化来解码的解码器；
                           当没有其他的外部依赖时，它提供了速度上的改进。否则其他的序列化实现更加可取
         ObjectEncoder---> 构建于 JDK 序列化之上的使用自定义的序列化来编码的编码器；
                           当没有其他的外部依赖时，它提供了速度上的改进。否则其他的序列化实现更加可取
     */

    /**
     * 如果你可以自由地使用外部依赖，那么JBoss Marshalling将是个理想的选择：它比JDK序列化最多快 3 倍，
     * 而且也更加紧凑。
     *
     * CompatibleMarshallingDecoder , CompatibleMarshallingEncoder 与只使用 JDK 序列化的远程节点兼容
     * MarshallingDecoder, MarshallingEncoder 适用于使用 JBoss Marshalling 的节点。这些类必须一起使用
     */

    public class MarshallingInitializer extends ChannelInitializer<Channel> {
        private final MarshallerProvider marshallerProvider;
        private final UnmarshallerProvider unmarshallerProvider;
        public MarshallingInitializer(UnmarshallerProvider unmarshallerProvider, MarshallerProvider marshallerProvider) {
            this.marshallerProvider = marshallerProvider;
            this.unmarshallerProvider = unmarshallerProvider;
        }
        @Override
        protected void initChannel(Channel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();
            //添加 MarshallingDecoder 以将 ByteBuf 转换为 POJO
            pipeline.addLast(new MarshallingDecoder(unmarshallerProvider));
            //添加 MarshallingEncoder 以将POJO转换为 ByteBuf
            pipeline.addLast(new MarshallingEncoder(marshallerProvider));
            //添加 ObjectHandler，以处理普通的实现了
            pipeline.addLast(new ObjectHandler());
        }
    }
    public static final class ObjectHandler extends SimpleChannelInboundHandler<Serializable> {
        @Override
        public void channelRead0(ChannelHandlerContext channelHandlerContext, Serializable serializable) throws Exception {
            // Do something
        }
    }

    /**
     * Netty序列化的最后一个解决方案是利用Protocol Buffers 的编解码器，它是一种由Google公司开发的、现在已经开源的数据交换格式。
     * Protocol Buffers 以一种紧凑而高效的方式对结构化的数据进行编码以及解码。它具有许多的编程语言绑定，使得它很适合跨语言的项目。
     *
     * ProtobufDecoder---> 使用 protobuf 对消息进行解码
     * ProtobufEncoder---> 使用 protobuf 对消息进行编码
     * ProtobufVarint32FrameDecoder---> 根据消息中的 Google Protocol Buffers 的“Base 128 Varints”a整型长度字段值动态地分割所接收到的 ByteBuf
     * ProtobufVarint32LengthFieldPrepender---> 向 ByteBuf 前追加一个 Google Protocal Buffers 的“Base128 Varints”整型的长度字段值
     */
    public class ProtoBufInitializer extends ChannelInitializer<Channel> {
        private final MessageLite lite;
        public ProtoBufInitializer(MessageLite lite) {
            this.lite = lite;
        }
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            //添加 ProtobufVarint32FrameDecoder以分隔帧
            pipeline.addLast(new ProtobufVarint32FrameDecoder());
            //添加 ProtobufEncoder以处理消息的编码
            //还需要在当前的 ProtobufEncoder 之前添加一个相应的 ProtobufVarint32LengthFieldPrepender以编码进帧长度信息。==>不懂
            pipeline.addLast(new ProtobufEncoder());
            //添加 ProtobufDecoder以解码消息
            pipeline.addLast(new ProtobufDecoder(lite));
            //添加 ObjectHandler 以处理解码消息
            pipeline.addLast(new ObjectHandler());
        }
    }


}
