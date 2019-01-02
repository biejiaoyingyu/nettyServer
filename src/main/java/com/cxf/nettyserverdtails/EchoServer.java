package com.cxf.nettyserverdtails;

import com.cxf.nettyserver.EchoServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class EchoServer {
    private Channel channel;

    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.err.println("Usage: " + EchoServer.class.getSimpleName() + " <port>");
        }
        //设置端口值（如果端口参数的格式不正确，则抛出一个NumberFormatException）
        int port = Integer.parseInt(args[0]);
        //调用服务器的 start()方法
        new EchoServer(port).start();
    }

    public void start() throws Exception {

        final EchoServerHandler serverHandler = new EchoServerHandler();
        //创建EventLoopGroup
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            //创建ServerBootstrap
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class)//指定所使用的 NIO 传输 Channel
                    .localAddress(new InetSocketAddress(port))//使用指定的端口设置套接字地址
                    .childHandler(new ChannelInitializer<SocketChannel>()//添加一个EchoServerHandler 到子Channel的ChannelPipeline
                    {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            /**
                             * 拿到channel引用，但是每个链接进来都会创建一个channel的，这一点需要明确
                             */
                            channel = ch;
                            ch.pipeline().addLast(serverHandler);//EchoServerHandler被标注为@Shareable，所以我们可以总是使用同样的实例
                        }
                    });

            ChannelFuture f = b.bind().sync();//异步地绑定服务器；调用 sync()方法阻塞 等待直到绑定完成
            f.channel().closeFuture().sync();//获取 Channel的CloseFuture，并且阻塞当前线程直到它完成
        } finally {
            group.shutdownGracefully().sync();//关闭 EventLoopGroup释放资源
        }
    }

    /**
     * 拿到channel引用，操作写数据并将其冲刷到远程节点这样的常规任务
     */
    public void writeAndFlush() {
        //创建持有要写数据的 ByteBuf
        ByteBuf buf = Unpooled.copiedBuffer("your data", CharsetUtil.UTF_8);
        //写数据并冲刷它
        ChannelFuture cf = channel.writeAndFlush(buf);
        //添加 ChannelFutureListener 以便在写操作完成后接收通知
        cf.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                //写操作完成，并且没有错误发生
                if (future.isSuccess()) {
                    System.out.println("Write successful");
                } else {
                    //记录错误
                    System.err.println("Write error");
                    future.cause().printStackTrace();
                }
            }
        });
    }

    /**
     * channel是线程安全的Netty 的 Channel 实现是线程安全的，因此你可以存储一个到 Channel 的引用，
     * 并且每当你需要向远程节点写数据时，都可以使用它，即使当时许多线程都在使用它。
     */
    public void threadSafeChannel() {
        final Channel channel0 = channel;
        //创建持有要写数据的ByteBuf
        final ByteBuf buf = Unpooled.copiedBuffer("your data", CharsetUtil.UTF_8).retain();
        Runnable writer = new Runnable() {
            @Override
            public void run() {
                //创建将数据写到Channel的Runnable
                channel0.writeAndFlush(buf.duplicate());
            }
        };
        //获取到线程池Executor 的引用
        Executor executor = Executors.newCachedThreadPool();
        // write in one thread
        executor.execute(writer);
        // write in another thread
        executor.execute(writer);

    }


    /**
     * 堆缓冲区：支撑数组，它能在没有使用池化的情况下提供快速的分配和释放
     * 特点是内存的分配和回收速度快，可以被JVM自动回收；缺点就是如果进行Socket的I/O读写，
     * 需要额外做一次内存复制，将堆内存对应的缓冲区复制到内存Channel中，性能会有一定程度的下降；
     */
    public void byteBufHeap() {
        //怎么创建ByteBuf
        ByteBuf heapBuf = null;
        //检查是否有一个支撑数组，当 hasArray()方法返回 false 时，尝试访问支撑数组将触发一个 UnsupportedOperationException。
        if (heapBuf.hasArray()) {
            //如果有，则获取对该数组的引用
            byte[] array = heapBuf.array();
            //计算第一个字节的偏移量
            int offset = heapBuf.arrayOffset() + heapBuf.readerIndex();
            //获得可读字节数
            int length = heapBuf.readableBytes();
            //使用数组、偏移量和长度作为参数调用你的方法
            handleArray(array, offset, length);
        }
    }

    private void handleArray(byte[] array, int offset, int length) {
    }


    /**
     * 直接缓冲区：直接缓冲区的内容将驻留在常规的会被垃圾回收的堆之外。这也就解释了为何直
     * 接缓冲区对于网络数据传输是理想的选择。如果你的数据包含在一个在堆上分配的缓冲区中，
     * （这里说的是堆缓冲区）那么事实上，在通过套接字发送它之前，JVM将会在内部把你的缓冲
     * 区复制到一个直接缓冲区中。
     * <p>
     * 相对于基于堆的缓冲区，它们的分配和释放都较为昂贵。如果你正在处理遗留代码（注意是遗
     * 留代码），你也可能会遇到另外一个缺点：因为数据不是在堆上，所以你不得不进行一次复制。
     * <p>
     * 最佳实践：在I/O通信线程的读写缓冲区使用DirectByteBuf，后端业务消息的编解码模块使
     * 用HeapByteBuf。
     */
    public void byteBufDirect() {
        ByteBuf directBuf = null;
        //检查 ByteBuf 是否由数组支撑。如果不是，则这是一个直接缓冲区
        if (!directBuf.hasArray()) {
            //获取可读字节数
            int length = directBuf.readableBytes();
            //分配一个新的数组来保存具有该长度的字节数据
            byte[] array = new byte[length];
            //将字节复制到该数组
            directBuf.getBytes(directBuf.readerIndex(), array);
            //使用数组、偏移量和长度作为参数调用你的方法
            handleArray(array, 0, length);
        }
    }

    /**
     * 复合缓冲区：Netty 通过一个 ByteBuf 子类——CompositeByteBuf——实现了这个模式，它提、
     * 供了一个将多个缓冲区表示为单个合并缓冲区的虚拟表示。
     * <p>
     * 注意：
     * CompositeByteBuf 中的 ByteBuf实例可能同时包含直接内存分配和非直接内存分配。如果其
     * 中只有一个实例，那么对CompositeByteBuf 上的 hasArray()方法的调用将返回该组件上的
     * hasArray()方法的值；否则它将返回 false。
     */
    public void byteBufComposite() {
        CompositeByteBuf messageBuf = Unpooled.compositeBuffer();
        ByteBuf headerBuf = null; // can be backing or direct
        ByteBuf bodyBuf = null; // can be backing or direct
        //将 ByteBuf 实例追加到 CompositeByteBuf
        messageBuf.addComponents(headerBuf, bodyBuf);
        //删除位于索引位置为 0（第一个组件）的 ByteBuf
        messageBuf.removeComponent(0); // remove the header
        //循环遍历所有的 ByteBuf 实例
        for (ByteBuf buf : messageBuf) {
            System.out.println(buf.toString());
        }

        /**
         * Netty使用了CompositeByteBuf来优化套接字的I/O操作，尽可能地消除了由JDK的缓冲区实现所
         * 导致的性能以及内存使用率的惩罚这种优化发生在Netty的核心代码中,因此不会被暴露出来，但是
         * 你应该知道它所带来的影响。
         */
        //CompositeByteBuf 可能不支持访问其支撑数组，因此访问 CompositeByteBuf 中的数据类似于（访问）直接缓冲区的模式
        CompositeByteBuf compBuf = Unpooled.compositeBuffer();
        //获得可读取的字节数
        int length = compBuf.readableBytes();
        //分配一个具有可读字节数长度的新数组
        byte[] array = new byte[length];
        //将字节读到该数组中
        compBuf.getBytes(compBuf.readerIndex(), array);
        //使用偏移量和长度作为参数使用该数组
        handleArray(array, 0, array.length);


    }


    /**
     * 随机访问索引
     * 如同在普通的 Java 字节数组中一样，ByteBuf的索引是从零开始的：第一个字节的索引是0，最后一
     * 个字节的索引总是 capacity() - 1。对存储机制的封装使得遍历 ByteBuf 的内容非常简单。
     * 使用那些需要一个索引值参数的方法（的其中）之一来访问数据既不会改变readerIndex 也不会改变
     * writerIndex。如果有需要，也可以通过调用 readerIndex(index)或者 writerIndex(index)来
     * 手动移动这两者
     * <p>
     * 可丢弃字节的分段包含了已经被读过的字节。通过调用discardReadBytes()方法，可以丢弃它们并
     * 回收空间。这个分段的初始大小为0，存储在readerIndex中，会随着read 操作的执行而增加（get*
     * 操作不会移动 readerIndex）。
     * <p>
     * 在缓冲区上调用discardReadBytes()方法后的结果。可丢弃字节分段中的空间已经变为可写的了。
     * 注意，在调用discardReadBytes()之后，对可写分段的内容并没有任何的保证。因为只是移动了可
     * 以读取的字节以及 writerIndex，而没有对所有可写入的字节进行擦除写
     * <p>
     * 虽然你可能会倾向于频繁地调用 discardReadBytes()方法以确保可写分段的最大化，但是请注意，
     * 这将极有可能会导致内存复制，因为可读字节必须被移动到缓冲区的开始位置。我们建议只在有真正需
     * 要的时候才这样做，例如，当内存非常宝贵的时候。
     */
    public void operateIndex() {
        ByteBuf buffer = null;
        for (int i = 0; i < buffer.capacity(); i++) {
            byte b = buffer.getByte(i);
            System.out.println((char) b);
        }

    }


    /**
     * 可读字节
     * 新分配的、包装的或者复制的缓冲区的默认的readerIndex 值为 0。任何名称以 read 或者 skip 开头的操
     * 作都将检索或者跳过位于当前readerIndex 的数据，并且将它增加已读字节数。
     * 可写字节
     * 新分配的缓冲区的writerIndex 的默认值为 0。任何名称以 write 开头的操作都将从当前的 writerIndex
     * 处开始写数据，并将它增加已经写入的字节数。
     *
     *
     */
    public void readWriteData(){
        ByteBuf buffer = null;
        while (buffer.isReadable()) {
            System.out.println(buffer.readByte());
        }
        // Fills the writable bytes of a buffer with random integers.
        // 如果尝试往目标写入超过目标容量的数据，将会引发一个IndexOutOfBoundException
        ByteBuf buffer0 = null;
        while (buffer.writableBytes() >= 4) {
            //buffer.writeInt(random.nextInt());
        }

    }

    /**
     * 可以通过调用 markReaderIndex()、markWriterIndex()、resetWriterIndex()和 resetReaderIndex()来标
     * 记和重置 ByteBuf 的 readerIndex 和 writerIndex。
     *
     * 也可以通过调用 readerIndex(int)或者 writerIndex(int)来将索引移动到指定位置。图将任何一个索引设置到一
     * 个无效的位置都将导致一个 IndexOutOfBoundsException。
     *
     * 可以通过调用 clear()方法来将 readerIndex 和 writerIndex 都设置为 0。注意，这并不会清除内存中的内容
     * 调用 clear()比调用 discardReadBytes()轻量得多，因为它将只是重置索引而不会复制任何的内存。然而
     * discardReadBytes()会移动数据，清除已经读的数据。
     */
    public void indexOperate(){
         return;
    }


    /**
     * 派生缓冲区派为 ByteBuf 提供了以专门的方式来呈现其内容的视图这类视图是通过以下方法被创建的：
     * duplicate()
     * slice()
     * slice(int, int)
     * Unpooled.unmodifiableBuffer()
     * order(ByteOrder)
     * readSlice(int)
     *
     * 每个这些方法都将返回一个新的 ByteBuf 实例，它具有自己的读索引、写索引和标记索引。其内部存储和 JDK 的
     * ByteBuffer 一样也是共享的。这使得派生缓冲区的创建成本是很低廉的，但是这也意味着，如果你修改了它的内容，
     * 也同时修改了其对应的源实例，所以要小心
     *
     * 如果需要一个现有缓冲区的真实副本，请使用 copy()或者 copy(int, int)方法。不同于派生缓冲区，由这个调用
     * 所返回的 ByteBuf 拥有独立的数据副本。
     *
     */
    public void psBuf(){

        Charset utf8 = Charset.forName("UTF-8");
        //创建一个用于保存给定字符串的字节的 ByteBuf
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        //创建该 ByteBuf 从索引 0 开始到索引 15结束的一个新切片
        ByteBuf sliced = buf.slice(0, 15);
        //将打印“Netty in Action”
        System.out.println(sliced.toString(utf8));
        //更新索引 0 处的字节
        buf.setByte(0, (byte)'J');
        //将会成功，因为数据是共享的，对其中一个所做的更改对另外一个也是可见的
        assert buf.getByte(0) == sliced.getByte(0);

        Charset utf_8 = Charset.forName("UTF-8");
        ByteBuf buf_1 = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        ByteBuf copy = buf.copy(0, 15);
        System.out.println(copy.toString(utf_8));
        buf_1.setByte(0, (byte) 'J');
        //将会成功，因为数据不是共享的
        assert buf_1.getByte(0) != copy.getByte(0);

    }

    /**
     * 不同的get()和set()方法对应索引值的容量大小不一致啊？===>这是个问题啊？
     * get()和 set()操作，从给定的索引开始，并且保持索引不变；
     * read()和 write()操作，从给定的索引开始，并且会根据已经访问过的字节数对索引进行调整。
     */
    public void getAndSet(){
        Charset utf8 = Charset.forName("UTF-8");
        //创建一个新的 ByteBuf以保存给定字符串的字节
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        //打印第一个字符'N'
        System.out.println((char)buf.getByte(0));
        //存储当前的 readerIndex和 writerIndex
        int readerIndex = buf.readerIndex();
        int writerIndex = buf.writerIndex();
        //将索引 0 处的字节更新为字符'B'
        buf.setByte(0, (byte)'B');
        //打印第一个字符，现在是'B'
        System.out.println((char)buf.getByte(0));
        //将会成功，因为这些操作并不会修改相应的索引
        assert readerIndex == buf.readerIndex();
        assert writerIndex == buf.writerIndex();

    }

    /**
     *
     * read()和 write()操作，从给定的索引开始，并且会根据已经访问过的字节数对索引进行调整。
     */
    public void readAndWrite(){
        Charset utf8 = Charset.forName("UTF-8");
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        System.out.println((char)buf.readByte());
        int readerIndex = buf.readerIndex();
        int writerIndex = buf.writerIndex();
        //将字符'?'追加 writerIndex到缓冲区
        buf.writeByte((byte)'?');
        //将会成功，因为 writeByte()方法移动了 writerIndex
        assert readerIndex == buf.readerIndex();
        assert writerIndex != buf.writerIndex();
    }

    /**
     * Netty 提供了 ByteBufHolder。ByteBufHolder 也为 Netty 的高级特性提供了支持，如缓冲区
     * 池化，其中可以从池中借用 ByteBuf，并且在需要时自动释放
     *
     * content() 返回由这个 ByteBufHolder 所持有的 ByteBuf
     * copy() 返回这个 ByteBufHolder 的一个深拷贝，包括一个其所包含的 ByteBuf 的非共享拷贝
     * duplicate() 返回这个 ByteBufHolder 的一个浅拷贝，包括一个其所包含的 ByteBuf 的共享拷贝
     */
    public  void byteBufHolder(){

    }


    /**
     * 按需分配：ByteBufAllocator 接口(-->api都有限制容量的版本)
     * 为了降低分配和释放内存的开销，Netty 通过 interface ByteBufAllocator 实现了（ByteBuf 的）
     * 池化，它可以用来分配我们所描述过的任意类型的 ByteBuf 实例。使用池化是特定于应用程序的决定，
     * 其并不会以任何方式改变 ByteBuf API
     * buffer()返回一个基于堆或者直接内存存储的 ByteBuf
     * heapBuffer()返回一个基于堆内存存储的ByteBuf
     * directBuffer()返回一个基于直接内存存储的ByteBuf
     * compositeBuffer()返回一个可以通过添加最大到指定数目的基于堆的或者直接内存存储的缓冲区来扩展
     * 的CompositeByteBuf
     *
     * ==================================================================================
     *
     * 可以通过 Channel（每个都可以有一个不同的 ByteBufAllocator 实例）或者绑定到ChannelHandler
     * 的 ChannelHandlerContext 获取一个到 ByteBufAllocator 的引用。
     *
     * ==================================================================================
     * Netty有两种ByteBufAllocator的实现：PooledByteBufAllocator和UnpooledByteBufAllocator。
     * 前者池化了ByteBuf的实例以提高性能并最大限度地减少内存碎片。此实现使用了一种称为jemalloc的已
     * 被大量现代操作系统所采用的高效方法来分配内存。后者的实现不池化ByteBuf实例，并且在每次它被调用
     * 时都会返回一个新的实例。
     *
     * 虽然Netty默认使用了PooledByteBufAllocator，但这可以很容易地通过ChannelConfig API或者在引
     * 导你的应用程序时指定一个不同的分配器来更改。
     *
     * ==================================================================================
     * 可能某些情况下，你未能获取一个到 ByteBufAllocator的引用。对于这种情况，Netty提供了一个简单
     * 的称为 Unpooled 的工具类，它提供了静态的辅助方法来创建未池化的 ByteBuf实例。
     */
    public void byteBufAllocator(){

        Channel channel = null;
        //从 Channel 获取一个到ByteBufAllocator 的引用
        ByteBufAllocator allocator = channel.alloc();

        ChannelHandlerContext ctx = null;
        //从 ChannelHandlerContext 获取一个到 ByteBufAllocator 的引用
        ByteBufAllocator allocator2 = ctx.alloc();

    }


    /**
     * 引用计数是一种通过在某个对象所持有的资源不再被其他对象引用时释放该对象所持有的资源来优化内存使用和性能
     * 的技术。Netty 在第 4 版中为 ByteBuf 和 ByteBufHolder 引入了引用计数技术，它们都实现了 interface
     * ReferenceCounted。引用计数背后的想法并不是特别的复杂；它主要涉及跟踪到某个特定对象的活动引用的数量。
     * 一个 ReferenceCounted 实现的实例将通常以活动的引用计数为 1 作为开始。只要引用计数大于 0，就能保证对
     * 象不会被释放。当活动引用的数量减少到0时，该实例就会被释放。注意，虽然释放的确切语义可能是特定于实现的，
     * 但是至少已经释放的对象应该不可再用了。引用计数对于池化实现（如 PooledByteBufAllocator）来说是至关重
     * 要的，它降低了内存分配的开销。
     */
    public void referenceCount(){
        Channel channel = null;
        // 从 Channel 获取ByteBufAllocator
        ByteBufAllocator allocator = channel.alloc();
        //从 ByteBufAllocator分配一个 ByteBuf
        ByteBuf buffer = allocator.directBuffer();
        //检查引用计数是否为预期的 1
        assert buffer.refCnt() == 1;


        //减少到该对象的活动引用。当减少到 0 时，该对象被释放，并且该方法返回 true
        //试图访问一个已经被释放的引用计数的对象，将会导致一个 IllegalReferenceCountException
        //一般来说，是由最后访问（引用计数）对象的那一方来负责将它释放
        boolean released = buffer.release();

    }
}
