package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.function.Supplier;
import net.minecraft.network.protocol.Packet;
import org.slf4j.Logger;

public class PacketSendListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static ChannelFutureListener thenRun(Runnable pAction) {
        return p_406613_ -> {
            pAction.run();
            if (!p_406613_.isSuccess()) {
                p_406613_.channel().pipeline().fireExceptionCaught(p_406613_.cause());
            }
        };
    }

    public static ChannelFutureListener exceptionallySend(Supplier<Packet<?>> pPacketSupplier) {
        return p_407106_ -> {
            if (!p_407106_.isSuccess()) {
                Packet<?> packet = pPacketSupplier.get();
                if (packet != null) {
                    LOGGER.warn("Failed to deliver packet, sending fallback {}", packet.type(), p_407106_.cause());
                    p_407106_.channel().writeAndFlush(packet, p_407106_.channel().voidPromise());
                } else {
                    p_407106_.channel().pipeline().fireExceptionCaught(p_407106_.cause());
                }
            }
        };
    }
}