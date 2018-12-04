package com.cxf.nettyserverdtails;

import io.netty.channel.Channel;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class EventLoopDetails {


    /**
     *
     * 一个 EventLoop 将由一个永远都不会改变的 Thread 驱动，同时任务（Runnable 或者 Callable）可以直接提交给
     * EventLoop 实现，以立即执行或者调度执行。根据配置和可用核心的不同，可能会创建多个 EventLoop 实例用以优化
     * 资源的使用，并且单个EventLoop 可能会被指派用于服务多个 Channel。需要注意的是，Netty的EventLoop在继承了
     * ScheduledExecutorService的同时，只定义了一个方法，parent()
     *
     * public interface EventLoop extends EventExecutor, EventLoopGroup {
     *       @Override
     *      EventLoopGroup parent();
     *  }
     */
    public void eventPre(){
        //jdk定时调用,虽然 ScheduledExecutorService API 是直截了当的，但是在高负载下它将带来性能上的负担。
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
        ScheduledFuture<?> future = executor.schedule(()-> System.out.println("60 seconds later"),
                60, TimeUnit.SECONDS);
        //旦调度任务执行完成，就关闭ScheduledExecutorService 以释放资源
        executor.shutdown();


        //如果有大量任务被紧凑地调度，那么这将成为一个瓶颈。Netty 通过 Channel 的 EventLoop 实现任务调度解决了这
        // 一问题
        Channel ch =null;
        ScheduledFuture<?> future1 = ch.eventLoop().schedule(()-> System.out.println("60 seconds later"),
                60, TimeUnit.SECONDS);


        //调度在 60 秒之后，并且以后每间隔 60 秒运行
        Channel ch0 = null;
        ScheduledFuture<?> future2 = ch.eventLoop().scheduleAtFixedRate(()-> System.out.println("60 seconds later"),
                60, 60, TimeUnit.SECONDS);


        //要想取消或者检查（被调度任务的）执行状态，可以使用每个异步操作所返回的 ScheduledFuture。
        ScheduledFuture<?> future3 = ch.eventLoop().scheduleAtFixedRate(()-> System.out.println("60 seconds later"),
                60, 60, TimeUnit.SECONDS);
        // Some other code that runs...
        boolean mayInterruptIfRunning = false;
        //取消该任务，防止它再次运行
        future.cancel(mayInterruptIfRunning);

    }

    /**
     * Netty线程模型的卓越性能取决于对于当前执行的Thread的身份的确定也就是说，确定它是否是分配给当
     * 前Channel以及它的EventLoop的那一个线程。
     *
     * 如果（当前）调用线程正是支撑 EventLoop 的线程，那么所提交的代码块将会被（直接）执行。否则，
     * EventLoop 将调度该任务以便稍后执行，并将它放入到内部队列中。当 EventLoop下次处理它的事件时，
     * 它会执行队列中的那些任务/事件。这也就解释了任何的 Thread 是如何与 Channel 直接交互而无需在
     * ChannelHandler 中进行额外同步的。
     *
     * ==================================================================================
     * EventLoopGroup 负责为每个新创建的 Channel 分配一个 EventLoop。在当前实现中，使用顺序循环
     * （round-robin）的方式进行分配以获取一个均衡的分布，并且相同的 EventLoop可能会被分配给多个
     * Channel。（这一点在将来的版本中可能会改变。）一旦一个Channel 被分配给一个EventLoop，它将在
     * 它的整个生命周期中都使用这个EventLoop（以及相关联的Thread）。请牢记这一点，因为它可以使你从
     * 担忧你的ChannelHandler 实现中的线程安全和同步问题中解脱出来。另外，需要注意的是，EventLoop
     * 的分配方式对 ThreadLocal 的使用的影响。因为一个EventLoop 通常会被用于支撑多个 Channel，所
     * 以对于所有相关联的 Channel 来说，ThreadLocal 都将是一样的。这使得它对于实现状态追踪等功能来
     * 说是个糟糕的选择。然而，在一些无状态的上下文中，它仍然可以被用于在多个 Channel 之间共享一些重
     * 度的或者代价昂贵的对象，甚至是事件。
     */
    public void threadOperate(){

    }




}
