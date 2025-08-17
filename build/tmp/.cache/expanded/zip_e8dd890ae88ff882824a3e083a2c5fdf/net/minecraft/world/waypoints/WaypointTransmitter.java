package net.minecraft.world.waypoints;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public interface WaypointTransmitter extends Waypoint {
    int REALLY_FAR_DISTANCE = 332;

    boolean isTransmittingWaypoint();

    Optional<WaypointTransmitter.Connection> makeWaypointConnectionWith(ServerPlayer pPlayer);

    Waypoint.Icon waypointIcon();

    static boolean doesSourceIgnoreReceiver(LivingEntity pEntity, ServerPlayer pPlayer) {
        if (pPlayer.isSpectator()) {
            return false;
        } else if (!pEntity.isSpectator() && !pEntity.hasIndirectPassenger(pPlayer)) {
            double d0 = Math.min(pEntity.getAttributeValue(Attributes.WAYPOINT_TRANSMIT_RANGE), pPlayer.getAttributeValue(Attributes.WAYPOINT_RECEIVE_RANGE));
            return pEntity.distanceTo(pPlayer) >= d0;
        } else {
            return true;
        }
    }

    static boolean isChunkVisible(ChunkPos pPos, ServerPlayer pPlayer) {
        return pPlayer.getChunkTrackingView().isInViewDistance(pPos.x, pPos.z);
    }

    static boolean isReallyFar(LivingEntity pEntity, ServerPlayer pPlayer) {
        return pEntity.distanceTo(pPlayer) > 332.0F;
    }

    public interface BlockConnection extends WaypointTransmitter.Connection {
        int distanceManhattan();

        @Override
        default boolean isBroken() {
            return this.distanceManhattan() > 1;
        }
    }

    public interface ChunkConnection extends WaypointTransmitter.Connection {
        int distanceChessboard();

        @Override
        default boolean isBroken() {
            return this.distanceChessboard() > 1;
        }
    }

    public interface Connection {
        void connect();

        void disconnect();

        void update();

        boolean isBroken();
    }

    public static class EntityAzimuthConnection implements WaypointTransmitter.Connection {
        private final LivingEntity source;
        private final Waypoint.Icon icon;
        private final ServerPlayer receiver;
        private float lastAngle;

        public EntityAzimuthConnection(LivingEntity pSource, Waypoint.Icon pIcon, ServerPlayer pReciever) {
            this.source = pSource;
            this.icon = pIcon;
            this.receiver = pReciever;
            Vec3 vec3 = pReciever.position().subtract(pSource.position()).rotateClockwise90();
            this.lastAngle = (float)Mth.atan2(vec3.z(), vec3.x());
        }

        @Override
        public boolean isBroken() {
            return WaypointTransmitter.doesSourceIgnoreReceiver(this.source, this.receiver)
                || WaypointTransmitter.isChunkVisible(this.source.chunkPosition(), this.receiver)
                || !WaypointTransmitter.isReallyFar(this.source, this.receiver);
        }

        @Override
        public void connect() {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointAzimuth(this.source.getUUID(), this.icon, this.lastAngle));
        }

        @Override
        public void disconnect() {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(this.source.getUUID()));
        }

        @Override
        public void update() {
            Vec3 vec3 = this.receiver.position().subtract(this.source.position()).rotateClockwise90();
            float f = (float)Mth.atan2(vec3.z(), vec3.x());
            if (Mth.abs(f - this.lastAngle) > 0.008726646F) {
                this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointAzimuth(this.source.getUUID(), this.icon, f));
                this.lastAngle = f;
            }
        }
    }

    public static class EntityBlockConnection implements WaypointTransmitter.BlockConnection {
        private final LivingEntity source;
        private final Waypoint.Icon icon;
        private final ServerPlayer receiver;
        private BlockPos lastPosition;

        public EntityBlockConnection(LivingEntity pSource, Waypoint.Icon pIcon, ServerPlayer pReciever) {
            this.source = pSource;
            this.receiver = pReciever;
            this.icon = pIcon;
            this.lastPosition = pSource.blockPosition();
        }

        @Override
        public void connect() {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointPosition(this.source.getUUID(), this.icon, this.lastPosition));
        }

        @Override
        public void disconnect() {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(this.source.getUUID()));
        }

        @Override
        public void update() {
            BlockPos blockpos = this.source.blockPosition();
            if (blockpos.distManhattan(this.lastPosition) > 0) {
                this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointPosition(this.source.getUUID(), this.icon, blockpos));
                this.lastPosition = blockpos;
            }
        }

        @Override
        public int distanceManhattan() {
            return this.lastPosition.distManhattan(this.source.blockPosition());
        }

        @Override
        public boolean isBroken() {
            return WaypointTransmitter.BlockConnection.super.isBroken() || WaypointTransmitter.doesSourceIgnoreReceiver(this.source, this.receiver);
        }
    }

    public static class EntityChunkConnection implements WaypointTransmitter.ChunkConnection {
        private final LivingEntity source;
        private final Waypoint.Icon icon;
        private final ServerPlayer receiver;
        private ChunkPos lastPosition;

        public EntityChunkConnection(LivingEntity pSource, Waypoint.Icon pIcon, ServerPlayer pReceiver) {
            this.source = pSource;
            this.icon = pIcon;
            this.receiver = pReceiver;
            this.lastPosition = pSource.chunkPosition();
        }

        @Override
        public int distanceChessboard() {
            return this.lastPosition.getChessboardDistance(this.source.chunkPosition());
        }

        @Override
        public void connect() {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointChunk(this.source.getUUID(), this.icon, this.lastPosition));
        }

        @Override
        public void disconnect() {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(this.source.getUUID()));
        }

        @Override
        public void update() {
            ChunkPos chunkpos = this.source.chunkPosition();
            if (chunkpos.getChessboardDistance(this.lastPosition) > 0) {
                this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointChunk(this.source.getUUID(), this.icon, chunkpos));
                this.lastPosition = chunkpos;
            }
        }

        @Override
        public boolean isBroken() {
            return !WaypointTransmitter.ChunkConnection.super.isBroken() && !WaypointTransmitter.doesSourceIgnoreReceiver(this.source, this.receiver)
                ? WaypointTransmitter.isChunkVisible(this.lastPosition, this.receiver)
                : true;
        }
    }
}