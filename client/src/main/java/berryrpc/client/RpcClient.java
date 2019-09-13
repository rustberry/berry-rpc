package berryrpc.client;

import berryrpc.common.RpcRequest;
import berryrpc.common.RpcResponse;
import berryrpc.common.codec.RpcDecoder;
import berryrpc.common.codec.RpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {
    private String serviceAddress;
    private RpcResponse response;

    private ChannelHandlerContext ctx;
    private EventLoopGroup group;
    private Channel channel;

    private volatile boolean inConnection = false;
    private final CountDownLatch respReceived = new CountDownLatch(1);

    public RpcClient(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    private void ensureConnected() {
        if (inConnection) return;

        this.group = new NioEventLoopGroup();
        final RpcEncoder encoder = new RpcEncoder();

        synchronousConnect(encoder);

        // When channel is being shutdown, ensure clean shutdown after the channel down.
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                shutDownConnection();
            }
        });
    }

    /**
     * Blocks until connected.
     */
    private synchronized void synchronousConnect(ChannelHandler... handlers) {
        if (inConnection) return;  // Ensure the latest value is read.

        String[] remoteAddress = this.serviceAddress.split(":");
        String serverHost = remoteAddress[0];
        int port = Integer.parseInt(remoteAddress[1]);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .remoteAddress(serverHost, port)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(handlers)
                                .addLast(new RpcDecoder(RpcResponse.class))
                                .addLast(RpcClient.this);
                    }
                });
        ChannelFuture future = bootstrap.connect().syncUninterruptibly();
        inConnection = true;
        this.channel = future.channel();
    }

    public RpcResponse send(RpcRequest request) {
        ensureConnected();
        synchSend(request);
        try {
            respReceived.await();
        } catch (InterruptedException e) {
            log.error("", e);
        }
        return this.response;
    }

    private void synchSend(RpcRequest request) {
        channel.writeAndFlush(request).syncUninterruptibly();
    }

    /**
     * Shuts dow the connection with service provider server.
     */
    public void shutDownConnection() {
        group.shutdownGracefully().syncUninterruptibly();
        log.debug("See if channel#close() operation had finished, channel: " + channel);
    }

    // SimpleInboundHandler auto releases byte buffer.
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse msg) throws Exception {
        log.debug("Inbound response: " + msg);
        this.ctx = ctx;
        this.response = msg;
        respReceived.countDown();
    }
}
