package berryrpc.server;

import berryrpc.common.RpcRequest;
import berryrpc.common.RpcResponse;
import berryrpc.common.util.ProxyUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

@ChannelHandler.Sharable
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private Map<String, Object> handlerMap;

    public RpcHandler(Map<String, Object> map) {
        this.handlerMap = map;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest msg) throws Exception {
        RpcResponse response = new RpcResponse();
        response.setRequestId(msg.getRequestId());

        try {
            Object result = handle(msg);
            response.setResult(result);
        } catch (Exception e) {
            response.setException(e);
        }

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

        return result;
    }
}
