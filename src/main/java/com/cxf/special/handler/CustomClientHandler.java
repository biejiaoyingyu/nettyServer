package com.cxf.special.handler;

import com.cxf.special.bin.CustomMsg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by cxf on 2019/1/2.
 */
public class CustomClientHandler  extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        CustomMsg customMsg = new CustomMsg( "Hello,Netty");
        ctx.writeAndFlush(customMsg);
    }
}
