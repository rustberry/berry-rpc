package berryrpc.common;

import lombok.Data;

@Data
public class RpcResponse {
    private String requestId;
    private Object result;
    private Exception exception;

    public boolean hasException() {
        return exception != null;
    }

}
