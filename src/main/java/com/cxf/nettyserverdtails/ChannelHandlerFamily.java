package com.cxf.nettyserverdtails;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

public class ChannelHandlerFamily {

    /**
     * Interface Channel 定义了一组和 ChannelInboundHandler API 密切相关的简单但功能强大的状态模型，
     * 4个状态
     * ChannelUnregistered--> Channel 已经被创建，但还未注册到 EventLoop
     * ChannelRegistered--> Channel 已经被注册到了 EventLoop
     * ChannelActive--> Channel 处于活动状态（已经连接到它的远程节点）。它现在可以接收和发送数据了
     * ChannelInactive--> Channel 没有连接到远程节点
     * <p>
     * 当这些状态发生改变时，将会生成对应的事件。这些事件将会被转发给 ChannelPipeline 中的
     * ChannelHandler，其可以随后对它们做出响应。
     */
    public void channelLife() {

    }

    /**
     * 在ChannelHandler被添加到 ChannelPipeline 中或者被从 ChannelPipeline 中移除时会调用这些操作。
     * 这些方法中的每一个都接受一个 ChannelHandlerContext 参数。
     * handlerAdded() 当把 ChannelHandler 添加到 ChannelPipeline 中时被调用
     * handlerRemoved() 当从 ChannelPipeline 中移除 ChannelHandler 时被调用
     * exceptionCaught() 当处理过程中在 ChannelPipeline 中有错误产生时被调用
     * <p>
     * Netty 定义了下面两个重要的 ChannelHandler 子接口：
     *  ChannelInboundHandler——处理入站数据以及各种状态变化；
     *  ChannelOutboundHandler——处理出站数据并且允许拦截所有的操作
     */
    public void channelHandlerLife() {

    }


    /**
     * interface ChannelInboundHandler 的生命周期方法。这些方法将会在数据被接收时或者与其对应的 Channel
     * 状态发生改变时被调用。正如我们前面所提到的，这些方法和 Channel 的生命周期密切相关。
     * <p>
     * channelRegistered               当 Channel 已经注册到它的 EventLoop 并且能够处理 I/O 时被调用
     * <p>
     * channelUnregistered             当 Channel 从它的 EventLoop 注销并且无法处理任何 I/O 时被调用
     * <p>
     * channelActive                   当 Channel 处于活动状态时被调用；Channel 已经连接/绑定并且已经就绪
     * <p>
     * channelInactive                 当 Channel 离开活动状态并且不再连接它的远程节点时被调用
     * <p>
     * channelReadComplete             当Channel上的一个读操作完成时被调用
     * <p>
     * channelRead                     当从 Channel 读取数据时被调用
     * <p>
     * ChannelWritabilityChanged       当 Channel 的可写状态发生改变时被调用。用户可以确保写操作不会完成
     *                                 得太快（以避免发生 OutOfMemoryError）或者可以在 Channel 变为再
     *                                 次可写时恢复写入。可以通过调用 Channel 的 isWritable()方法来检测
     *                                 Channel 的可写性。与可写性相关的阈值可以通过 Channel.config().
     *                                 setWriteHighWaterMark()和 Channel.config().setWriteLowWaterMark()方法来设置
     * <p>
     * userEventTriggered              当 ChannelnboundHandler.fireUserEventTriggered()方法被调
     * 用时被调用，因为一个 POJO 被传经了 ChannelPipeline
     */

    public void channelInboundHandler() {

    }

    /**
     * 扩展了 ChannelInboundHandlerAdapter
     */
    @ChannelHandler.Sharable
    public class DiscardHandler extends ChannelInboundHandlerAdapter {
        /**
         * 当某个ChannelInboundHandler的实现重写channelRead()方法时，它将负责显式地释放与池化的
         * ByteBuf 实例相关的内存。Netty 为此提供了一个实用方法 ReferenceCountUtil.release()
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            //丢弃已接收的消息
            ReferenceCountUtil.release(msg);
        }
    }


    public class SimpleDiscardHandler extends SimpleChannelInboundHandler<Object> {
        /**
         * 由于 SimpleChannelInboundHandler 会自动释放资源，所以你不应该存储指向任何消息的引用供将来使
         * 用，因为这些引用都将会失效。
         */
        @Override
        public void channelRead0(ChannelHandlerContext ctx, Object msg) {
            // No need to do anything special
        }

    }


