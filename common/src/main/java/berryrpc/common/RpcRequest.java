package berryrpc.common;

import lombok.Data;

@Data
public class RpcRequest {
    private String requestId;
    private String interfaceName;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;

    // Todo add service version
}
