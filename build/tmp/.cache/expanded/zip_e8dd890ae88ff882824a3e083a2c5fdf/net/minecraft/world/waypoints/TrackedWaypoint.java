package net.minecraft.world.waypoints;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;

public abstract class TrackedWaypoint implements Waypoint {
    static final Logger LOGGER = LogUtils.getLogger();
    public static StreamCodec<ByteBuf, TrackedWaypoint> STREAM_CODEC = StreamCodec.ofMember(TrackedWaypoint::write, TrackedWaypoint::read);
    protected final Either<UUID, String> identifier;
    private final Waypoint.Icon icon;
    private final TrackedWaypoint.Type type;

    TrackedWaypoint(Either<UUID, String> pIdentifier, Waypoint.Icon pIcon, TrackedWaypoint.Type pType) {
        this.identifier = pIdentifier;
        this.icon = pIcon;
        this.type = pType;
    }

    public Either<UUID, String> id() {
        return this.identifier;
    }

    public abstract void update(TrackedWaypoint pWaypoint);

    public void write(ByteBuf pBuffer) {
        FriendlyByteBuf friendlybytebuf = new FriendlyByteBuf(pBuffer);
        friendlybytebuf.writeEither(this.identifier, UUIDUtil.STREAM_CODEC, FriendlyByteBuf::writeUtf);
        Waypoint.Icon.STREAM_CODEC.encode(friendlybytebuf, this.icon);
        friendlybytebuf.writeEnum(this.type);
        this.writeContents(pBuffer);
    }

    public abstract void writeContents(ByteBuf pBuffer);

    private static TrackedWaypoint read(ByteBuf pBuffer) {
        FriendlyByteBuf friendlybytebuf = new FriendlyByteBuf(pBuffer);
        Either<UUID, String> either = friendlybytebuf.readEither(UUIDUtil.STREAM_CODEC, FriendlyByteBuf::readUtf);
        Waypoint.Icon waypoint$icon = Waypoint.Icon.STREAM_CODEC.decode(friendlybytebuf);
        TrackedWaypoint.Type trackedwaypoint$type = friendlybytebuf.readEnum(TrackedWaypoint.Type.class);
        return trackedwaypoint$type.constructor.apply(either, waypoint$icon, friendlybytebuf);
    }

    public static TrackedWaypoint setPosition(UUID pUuid, Waypoint.Icon pIcon, Vec3i pPosition) {
        return new TrackedWaypoint.Vec3iWaypoint(pUuid, pIcon, pPosition);
    }

    public static TrackedWaypoint setChunk(UUID pUuid, Waypoint.Icon pIcon, ChunkPos pChunkPos) {
        return new TrackedWaypoint.ChunkWaypoint(pUuid, pIcon, pChunkPos);
    }

    public static TrackedWaypoint setAzimuth(UUID pUuid, Waypoint.Icon pIcon, float pAngle) {
        return new TrackedWaypoint.AzimuthWaypoint(pUuid, pIcon, pAngle);
    }

    public static TrackedWaypoint empty(UUID pUuid) {
        return new TrackedWaypoint.EmptyWaypoint(pUuid);
    }

    public abstract double yawAngleToCamera(Level pLevel, TrackedWaypoint.Camera pCamera);