    /**
     * 出站操作和数据将由 ChannelOutboundHandler 处理。它的方法将被 Channel、ChannelPipeline 以及
     * ChannelHandlerContext 调用。ChannelOutboundHandler 的一个强大的功能是可以按需推迟操作或者事件，
     * 这使得可以通过一些复杂的方法来处理请求。例如，如果到远程节点的写入被暂停了，那么你可以推迟冲刷操作并
     * 在稍后继续
     *
     * bind(ChannelHandlerContext,SocketAddress,ChannelPromise)    当请求将 Channel 绑定到本地地址时被调用
     * connect(ChannelHandlerContext,SocketAddress,SocketAddress,ChannelPromise)当请求将 Channel 连接到远程节点时被调用
     * disconnect(ChannelHandlerContext,ChannelPromise)当请求将 Channel 从远程节点断开时被调用close(ChannelHandlerContext,ChannelPromise) 当请求关闭 Channel 时被调用
     * deregister(ChannelHandlerContext,ChannelPromise)当请求将 Channel 从它的 EventLoop 注销时被调用
     * read(ChannelHandlerContext) 当请求从 Channel 读取更多的数据时被调用
     * flush(ChannelHandlerContext) 当请求通过 Channel 将入队数据冲刷到远程节点时被调用
     * write(ChannelHandlerContext,Object,ChannelPromise)当请求通过 Channel 将数据写到远程节点时被调用
     *
     *
     * ===========================================================================================
     *
     * ChannelOutboundHandler中的大部分方法都需要一个ChannelPromise参数，以便在操作完成时得到通知。
     * ChannelPromise是ChannelFuture的一个子类，其定义了一些可写的方法，如setSuccess()和setFailure()，
     * 从而使ChannelFuture不可变。
     */
    public void channelOutboundHandler(){

    }

    /**
     * 资源管理：
     * 为了帮助你诊断潜在的（资源泄漏）问题，Netty提供了class ResourceLeakDetector级别，它将对你应用程序的缓冲
     * 区分配做大约 1%的采样来检测内存泄露。
     * netty定义了4中泄露级别：
     * DISABLED    禁用泄漏检测。只有在详尽的测试之后才应设置为这个值
     * SIMPLE      使用 1%的默认采样率检测并报告任何发现的泄露。这是默认级别，适合绝大部分的情况
     * ADVANCED    使用默认的采样率，报告所发现的任何的泄露以及对应的消息被访问的位置
     * PARANOID    类似于 ADVANCED，但是其将会对每次（对消息的）访问都进行采样。这对性能将会有很大的影响，应该只在调试阶段使用
     *
     *  java -Dio.netty.leakDetectionLevel=ADVANCED
     *
     * 由于消费入站数据是一项常规任务，所以 Netty 提供了一个特殊的被称为 SimpleChannelInboundHandler 的
     * ChannelInboundHandler 实现。这个实现会在消息被 channelRead0()方法消费之后自动释放消息。
     *
     *
     * 如果一个消息被消费或者丢弃了，并且没有传递给 ChannelPipeline 中的下一个ChannelOutboundHandler，那么用户就有责任调用
     * ReferenceCountUtil.release()。如果消息到达了实际的传输层，那么当它被写入时或者 Channel 关闭时，都将被自动释放。
     */
    @ChannelHandler.Sharable
    public class DiscardInboundHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            //通过调用 ReferenceCountUtil.release()释放资源
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 每一个新创建的 Channel 都将会被分配一个新的 ChannelPipeline。这项关联是永久性的；Channel 既不能附加另外一个
     * ChannelPipeline，也不能分离其当前的。在 Netty 组件的生命周期中，这是一项固定的操作，不需要开发人员的任何干预。
     *
     * 根据事件的起源，事件将会被 ChannelInboundHandler 或者 ChannelOutboundHandler处理。随后，通过调用
     * ChannelHandlerContext 实现，它将被转发给同一超类型的下一个ChannelHandler。
     *
     * ChannelHandlerContext使得ChannelHandler能够和它的ChannelPipeline以及其他的ChannelHandler 交互。
     * ChannelHandler 可以通知其所属的 ChannelPipeline 中的下一 个ChannelHandler，甚至可以动态修改它所属
     * 的ChannelPipeline(这里指的是channelHandler的编排)。ChannelHandlerContext 具有丰富的用于处理事件和
     * 执行 I/O 操作的 API。
     *
     * 在 ChannelPipeline 传播事件时，它会测试 ChannelPipeline 中的下一个 ChannelHandler 的类型是否和事
     * 件的运动方向相匹配。如果不匹配，ChannelPipeline 将跳过该ChannelHandler 并前进到下一个，直到它找到和该
     * 事件所期望的方向相匹配的为止。
     *
     * ===================================================================================================
     * ChannelHandler 可以通过添加、删除或者替换其他的 ChannelHandler 来实时地修改ChannelPipeline 的布局。（它也可以
     * 将它自己从 ChannelPipeline 中移除。）这是 ChannelHandler 最重要的能力之一，
     * AddFirstaddBefore/addAfteraddLast将一个ChannelHandler 添加到ChannelPipeline 中
     * remove 将一个ChannelHandler 从ChannelPipeline 中移除
     * replace 将 ChannelPipeline 中的一个 ChannelHandler 替换为另一个 ChannelHandler
     *
     *
     */
    public void channelPipeline(){

        ChannelPipeline pipeline = null;
        //创建一个 FirstHandler 的实例
        FirstHandler firstHandler = new FirstHandler();
        //将该实例作为"handler1"添加到ChannelPipeline 中
        pipeline.addLast("handler1", firstHandler);
        //将一个 SecondHandler的实例作为"handler2" 添加到 ChannelPipeline 的第一个槽中。
        // 这意 味着它将被放置在已 有的"handler1"之前
        pipeline.addFirst("handler2", new SecondHandler());
        //将一个 ThirdHandler 的实例作为"handler3"添加到 ChannelPipeline 的最后一个槽中
        pipeline.addLast("handler3", new ThirdHandler());
        //通过名称移除"handler3
        pipeline.remove("handler3");
        //通过引用移除FirstHandler（它是唯一的，所以不需要它的名称）
        pipeline.remove(firstHandler);
        //将 SecondHandler("handler2")替换为 FourthHandler:"handler4"
        pipeline.replace("handler2", "handler4", new ForthHandler());

    }

