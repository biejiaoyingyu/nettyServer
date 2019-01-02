package com.cxf.special.six.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class LineBaseDecoderClientHandler  extends ChannelInboundHandlerAdapter{

    private byte[] req;
    private int count;


    public LineBaseDecoderClientHandler() {
        System.out.println(System.getProperty("line.separator"));

         //1.用大字符串来检测拆包现象
//        this.req = ("In this chapter you general, we recommend Java Concurrency in Practice by Brian Goetz. His book w"
//                + "ill give We’ve reached an exciting point—in the next chapter we’ll discuss bootstrapping, the process "
//                + "of configuring and connecting all of Netty’s components to bring your learned about threading models in ge"
//                + "neral and Netty’s threading model in particular, whose performance and consistency advantages we discuss"
//                + "ed in detail In this chapter you general, we recommend Java Concurrency in Practice by Brian Goetz. Hi"
//                + "s book will give We’ve reached an exciting point—in the next chapter we’ll discuss bootstrapping, the"
//                + " process of configuring and connecting all of Netty’s components to bring your learned about threading "
//                + "models in general and Netty’s threading model in particular, whose performance and consistency advantag"
//                + "es we discussed in detailIn this chapter you general, we recommend Java Concurrency in Practice by Bri"
//                + "an Goetz. His book will give We’ve reached an exciting point—in the next chapter;the counter is: 1 2222"
//                + "sdsa ddasd asdsadas dsadasdas"+System.getProperty("line.separator")).getBytes();
        // 2.用小字符串来模拟多次发送消息产生的黏包现象--->用System.getProperty("line.separator")
        // this.req = ("my name is cxf "+System.getProperty("line.separator")).getBytes();

        //3.用FixedLengthFrameDecoder解决
        //this.req = ("my name is cxf").getBytes();

        //4.用DelimiterBasedFrameDecoder解决$$__
        this.req = ("In this chapter you general, we recommend Java Concurrency in $$__ Practice by Brian Goetz. His book w"
                + "ill give We’ve reached an exciting point—in the next chapter we’ll dis$$__cuss bootstrapping, the process "
                + "of configuring and connecting all of Netty’s components to bring your $$__learned about threading models in ge"
                + "neral and Netty’s threading model in particular, whose performance and$$__ consistency advantages we discuss"
                + "ed in detail In this chapter you general, we recommend Java Concurr$$__ency in Practice by Brian Goetz. Hi"
                + "s book will give We’ve reached an exciting point—in the next chap$$__ter we’ll discuss bootstrapping, the"
                + " process of configuring and connecting all of Netty’s components $$__to bring your learned about threading "
                + "models in general and Netty’s threading model in particular, whose $$__performance and consistency advantag"
                + "es we discussed in detailIn this chapter you general, we recommend Java Concur$$__rency in Practice by Bri"
                + "an Goetz. His book will give We’ve reached an exciting point—in the next ch$$__apter;the counter is: 1 2222"
                + "sdsa ddasd asdsadas dsadasdas").getBytes();

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf message = null;

        //1.注意这里的用法
        message = Unpooled.buffer(req.length);
        message.writeBytes(req);
        ctx.writeAndFlush(message);
        message = Unpooled.buffer(req.length);
        message.writeBytes(req);
        ctx.writeAndFlush(message);
//
//        for (int i = 0; i < 100; i++) {
//            message = Unpooled.buffer(req.length);
//            message.writeBytes(req);
//            ctx.writeAndFlush(message);
//        }

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        String buf = (String) msg;
        System.out.println("Now is : " + buf + " ; the counter is : " + ++count);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
