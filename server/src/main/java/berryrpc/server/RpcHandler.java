package berryrpc.server;

import berryrpc.common.RpcRequest;
import berryrpc.common.RpcResponse;
import berryrpc.common.util.ProxyUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
@ChannelHandler.Sharable
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private Map<String, Object> handlerMap;
//    final private ExecutorService reqHandler = Executors.newCachedThreadPool();

    public RpcHandler(Map<String, Object> map) {
        this.handlerMap = map;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest msg) throws Exception {
        RpcResponse response = new RpcResponse();
        response.setRequestId(msg.getRequestId());
        log.debug("Got inbound request: " + msg);

        try {
            Object result = handle(msg);
            response.setResult(result);
        } catch (Exception e) {
            response.setException(e);
        }

        log.debug("Outbound response: " + response);
        ctx.writeAndFlush(response);
    }

    /**
     *
     * @param request
     * @return The call result, via Java standard reflection library.
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public Object handle(RpcRequest request) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> targetClass = null;
        Object target = handlerMap.get(request.getInterfaceName());
        Method method = target.getClass().getMethod(request.getMethodName(),request.getParameterTypes());

        Object result = ProxyUtil.invokeMethod(target, method, request.getParameters());
//        EventExecutor
//        reqHandler.

        return result;
    }
}