    /**
     * ChannelHandlerContext 代表了ChannelHandler和ChannelPipeline之间的关联，每当有ChannelHandler
     * 添加到 ChannelPipeline 中时，都会创建 ChannelHandlerContext。ChannelHandlerContext 的主要功能
     * 是管理它所关联的 ChannelHandler 和在同一个ChannelPipeline 中的其他 ChannelHandler 之间的交互。
     *
     * ChannelHandlerContext 有很多的方法，其中一些方法也存在于Channel和ChannelPipeline本身上，但是有
     * 一点重要的不同。如果调用Channel或者ChannelPipeline上的这些方法，它们将沿着整个 ChannelPipeline
     * 进行传播。而调用位于 ChannelHandlerContext上的相同方法，则将从当前所关联的 ChannelHandler 开始，
     * 并且只会传播给位于该ChannelPipeline 中的下一个能够处理该事件的 ChannelHandler
     *
     * a.ChannelHandlerContext 和 ChannelHandler 之间的关联（绑定）是永远不会改变的，所以缓存对它的引
     *   用是安全的；
     * b.如同我们在本节开头所解释的一样，相对于其他类的同名方法，ChannelHandlerContext的方法将产生更短
     *   的事件流，应该尽可能地利用这个特性来获得最大的性能。
     *
     */
    public void channelHandlerContext(){

        /**
         * 重要的是要注意到，虽然被调用的 Channel 或 ChannelPipeline 上的 write()方法将一直传播事件
         * 通过整个 ChannelPipeline，但是在 ChannelHandler 的级别上，事件从一个 ChannelHandler到下
         * 一个ChannelHandler 的移动是由 ChannelHandlerContext 上的调用完成的。
         */

        ChannelHandlerContext ctx = null;
        //获取到与 ChannelHandlerContext相关联的 Channel 的引用
        Channel channel = ctx.channel();
        //通过 Channel 写入缓冲区
        channel.write(Unpooled.copiedBuffer("Netty in Action", CharsetUtil.UTF_8));


        ChannelHandlerContext ctx1 = null;
        //获取到与 ChannelHandlerContext相关联的 ChannelPipeline 的引用
        ChannelPipeline pipeline = ctx1.pipeline();
        //通过 ChannelPipeline写入缓冲区
        pipeline.write(Unpooled.copiedBuffer("Netty in Action", CharsetUtil.UTF_8));

        /**
         * 为什么会想要从 ChannelPipeline 中的某个特定点开始传播事件呢？
         *  为了减少将事件传经对它不感兴趣的 ChannelHandler 所带来的开销。
         *  为了避免将事件传经那些可能会对它感兴趣的 ChannelHandler。
         */
        ChannelHandlerContext ctx2 = null;
        //write()方法将把缓冲区数据发送到下一个 ChannelHandler
        //消息将从下一个 ChannelHandler 开始流经ChannelPipeline，绕过了所有前面的 ChannelHandler。
        //会一直执行后面所有的channelhandler还是只执行一个？
        ctx2.write(Unpooled.copiedBuffer("Netty in Action", CharsetUtil.UTF_8));

    }

