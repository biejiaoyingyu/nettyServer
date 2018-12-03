package com.cxf.nettyserverdtails;


import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.LineBasedFrameDecoder;

/**
 * 基于分隔符的协议
 *
 * DelimiterBasedFrameDecoder 使用任何由用户提供的分隔符来提取帧的通用解码器
 * LineBasedFrameDecoder 提取由行尾符（\n 或者\r\n）分隔的帧的解码器。这个解码
 * 器比 DelimiterBasedFrameDecoder 更快
 */
public class Delimited {
    final static byte SPACE = (byte)' ';

        // ------------- 由行尾符分隔的帧 ------------------

        // 字节流： ABC\r\nDEF\r\n ---->  第一个帧ABC\r\n -- 第二个帧 DEF\r\n

    public class LineBasedHandlerInitializer extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            //该LineBasedFrameDecoder将提取的帧转发给下一个ChannelInboundHandler
            pipeline.addLast(new LineBasedFrameDecoder(64 * 1024));
            //添加 FrameHandler以接收帧
            pipeline.addLast(new FrameHandler());
        }

    }
    public static final class FrameHandler extends SimpleChannelInboundHandler<ByteBuf> {
        //传入了单个帧的内容
        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            // Do something with the data extracted from the frame
        }
    }

    /**
     * 这些解码器是实现你自己的基于分隔符的协议的工具。作为示例，我们将使用下面的协议规范：
      传入数据流是一系列的帧，每个帧都由换行符（\n）分隔；
      每个帧都由一系列的元素组成，每个元素都由单个空格字符分隔；
      一个帧的内容代表一个命令，定义为一个命令名称后跟着数目可变的参数。

     我们用于这个协议的自定义解码器将定义以下类：
      Cmd----->将帧（命令）的内容存储在 ByteBuf 中，一个 ByteBuf 用于名称，另一个用于参数；
      CmdDecoder----->从被重写了的 decode()方法中获取一行字符串，并从它的内容构建一个 Cmd 的实例；
      CmdHandler----->从 CmdDecoder 获取解码的 Cmd 对象，并对它进行一些处理；
      CmdHandlerInitializer ------>为了简便起见，我们将会把前面的这些类定义为专门的ChannelInitializer
     的嵌套类，其将会把这些ChannelInboundHandler 安装到 ChannelPipeline 中。

     */

    public class CmdHandlerInitializer extends ChannelInitializer<Channel> {

        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            //添加 CmdDecoder 以提取Cmd 对象，并将它转发给下一个ChannelInboundHandler
            pipeline.addLast(new CmdDecoder(64 * 1024));
            //添加 CmdHandler 以接收和处理 Cmd 对象
            pipeline.addLast(new CmdHandler());
        }


    }
    //Cmd POJO
    public static final class Cmd {
        private final ByteBuf name;
        private final ByteBuf args;
        public Cmd(ByteBuf name, ByteBuf args) {
            this.name = name;
            this.args = args;
        }
        public ByteBuf name() {
            return name;
        }
        public ByteBuf args() {
            return args;
        }
    }

    //从 ByteBuf 中提取由行尾符序列分隔的帧
    public static final class CmdDecoder extends LineBasedFrameDecoder {
        public CmdDecoder(int maxLength) {
            super(maxLength);
        }
        @Override
        protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
            //从 ByteBuf 中提取由行尾符序列分隔的帧
            ByteBuf frame = (ByteBuf) super.decode(ctx, buffer);
            //如果输入中没有帧，则返回 null
            if (frame == null) {
                return null;
            }
            //查找第一个空格字符的索引。前面是命令名称，接着是参数
            int index = frame.indexOf(frame.readerIndex(), frame.writerIndex(), SPACE);
            //使用包含有命令名称和参数的切片创 建新的Cmd 对象
            return new Cmd(frame.slice(frame.readerIndex(), index), frame.slice(index + 1, frame.writerIndex()));
        }

    }
    public static final class CmdHandler extends SimpleChannelInboundHandler<Cmd> {
        //处理传经 ChannelPipeline的 Cmd 对象
        @Override
        public void channelRead0(ChannelHandlerContext ctx, Cmd msg) throws Exception {
            // Do something with the command
        }
    }
}
