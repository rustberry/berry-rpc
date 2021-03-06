package berryrpc.common.codec;

import berryrpc.common.util.support.ProtostuffSerialization;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RpcDecoder extends ByteToMessageDecoder {
    private Class<?> genericType;

    public RpcDecoder(Class<?> cls) {
        this.genericType = cls;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;  // The beginning 4 bytes is by definition length of the byte array
        }
        in.markReaderIndex();
        int dataLen = in.readInt();
        if (in.readableBytes() < dataLen) {
            in.resetReaderIndex();  // Restore.
            return;
        }

        byte[] byteArray = new byte[dataLen];
        in.readBytes(byteArray);

        Object msg = ProtostuffSerialization.deserialize(byteArray, this.genericType);
        log.debug("Inbound request/response received: " + msg);
        out.add(msg);
//        out.add(ProtostuffSerialization.deserialize(byteArray, this.genericType));
    }
    /*public RpcDecoder(com.google.protobuf.MessageLite prototype) {
        super(prototype);
    }*/
}
