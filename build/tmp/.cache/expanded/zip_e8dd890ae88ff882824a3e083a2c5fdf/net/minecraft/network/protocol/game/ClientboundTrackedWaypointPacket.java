package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.TrackedWaypointManager;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointManager;

public record ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation operation, TrackedWaypoint waypoint)
    implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundTrackedWaypointPacket> STREAM_CODEC = StreamCodec.composite(
        ClientboundTrackedWaypointPacket.Operation.STREAM_CODEC,
        ClientboundTrackedWaypointPacket::operation,
        TrackedWaypoint.STREAM_CODEC,
        ClientboundTrackedWaypointPacket::waypoint,
        ClientboundTrackedWaypointPacket::new
    );

    public static ClientboundTrackedWaypointPacket removeWaypoint(UUID pUuid) {
        return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.UNTRACK, TrackedWaypoint.empty(pUuid));
    }

    public static ClientboundTrackedWaypointPacket addWaypointPosition(UUID pUuid, Waypoint.Icon pIcon, Vec3i pPosition) {
        return new ClientboundTrackedWaypointPacket(
            ClientboundTrackedWaypointPacket.Operation.TRACK, TrackedWaypoint.setPosition(pUuid, pIcon, pPosition)
        );
    }

    public static ClientboundTrackedWaypointPacket updateWaypointPosition(UUID pUuid, Waypoint.Icon pIcon, Vec3i pPosition) {
        return new ClientboundTrackedWaypointPacket(
            ClientboundTrackedWaypointPacket.Operation.UPDATE, TrackedWaypoint.setPosition(pUuid, pIcon, pPosition)
        );
    }

    public static ClientboundTrackedWaypointPacket addWaypointChunk(UUID pUuid, Waypoint.Icon pIcon, ChunkPos pChunkPos) {
        return new ClientboundTrackedWaypointPacket(
            ClientboundTrackedWaypointPacket.Operation.TRACK, TrackedWaypoint.setChunk(pUuid, pIcon, pChunkPos)
        );
    }

    public static ClientboundTrackedWaypointPacket updateWaypointChunk(UUID pUuid, Waypoint.Icon pIcon, ChunkPos pChunkPos) {
        return new ClientboundTrackedWaypointPacket(
            ClientboundTrackedWaypointPacket.Operation.UPDATE, TrackedWaypoint.setChunk(pUuid, pIcon, pChunkPos)
        );
    }

    public static ClientboundTrackedWaypointPacket addWaypointAzimuth(UUID pUuid, Waypoint.Icon pIcon, float pAngle) {
        return new ClientboundTrackedWaypointPacket(
            ClientboundTrackedWaypointPacket.Operation.TRACK, TrackedWaypoint.setAzimuth(pUuid, pIcon, pAngle)
        );
    }

    public static ClientboundTrackedWaypointPacket updateWaypointAzimuth(UUID pUuid, Waypoint.Icon pIcon, float pAngle) {
        return new ClientboundTrackedWaypointPacket(
            ClientboundTrackedWaypointPacket.Operation.UPDATE, TrackedWaypoint.setAzimuth(pUuid, pIcon, pAngle)
        );
    }

    @Override
    public PacketType<ClientboundTrackedWaypointPacket> type() {
        return GamePacketTypes.CLIENTBOUND_WAYPOINT;
    }

    public void handle(ClientGamePacketListener p_406728_) {
        p_406728_.handleWaypoint(this);
    }

    public void apply(TrackedWaypointManager pWaypointManager) {
        this.operation.action.accept(pWaypointManager, this.waypoint);
    }

    static enum Operation {
        TRACK(WaypointManager::trackWaypoint),
        UNTRACK(WaypointManager::untrackWaypoint),
        UPDATE(WaypointManager::updateWaypoint);

        final BiConsumer<TrackedWaypointManager, TrackedWaypoint> action;
        public static final IntFunction<ClientboundTrackedWaypointPacket.Operation> BY_ID = ByIdMap.continuous(
            Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP
        );
        public static final StreamCodec<ByteBuf, ClientboundTrackedWaypointPacket.Operation> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

        private Operation(final BiConsumer<TrackedWaypointManager, TrackedWaypoint> pAction) {
            this.action = pAction;
        }
    }
}