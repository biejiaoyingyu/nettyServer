package com.cxf.special.specialheartbeat;

import io.netty.channel.ChannelHandler;

/**
 * Created by cxf on 2019/1/2.
 */
public interface ChannelHandlerHolder {

    ChannelHandler[] handlers();
}
