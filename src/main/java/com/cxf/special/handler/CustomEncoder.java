package com.cxf.special.handler;

import com.cxf.special.bin.CustomMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.Charset;

/**
 * Created by cxf on 2019/1/2.
 */
public class CustomEncoder extends MessageToByteEncoder<CustomMsg> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, CustomMsg msg, ByteBuf out) throws Exception {
        if(msg == null) throw new Exception("msg is null");
        String body = msg.getBody();
        out.writeByte((byte) 1);
        out.writeInt(body.length());
        out.writeByte((byte)0);
        out.writeBytes(body.getBytes(Charset.forName("utf-8")));
    }
}