    /**
     * ChannelHandler 和 ChannelHandlerContext 的高级用法
     * a.你可以通过将 ChannelHandler 添加到 ChannelPipeline 中来实现动态的协议切换。
     * b.另一种高级的用法是缓存到 ChannelHandlerContext 的引用以供稍后使用，这可能会发生在任何的 ChannelHandler
     *   方法之外，甚至来自于不同的线程。
     */
    public class WriteHandler extends ChannelHandlerAdapter {
        private ChannelHandlerContext ctx;

        /**
         * 添加handler的时候触发
         */
        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            //存储到 ChannelHandlerContext的引用以供稍后使用
            this.ctx = ctx;
        }

        /**
         * 使用之前存储的到 ChannelHandlerContext的引用来发送消息
         */
        public void send(String msg) {
            ctx.writeAndFlush(msg);
        }
    }

    /**
     * 因为一个 ChannelHandler 可以从属于多个 ChannelPipeline，所以它也可以绑定到多个
     * ChannelHandlerContext 实例。对于这种用法指在多个 ChannelPipeline 中共享同一个
     * ChannelHandler，对应的 ChannelHandler 必须要使用@Sharable 注解标注；否则，试图
     * 将它添加到多个 ChannelPipeline 时将会触发异常。显而易见，为了安全地被用于多个并发
     * 的 Channel（即连接），这样的 ChannelHandler 必须是线程安全的
     *
     *
     * 为何要共享同一个ChannelHandler 在多个ChannelPipeline中安装同一个ChannelHandler
     * 的一个常见的原因是用于收集跨越多个 Channel 的统计信息。
     */
    @ChannelHandler.Sharable
    public class SharableHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            System.out.println("Channel read message: " + msg);
            //记录方法调用，并转发给下一个 ChannelHandler
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * 异常处理 （入站）
     * 如果在处理入站事件的过程中有异常被抛出，那么它将从它在 ChannelInboundHandler里被触发的那一点开始
     * 流经 ChannelPipeline。要想处理这种类型的入站异常，你需要在你的 ChannelInboundHandler 实现中重
     * 写下面的方法：
     *
     * 因为异常将会继续按照入站方向流动（就像所有的入站事件一样），所以实现了前面所示逻辑的ChannelInboundHandler
     * 通常位于 ChannelPipeline 的最后。这确保了所有的入站异常都总是会被处理，无论它们可能会发生在
     * ChannelPipeline 中的什么位置。
     *
     * 异常信息会被正常的传递么？？？
     *
     *  ChannelHandler.exceptionCaught()的默认实现是简单地将当前异常转发给ChannelPipeline 中的下一个 ChannelHandler；
     *  如果异常到达了 ChannelPipeline 的尾端，它将会被记录为未被处理；
     *  要想定义自定义的处理逻辑，你需要重写 exceptionCaught()方法。然后你需要决定是否需要将该异常传播出去。
     *
     */
    public class InboundExceptionHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }


    /**
     * 异常处理：（出站）
     * 每个出站操作都将返回一个 ChannelFuture。注册到 ChannelFuture 的 ChannelFutureListener 将在操作完成时被通
     * 知该操作是成功了还是出错了。
     *
     */
    public void outException(){
        Channel channel = null;
        String someMessage = "hahh";
        ChannelFuture future = channel.write(someMessage);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture f) {
                if (!f.isSuccess()) {
                    f.cause().printStackTrace();
                    f.channel().close();
                }
            }
        });
    }

    /**
     * 另一种处理出站异常的方式
     * 几乎所有的 ChannelOutboundHandler 上的方法都会传入一个 ChannelPromise的实例。作为 ChannelFuture 的子类，
     * ChannelPromise 也可以被分配用于异步通知的监听器。但是，ChannelPromise 还具有提供立即通知的可写方法：
     *    ChannelPromise setSuccess();
     *    ChannelPromise setFailure(Throwable cause);
     * 第二种方式是将 ChannelFutureListener添加到即将作为参数传递给ChannelOutboundHandler的方法的ChannelPromise。
     * 通过调用 ChannelPromise 上的 setSuccess()和 setFailure()方法，可以使一个操作的状态在 ChannelHandler 的方法
     * 返回给其调用者时便即刻被感知到。
     */
    public class OutboundExceptionHandler extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            promise.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) {
                    if (!f.isSuccess()) {
                        f.cause().printStackTrace();
                        f.channel().close();
                    }
                }
            });
        }
    }


}
