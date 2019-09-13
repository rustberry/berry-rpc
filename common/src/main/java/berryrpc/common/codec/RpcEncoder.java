package berryrpc.common.codec;

import berryrpc.common.util.support.ProtostuffSerialization;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class RpcEncoder extends MessageToByteEncoder {
    private Class<?> genericType;

    public RpcEncoder() {}

    public RpcEncoder(Class<?> cls) {
        this.genericType = cls;
    }
    
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        byte[] byteArray = ProtostuffSerialization.serialize(msg);
        out.writeInt(byteArray.length);
        out.writeBytes(byteArray);
    }
}

