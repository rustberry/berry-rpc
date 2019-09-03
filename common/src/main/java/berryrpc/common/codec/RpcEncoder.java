package berryrpc.common.codec;

import berryrpc.common.RpcRequest;
import berryrpc.common.util.support.ProtostuffSerialization;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import static berryrpc.common.util.support.ProtostuffSerialization.serialize;

@ChannelHandler.Sharable
public class RpcEncoder extends MessageToByteEncoder<RpcRequest> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcRequest msg, ByteBuf out) throws Exception {
        byte[] byteArray = ProtostuffSerialization.serialize(msg);
        out.writeInt(byteArray.length);
        out.writeBytes(byteArray);
    }
}

