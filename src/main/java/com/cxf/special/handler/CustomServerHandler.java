package com.cxf.special.handler;

import com.cxf.special.bin.CustomMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.Charset;

/**
 * Created by cxf on 2019/1/2.
 */
public class CustomServerHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof CustomMsg) {
            CustomMsg customMsg = (CustomMsg)msg;
            System.out.println("Client->Server:"+ctx.channel().remoteAddress()+" send "+customMsg.getBody());
        }

        //如果不重LengthFieldBasedFrameDecoder的decode方法,那么正常情况下这个decode方法会返回ByteBuf，所以后面的handler需要去处理这个ByteBuf
        //而且要注意LengthFieldBasedFrameDecoder构造函数的各个参数的计算，个人感觉比较容易出错。
        if(msg instanceof ByteBuf){
            int len = ((ByteBuf) msg).readableBytes();

            byte[] bt = new byte[len];
            ((ByteBuf) msg).readBytes(bt);
            String str = new String (bt, Charset.forName("UTF-8"));
            System.out.println(str);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
