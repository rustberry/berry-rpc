package berryrpc.server;

import berryrpc.common.RpcRequest;
import berryrpc.common.codec.RpcDecoder;
import berryrpc.common.codec.RpcEncoder;
import berryrpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Log4j
public class RpcServer implements ApplicationContextAware, InitializingBean {
    private Map<String, Object> handlerMap = new HashMap<>();
    private ServiceRegistry registry;
    private String localAddress;

    public RpcServer(String address) {
        this.localAddress = address;
    }

    public RpcServer(String address, ServiceRegistry registry) {
        this.localAddress = address;
        this.registry = registry;
    }

    /**
     * {@inheritDoc}
     * Generate a handler map for classes annotated to be registered as service handler
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String , Object> rpcBeans = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (rpcBeans != null) {  // Todo use Commons to check null
            for (Object bean : rpcBeans.values()) {
                Class<?> apiInterface = bean.getClass().getAnnotation(RpcService.class).value();
                handlerMap.put(apiInterface.getName(), bean);
            }
        }
    }

    /**
     * {@inheritDoc}
     * Publish the service beans to {@code registry}.
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        EventLoopGroup  boss = new NioEventLoopGroup(1);
        EventLoopGroup  worker = new NioEventLoopGroup();
        final RpcHandler rpcHandler = new RpcHandler(handlerMap);
        final RpcEncoder encoder = new RpcEncoder();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(encoder)
                                    .addLast(new RpcDecoder(RpcRequest.class))
                                    .addLast(rpcHandler);
                        }
                    });
            int port = Integer.parseInt(this.localAddress.split(":")[1]);
            log.info("ServerChannel using port: " + port);
            ChannelFuture future = bootstrap.bind(port).sync();

            // Register services only after server had set up(bound)
            if (this.registry != null) {
                for (String serviceInterface :handlerMap.keySet()){
                    registry.register(serviceInterface, this.localAddress);
                }
            }

            // When the parent channel was being closed, sync until it finishes
            future.channel().closeFuture().sync();

            long currentTime = System.currentTimeMillis();
            log.debug("ServerChannel Closed: " +
                    new SimpleDateFormat("hh:mm:ss").format(new Date(currentTime)));
        } finally {
            boss.shutdownGracefully().sync();
            log.debug("boss group shutdown. " + boss + " at: " +
                    new SimpleDateFormat("hh:mm:ss").format(new Date(System.currentTimeMillis())));
            worker.shutdownGracefully().sync();
        }
    }

}
