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

public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {
    private String serviceAddress;
    private RpcResponse response;
    private ChannelHandlerContext ctx;
    private EventLoopGroup group;

    public RpcClient(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public RpcResponse send(RpcRequest request) {
        this.group = new NioEventLoopGroup();
        final RpcEncoder encoder = new RpcEncoder();
        final RpcDecoder decoder = new RpcDecoder(RpcResponse.class);

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
                        ch.pipeline().addLast(encoder)
                                .addLast(decoder)
                                .addLast(this);
                    }
                });
        ChannelFuture future = bootstrap.connect();
        future.channel().writeAndFlush(request);

        return this.response;
    }

    /**
     * Shuts dow the connection with service provider server.
     */
    public void shutDownConnection() {
        ctx.close();
        group.shutdownGracefully().syncUninterruptibly();
    }

    // SimpleInboundHandler auto releases byte buffer.
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse msg) throws Exception {
        this.ctx = ctx;
        this.response = msg;
    }
}
