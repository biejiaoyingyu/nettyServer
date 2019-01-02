package com.cxf.special.handler;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * Created by cxf on 2019/1/2.
 */
public class CustomDecoder extends LengthFieldBasedFrameDecoder {


    private byte lenHead;//1
    private byte lenStart; //1
    private byte lenLen;  //4
    private byte skip;  //1
    private int lenValue; //x--->这里用实际内容的实际长度就可以了，而不是总长度
    private int totalLen = lenHead+lenLen+lenValue+1;
    private int index = totalLen-lenStart-lenLen-lenValue;
    private int index0 = 2;



//    /**
//     * 构造器，父类没有默认的构造器，所以需要传递指定参数调用父类的指定参数
//     * @param maxFrameLength             解码时，处理每个帧数据的最大长度（总长度？）  ---> 发送的数据帧最大长度
//     *
//     * @param lengthFieldOffset          该帧数据中，存放该帧数据的长度（是长度域）的数据的起始位置 --->定义长度域位于发送的字节数组中的下标。
//     *                                   换句话说：发送的字节数组中下标为${lengthFieldOffset}的地方是长度域的开始地方
//     *
//     * @param lengthFieldLength          记录该帧数据长度的字段本身的长度（是长度） ---> 用于描述定义的长度域的长度。换句话说：发送字节数组bytes时,
//     *                                   字节数组bytes[lengthFieldOffset, lengthFieldOffset+lengthFieldLength]域对应于的定义长度域部分
//     *
//     * @param lengthAdjustment           修改帧数据长度字段中定义的值，可以为负数
//     *                                   --->满足公式: 发送的字节数组bytes.length - lengthFieldLength = bytes[lengthFieldOffset, lengthFieldOffset+lengthFieldLength] + lengthFieldOffset + lengthAdjustment 
//
//     * @param initialBytesToStrip        解析的时候需要跳过的字节数 -->接收到的发送数据包，去除前initialBytesToStrip位
//     *
//     * @param failFast                   为true，当frame长度超过maxFrameLength时立即报TooLongFrameException异常，为false，读取完整个帧再报异常
//     *                                   -->默认为true建议不要修改，会发生内存溢出
//     */
    //new CustomDecoder(1024,(byte)1,(byte)4,(byte)2,(byte)1,(byte)1)

    //LengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, boolean failFast)
    public CustomDecoder(int maxFrameLength, byte lenStart , byte lenLen, byte index0, byte skip,byte lenHead) {
        //实际上这里也要传递参数，因为父类没有默认构造器，但是这些参数会不会起作用，应该会吧---->也没有仔细看源码，还是尽量正确吧。
        super(maxFrameLength, lenStart, lenLen, index0, skip, true);
        this.lenHead = lenHead;
        this.skip = skip;
        this.index0 = index0;
        this.lenStart = lenStart;
        this.lenLen = lenLen;

    }

    //这里实际上是重写了LengthFieldBasedFrameDecoder的decode方法，而不是ByteToMessageDecoder
    //应该也可以直接用父类的方法
//    @Override
//    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
//        if (in == null) {
//            return null;
//        }
//        if (in.readableBytes() < 7) {
//            throw new Exception("可读信息比是头信息小");
//        }
//
//        //注意在读的过程中，readIndex的指针也在移动
//        in.skipBytes(skip);
//        int length = in.readInt();
//        in.skipBytes(lenHead);
//        if (in.readableBytes() < length) {
//            throw new Exception("内容不够");
//        }
//
//        //三板斧
//        ByteBuf buf = in.readBytes(length);
//        byte[] req = new byte[buf.readableBytes()];
//        buf.readBytes(req);
//
//        String body = new String(req, "UTF-8");
//        CustomMsg customMsg = new CustomMsg(body);
//        return customMsg;
//    }

}