    public abstract TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level pLevel, TrackedWaypoint.Projector pProjector);

    public abstract double distanceSquared(Entity pEntity);

    public Waypoint.Icon icon() {
        return this.icon;
    }

    static class AzimuthWaypoint extends TrackedWaypoint {
        private float angle;

        public AzimuthWaypoint(UUID pUuid, Waypoint.Icon pIcon, float pAngle) {
            super(Either.left(pUuid), pIcon, TrackedWaypoint.Type.AZIMUTH);
            this.angle = pAngle;
        }

        public AzimuthWaypoint(Either<UUID, String> pIdentifier, Waypoint.Icon pIcon, FriendlyByteBuf pBuffer) {
            super(pIdentifier, pIcon, TrackedWaypoint.Type.AZIMUTH);
            this.angle = pBuffer.readFloat();
        }

        @Override
        public void update(TrackedWaypoint p_406882_) {
            if (p_406882_ instanceof TrackedWaypoint.AzimuthWaypoint trackedwaypoint$azimuthwaypoint) {
                this.angle = trackedwaypoint$azimuthwaypoint.angle;
            } else {
                TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", p_406882_.getClass());
            }
        }

        @Override
        public void writeContents(ByteBuf p_407138_) {
            p_407138_.writeFloat(this.angle);
        }

        @Override
        public double yawAngleToCamera(Level p_408876_, TrackedWaypoint.Camera p_409314_) {
            return Mth.degreesDifference(p_409314_.yaw(), this.angle * (180.0F / (float)Math.PI));
        }

        @Override
        public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level p_410040_, TrackedWaypoint.Projector p_409196_) {
            double d0 = p_409196_.projectHorizonToScreen();
            if (d0 < -1.0) {
                return TrackedWaypoint.PitchDirection.DOWN;
            } else {
                return d0 > 1.0 ? TrackedWaypoint.PitchDirection.UP : TrackedWaypoint.PitchDirection.NONE;
            }
        }

        @Override
        public double distanceSquared(Entity p_408029_) {
            return Double.POSITIVE_INFINITY;
        }
    }

    public interface Camera {
        float yaw();

        Vec3 position();
    }

    static class ChunkWaypoint extends TrackedWaypoint {
        private ChunkPos chunkPos;

        public ChunkWaypoint(UUID pUuid, Waypoint.Icon pIcon, ChunkPos pChunkPos) {
            super(Either.left(pUuid), pIcon, TrackedWaypoint.Type.CHUNK);
            this.chunkPos = pChunkPos;
        }

        public ChunkWaypoint(Either<UUID, String> pIdentifier, Waypoint.Icon pIcon, FriendlyByteBuf pBuffer) {
            super(pIdentifier, pIcon, TrackedWaypoint.Type.CHUNK);
            this.chunkPos = new ChunkPos(pBuffer.readVarInt(), pBuffer.readVarInt());
        }

        @Override
        public void update(TrackedWaypoint p_410032_) {
            if (p_410032_ instanceof TrackedWaypoint.ChunkWaypoint trackedwaypoint$chunkwaypoint) {
                this.chunkPos = trackedwaypoint$chunkwaypoint.chunkPos;
            } else {
                TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", p_410032_.getClass());
            }
        }

        @Override
        public void writeContents(ByteBuf p_409801_) {
            VarInt.write(p_409801_, this.chunkPos.x);
            VarInt.write(p_409801_, this.chunkPos.z);
        }

        private Vec3 position(double pY) {
            return Vec3.atCenterOf(this.chunkPos.getMiddleBlockPosition((int)pY));
        }

        @Override
        public double yawAngleToCamera(Level p_406004_, TrackedWaypoint.Camera p_409311_) {
            Vec3 vec3 = p_409311_.position();
            Vec3 vec31 = vec3.subtract(this.position(vec3.y())).rotateClockwise90();
            float f = (float)Mth.atan2(vec31.z(), vec31.x()) * (180.0F / (float)Math.PI);
            return Mth.degreesDifference(p_409311_.yaw(), f);
        }

        @Override
        public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level p_408195_, TrackedWaypoint.Projector p_408761_) {
            double d0 = p_408761_.projectHorizonToScreen();
            if (d0 < -1.0) {
                return TrackedWaypoint.PitchDirection.DOWN;
            } else {
                return d0 > 1.0 ? TrackedWaypoint.PitchDirection.UP : TrackedWaypoint.PitchDirection.NONE;
            }
        }

        @Override
        public double distanceSquared(Entity p_410438_) {
            return p_410438_.distanceToSqr(Vec3.atCenterOf(this.chunkPos.getMiddleBlockPosition(p_410438_.getBlockY())));
        }
    }

    static class EmptyWaypoint extends TrackedWaypoint {
        private EmptyWaypoint(Either<UUID, String> pIdentifier, Waypoint.Icon pIcon, FriendlyByteBuf pBuffer) {
            super(pIdentifier, pIcon, TrackedWaypoint.Type.EMPTY);
        }

        EmptyWaypoint(UUID pUuid) {
            super(Either.left(pUuid), Waypoint.Icon.NULL, TrackedWaypoint.Type.EMPTY);
        }

        @Override
        public void update(TrackedWaypoint p_410388_) {
        }

        @Override
        public void writeContents(ByteBuf p_407110_) {
        }

        @Override
        public double yawAngleToCamera(Level p_408498_, TrackedWaypoint.Camera p_407913_) {
            return Double.NaN;
        }

        @Override
        public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level p_409138_, TrackedWaypoint.Projector p_407627_) {
            return TrackedWaypoint.PitchDirection.NONE;
        }

        @Override
        public double distanceSquared(Entity p_408264_) {
            return Double.POSITIVE_INFINITY;
        }
    }

    public static enum PitchDirection {
        NONE,
        UP,
        DOWN;
    }

    public interface Projector {
        Vec3 projectPointToScreen(Vec3 pPoint);

        double projectHorizonToScreen();
    }

    static enum Type {
        EMPTY(TrackedWaypoint.EmptyWaypoint::new),
        VEC3I(TrackedWaypoint.Vec3iWaypoint::new),
        CHUNK(TrackedWaypoint.ChunkWaypoint::new),
        AZIMUTH(TrackedWaypoint.AzimuthWaypoint::new);

        final TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint> constructor;

        private Type(final TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint> pConstructor) {
            this.constructor = pConstructor;
        }
    }

    static class Vec3iWaypoint extends TrackedWaypoint {
        private Vec3i vector;

        public Vec3iWaypoint(UUID pUuid, Waypoint.Icon pIcon, Vec3i pVector) {
            super(Either.left(pUuid), pIcon, TrackedWaypoint.Type.VEC3I);
            this.vector = pVector;
        }

        public Vec3iWaypoint(Either<UUID, String> pIdentifier, Waypoint.Icon pIcon, FriendlyByteBuf pBuffer) {
            super(pIdentifier, pIcon, TrackedWaypoint.Type.VEC3I);
            this.vector = new Vec3i(pBuffer.readVarInt(), pBuffer.readVarInt(), pBuffer.readVarInt());
        }

        @Override
        public void update(TrackedWaypoint p_407449_) {
            if (p_407449_ instanceof TrackedWaypoint.Vec3iWaypoint trackedwaypoint$vec3iwaypoint) {
                this.vector = trackedwaypoint$vec3iwaypoint.vector;
            } else {
                TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", p_407449_.getClass());
            }
        }

        @Override
        public void writeContents(ByteBuf p_407331_) {
            VarInt.write(p_407331_, this.vector.getX());
            VarInt.write(p_407331_, this.vector.getY());
            VarInt.write(p_407331_, this.vector.getZ());
        }

        private Vec3 position(Level pLevel) {
            return this.identifier
                .left()
                .map(pLevel::getEntity)
                .map(p_405831_ -> p_405831_.blockPosition().distManhattan(this.vector) > 3 ? null : p_405831_.getEyePosition())
                .orElseGet(() -> Vec3.atCenterOf(this.vector));
        }

        @Override
        public double yawAngleToCamera(Level p_407511_, TrackedWaypoint.Camera p_406431_) {
            Vec3 vec3 = p_406431_.position().subtract(this.position(p_407511_)).rotateClockwise90();
            float f = (float)Mth.atan2(vec3.z(), vec3.x()) * (180.0F / (float)Math.PI);
            return Mth.degreesDifference(p_406431_.yaw(), f);
        }

        @Override
        public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level p_409720_, TrackedWaypoint.Projector p_410220_) {
            Vec3 vec3 = p_410220_.projectPointToScreen(this.position(p_409720_));
            boolean flag = vec3.z > 1.0;
            double d0 = flag ? -vec3.y : vec3.y;
            if (d0 < -1.0) {
                return TrackedWaypoint.PitchDirection.DOWN;
            } else if (d0 > 1.0) {
                return TrackedWaypoint.PitchDirection.UP;
            } else {
                if (flag) {
                    if (vec3.y > 0.0) {
                        return TrackedWaypoint.PitchDirection.UP;
                    }

                    if (vec3.y < 0.0) {
                        return TrackedWaypoint.PitchDirection.DOWN;
                    }
                }

                return TrackedWaypoint.PitchDirection.NONE;
            }
        }

        @Override
        public double distanceSquared(Entity p_410208_) {
            return p_410208_.distanceToSqr(Vec3.atCenterOf(this.vector));
        }
    }
}