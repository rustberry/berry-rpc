package berryrpc.client;

import berryrpc.common.RpcRequest;
import berryrpc.common.RpcResponse;
import berryrpc.registry.ServiceDiscovery;
import lombok.extern.log4j.Log4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

@Log4j
public class RpcProxy {
    private ServiceDiscovery serviceDiscovery;
    private String serviceAddress;

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
                        RpcClient client = new RpcClient(serviceAddress);

                        RpcResponse response = client.send(request);
                        if (response == null) {
                            throw new RuntimeException("RPC response is null");
                        }
                        if (response.hasException()) {
                            throw response.getException();
                        } else {
                            return response.getResult();
                        }
                    }
                }
        );
    }
}
