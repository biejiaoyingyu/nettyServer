package com.cxf.special.six.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class BaseServerHandler extends ChannelInboundHandlerAdapter {
    private int counter;

//原因1.单次发送的消息太多
//    server receive order : In this chapter you general, we recommend Java Concurrency in Practice by Brian Goetz. His book will give We’ve reached an exciting point—in the next chapter we’ll discuss bootstrapping, the process of configuring and connecting all of Netty’s components to bring your learned about threading models in general and Netty’s threading model in particular, whose performance and consistency advantages we discussed in detail In this chapter you general, we recommend Java Concurrency in Practice by Brian Goetz. His book will give We’ve reached an exciting point—in the next chapter we’ll discuss bootstrapping, the process of configuring and connecting all of Netty’s components to bring your learned about threading models in general and Netty’s threading model in particular, whose performance and consistency advantages we discussed in detailIn this chapter you general, we recommend Java Concurrency in Practice by Brian Goetz. His book will give We’ve reached an exciting point—in the next chapter;the counter is: 1
//    server receive order : ;the counter is: 1 2222sdsa ddasd asdsadas dsadasdasIn this chapter you general, we recommend Java Concurrency in Practice by Brian Goetz. His book will give We’ve reached an exciting point—in the next chapter we’ll discuss bootstrapping, the process of configuring and connecting all of Netty’s components to bring your learned about threading models in general and Netty’s threading model in particular, whose performance and consistency advantages we discussed in detail In this chapter you general, we recommend Java Concurrency in Practice by Brian Goetz. His book will give We’ve reached an exciting point—in the next chapter we’ll discuss bootstrapping, the process of configuring and connecting all of Netty’s components to bring your learned about threading models in general and Netty’s threading model in particular, whose performance and consistency advantages we discussed in detailIn this chapter you general, we recommend Java Concurrency in Practice by Brian Goetz. His book will give We�;the counter is: 2
//    server receive order : ��ve reached an exciting point—in the next chapter;the counter is: 1 2222sdsa ddasd asdsadas dsadasdas;the counter is: 3

    //服务端分3次接收了客户端的发送的两次的大字符串，并且有乱码现象发生，这就是拆包现象


    //原因2.多次发送少量的消息，但是服务端只接受了两次
//    server receive order : my name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy;the counter is: 1
//    server receive order :  name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxfmy name is cxf;the counter is: 2

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String body = (String)msg;
        System.out.println("server receive order : " + body + ";the counter is: " + ++counter);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }


}
