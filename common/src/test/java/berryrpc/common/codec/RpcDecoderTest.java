package berryrpc.common.codec;

import berryrpc.common.RpcRequest;
import berryrpc.common.RpcResponse;
import berryrpc.common.util.support.ProtostuffSerialization;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

@Slf4j
public class RpcDecoderTest {

    private RpcResponse makeResponse() {
        RpcResponse response = new RpcResponse();
        response.setRequestId(UUID.randomUUID().toString());
        response.setResult("Hello there");
        return response;
    }

    private RpcRequest makeRequest() {
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setInterfaceName("berryrpc.sample.service.GreetingService");
        request.setMethodName("greet");
        request.setParameters(new Object[]{"num1", "num2"});
        request.setParameterTypes(new Class[]{String.class, String.class});

        return request;
    }

    @Test
    public void decode() {
        ByteBuf buf = Unpooled.buffer();

        RpcRequest testRequest = makeRequest();
        byte[] bytes = ProtostuffSerialization.serialize(testRequest);
        int len = bytes.length;

        buf.writeInt(len);
        buf.writeBytes(bytes);
        ByteBuf input = buf.duplicate();
        EmbeddedChannel channel = new EmbeddedChannel(new RpcDecoder(RpcRequest.class));
        assertTrue(channel.writeInbound(input.retain()));

        assertTrue(channel.finish());

        // read messages
        RpcRequest recvReq = channel.readInbound();

        assertEquals(testRequest, recvReq);

        assertNull(channel.readInbound());
        buf.release();
    }

    @Test
    public void encodeThenDecode() {
        RpcRequest testRequest = makeRequest();
        EmbeddedChannel channel = new EmbeddedChannel(new RpcDecoder(RpcRequest.class), new RpcEncoder());
        assertTrue(channel.writeOutbound(testRequest));

        ByteBuf buf = channel.readOutbound();
        assertTrue(channel.writeInbound(buf));
        assertTrue(channel.finish());

        assertEquals(testRequest, channel.readInbound());
//        buf.release();

    }

/*    @Test
    public void rpcClient() {
        EmbeddedChannel channel = new EmbeddedChannel(new RpcEncoder(), new RpcDecoder(RpcRequest.class),
                new RpcClient("127.0.0.1:8999"));
        RpcResponse response = makeResponse();
        // Convert response to bytes
        assertTrue(channel.writeOutbound(response));
        ByteBuf buf = channel.readOutbound();

        assertTrue(channel.writeInbound(buf));
        assertTrue(channel.finish());
    }*/
}