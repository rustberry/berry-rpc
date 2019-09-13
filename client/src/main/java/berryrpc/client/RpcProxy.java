package berryrpc.client;

import berryrpc.common.RpcRequest;
import berryrpc.common.RpcResponse;
import berryrpc.registry.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

@Slf4j
public class RpcProxy {
    private ServiceDiscovery serviceDiscovery;
    private String serviceAddress;
    private RpcClient client;

    public RpcProxy(ServiceDiscovery discovery) {
        this.serviceDiscovery = discovery;
    }

    public RpcProxy(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        RpcRequest request = new RpcRequest();
                        request.setRequestId(UUID.randomUUID().toString());
//                        request.setInterfaceName(interfaceClass.getName());
                        request.setInterfaceName(method.getDeclaringClass().getName());
                        request.setMethodName(method.getName());
                        request.setParameters(args);
                        request.setParameterTypes(method.getParameterTypes());

                        if (serviceDiscovery != null) serviceAddress = serviceDiscovery.discover(request.getInterfaceName());
                        if (client == null) {
                            client = new RpcClient(serviceAddress);
                        }

                        RpcResponse response = client.send(request);
                        log.debug("Outbound request: " + request);
                        if (response == null) {
                            shutdown();
                            throw new RuntimeException("RPC response is null");
                        }
                        if (response.hasException()) {
                            log.error("Response: " + response + " has exception: " + response.getException());
                            throw response.getException();
                        } else {
                            return response.getResult();
                        }
                    }
                }
        );
    }

    public void shutdown() {
        if (client != null) {
            client.shutDownConnection();
        } else {
            log.debug("This.client is null during shutdown");
        }
    }
}
