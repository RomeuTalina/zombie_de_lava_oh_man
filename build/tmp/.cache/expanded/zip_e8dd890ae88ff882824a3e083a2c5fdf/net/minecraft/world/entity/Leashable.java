package net.minecraft.world.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface Leashable {
    String LEASH_TAG = "leash";
    double LEASH_TOO_FAR_DIST = 12.0;
    double LEASH_ELASTIC_DIST = 6.0;
    double MAXIMUM_ALLOWED_LEASHED_DIST = 16.0;
    Vec3 AXIS_SPECIFIC_ELASTICITY = new Vec3(0.8, 0.2, 0.8);
    float SPRING_DAMPENING = 0.7F;
    double TORSIONAL_ELASTICITY = 10.0;
    double STIFFNESS = 0.11;
    List<Vec3> ENTITY_ATTACHMENT_POINT = ImmutableList.of(new Vec3(0.0, 0.5, 0.5));
    List<Vec3> LEASHER_ATTACHMENT_POINT = ImmutableList.of(new Vec3(0.0, 0.5, 0.0));
    List<Vec3> SHARED_QUAD_ATTACHMENT_POINTS = ImmutableList.of(new Vec3(-0.5, 0.5, 0.5), new Vec3(-0.5, 0.5, -0.5), new Vec3(0.5, 0.5, -0.5), new Vec3(0.5, 0.5, 0.5));

    @Nullable
    Leashable.LeashData getLeashData();

    void setLeashData(@Nullable Leashable.LeashData pLeashData);

    default boolean isLeashed() {
        return this.getLeashData() != null && this.getLeashData().leashHolder != null;
    }

    default boolean mayBeLeashed() {
        return this.getLeashData() != null;
    }

    default boolean canHaveALeashAttachedTo(Entity pEntity) {
        if (this == pEntity) {
            return false;
        } else {
            return this.leashDistanceTo(pEntity) > this.leashSnapDistance() ? false : this.canBeLeashed();
        }
    }

    default double leashDistanceTo(Entity pEntity) {
        return pEntity.getBoundingBox().getCenter().distanceTo(((Entity)this).getBoundingBox().getCenter());
    }

    default boolean canBeLeashed() {
        return true;
    }

    default void setDelayedLeashHolderId(int pDelayedLeashHolderId) {
        this.setLeashData(new Leashable.LeashData(pDelayedLeashHolderId));
        dropLeash((Entity & Leashable)this, false, false);
    }

    default void readLeashData(ValueInput pInput) {
        Leashable.LeashData leashable$leashdata = pInput.read("leash", Leashable.LeashData.CODEC).orElse(null);
        if (this.getLeashData() != null && leashable$leashdata == null) {
            this.removeLeash();
        }

        this.setLeashData(leashable$leashdata);
    }

    default void writeLeashData(ValueOutput pOutput, @Nullable Leashable.LeashData pLeashData) {
        pOutput.storeNullable("leash", Leashable.LeashData.CODEC, pLeashData);
    }

    private static <E extends Entity & Leashable> void restoreLeashFromSave(E pEntity, Leashable.LeashData pLeashData) {
        if (pLeashData.delayedLeashInfo != null && pEntity.level() instanceof ServerLevel serverlevel) {
            Optional<UUID> optional1 = pLeashData.delayedLeashInfo.left();
            Optional<BlockPos> optional = pLeashData.delayedLeashInfo.right();
            if (optional1.isPresent()) {
                Entity entity = serverlevel.getEntity(optional1.get());
                if (entity != null) {
                    setLeashedTo(pEntity, entity, true);
                    return;
                }
            } else if (optional.isPresent()) {
                setLeashedTo(pEntity, LeashFenceKnotEntity.getOrCreateKnot(serverlevel, optional.get()), true);
                return;
            }

            if (pEntity.tickCount > 100) {
                pEntity.spawnAtLocation(serverlevel, Items.LEAD);
                pEntity.setLeashData(null);
            }
        }
    }

    default void dropLeash() {
        dropLeash((Entity & Leashable)this, true, true);
    }

    default void removeLeash() {
        dropLeash((Entity & Leashable)this, true, false);
    }

    default void onLeashRemoved() {
    }

    private static <E extends Entity & Leashable> void dropLeash(E pEntity, boolean pBroadcastPacket, boolean pDropItem) {
        Leashable.LeashData leashable$leashdata = pEntity.getLeashData();
        if (leashable$leashdata != null && leashable$leashdata.leashHolder != null) {
            pEntity.setLeashData(null);
            pEntity.onLeashRemoved();
            if (pEntity.level() instanceof ServerLevel serverlevel) {
                if (pDropItem) {
                    pEntity.spawnAtLocation(serverlevel, Items.LEAD);
                }

                if (pBroadcastPacket) {
                    serverlevel.getChunkSource().broadcast(pEntity, new ClientboundSetEntityLinkPacket(pEntity, null));
                }

                leashable$leashdata.leashHolder.notifyLeasheeRemoved(pEntity);
            }
        }
    }

    static <E extends Entity & Leashable> void tickLeash(ServerLevel pLevel, E pEntity) {
        Leashable.LeashData leashable$leashdata = pEntity.getLeashData();
        if (leashable$leashdata != null && leashable$leashdata.delayedLeashInfo != null) {
            restoreLeashFromSave(pEntity, leashable$leashdata);
        }

        if (leashable$leashdata != null && leashable$leashdata.leashHolder != null) {
            if (!pEntity.isAlive() || !leashable$leashdata.leashHolder.isAlive()) {
                if (pLevel.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                    pEntity.dropLeash();
                } else {
                    pEntity.removeLeash();
                }
            }

            Entity entity = pEntity.getLeashHolder();
            if (entity != null && entity.level() == pEntity.level()) {
                double d0 = pEntity.leashDistanceTo(entity);
                pEntity.whenLeashedTo(entity);
                if (d0 > pEntity.leashSnapDistance()) {
                    pLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.LEAD_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    pEntity.leashTooFarBehaviour();
                } else if (d0 > pEntity.leashElasticDistance() - entity.getBbWidth() - pEntity.getBbWidth() && pEntity.checkElasticInteractions(entity, leashable$leashdata)) {
                    pEntity.onElasticLeashPull();
                } else {
                    pEntity.closeRangeLeashBehaviour(entity);
                }

                pEntity.setYRot((float)(pEntity.getYRot() - leashable$leashdata.angularMomentum));
                leashable$leashdata.angularMomentum = leashable$leashdata.angularMomentum * angularFriction(pEntity);
            }
        }
    }

    default void onElasticLeashPull() {
        Entity entity = (Entity)this;
        entity.checkFallDistanceAccumulation();
    }

    default double leashSnapDistance() {
        return 12.0;
    }

    default double leashElasticDistance() {
        return 6.0;
    }

    static <E extends Entity & Leashable> float angularFriction(E pEntity) {
        if (pEntity.onGround()) {
            var pos = pEntity.getBlockPosBelowThatAffectsMyMovement();
            return pEntity.level().getBlockState(pos).getFriction(pEntity.level(), pos, pEntity) * 0.91F;
        } else {
            return pEntity.isInLiquid() ? 0.8F : 0.91F;
        }
    }

    default void whenLeashedTo(Entity pEntity) {
        pEntity.notifyLeashHolder(this);
    }

    default void leashTooFarBehaviour() {
        this.dropLeash();
    }

    default void closeRangeLeashBehaviour(Entity pEntity) {
    }

    default boolean checkElasticInteractions(Entity pEntity, Leashable.LeashData pLeashData) {
        boolean flag = pEntity.supportQuadLeashAsHolder() && this.supportQuadLeash();
        List<Leashable.Wrench> list = computeElasticInteraction((Entity & Leashable)this, pEntity, flag ? SHARED_QUAD_ATTACHMENT_POINTS : ENTITY_ATTACHMENT_POINT, flag ? SHARED_QUAD_ATTACHMENT_POINTS : LEASHER_ATTACHMENT_POINT);
        if (list.isEmpty()) {
            return false;
        } else {
            Leashable.Wrench leashable$wrench = Leashable.Wrench.accumulate(list).scale(flag ? 0.25 : 1.0);
            pLeashData.angularMomentum = pLeashData.angularMomentum + 10.0 * leashable$wrench.torque();
            Vec3 vec3 = getHolderMovement(pEntity).subtract(((Entity)this).getKnownMovement());
            ((Entity)this).addDeltaMovement(leashable$wrench.force().multiply(AXIS_SPECIFIC_ELASTICITY).add(vec3.scale(0.11)));
            return true;
        }
    }

    private static Vec3 getHolderMovement(Entity pHolder) {
        return pHolder instanceof Mob mob && mob.isNoAi() ? Vec3.ZERO : pHolder.getKnownMovement();
    }

    private static <E extends Entity & Leashable> List<Leashable.Wrench> computeElasticInteraction(E pEntity, Entity pLeashHolder, List<Vec3> pEntityAttachmentPoint, List<Vec3> pLeasherAttachmentPoint) {
        double d0 = pEntity.leashElasticDistance();
        Vec3 vec3 = getHolderMovement(pEntity);
        float f = pEntity.getYRot() * (float) (Math.PI / 180.0);
        Vec3 vec31 = new Vec3(pEntity.getBbWidth(), pEntity.getBbHeight(), pEntity.getBbWidth());
        float f1 = pLeashHolder.getYRot() * (float) (Math.PI / 180.0);
        Vec3 vec32 = new Vec3(pLeashHolder.getBbWidth(), pLeashHolder.getBbHeight(), pLeashHolder.getBbWidth());
        List<Leashable.Wrench> list = new ArrayList<>();

        for (int i = 0; i < pEntityAttachmentPoint.size(); i++) {
            Vec3 vec33 = pEntityAttachmentPoint.get(i).multiply(vec31).yRot(-f);
            Vec3 vec34 = pEntity.position().add(vec33);
            Vec3 vec35 = pLeasherAttachmentPoint.get(i).multiply(vec32).yRot(-f1);
            Vec3 vec36 = pLeashHolder.position().add(vec35);
            computeDampenedSpringInteraction(vec36, vec34, d0, vec3, vec33).ifPresent(list::add);
        }

        return list;
    }

    private static Optional<Leashable.Wrench> computeDampenedSpringInteraction(Vec3 pEntityAttachmentPoint, Vec3 pLeasherAttachmentPoint, double pElasticDistance, Vec3 pKnownMovement, Vec3 pRelativeAttachmentPoint) {
        double d0 = pLeasherAttachmentPoint.distanceTo(pEntityAttachmentPoint);
        if (d0 < pElasticDistance) {
            return Optional.empty();
        } else {
            Vec3 vec3 = pEntityAttachmentPoint.subtract(pLeasherAttachmentPoint).normalize().scale(d0 - pElasticDistance);
            double d1 = Leashable.Wrench.torqueFromForce(pRelativeAttachmentPoint, vec3);
            boolean flag = pKnownMovement.dot(vec3) >= 0.0;
            if (flag) {
                vec3 = vec3.scale(0.3F);
            }

            return Optional.of(new Leashable.Wrench(vec3, d1));
        }
    }

    default boolean supportQuadLeash() {
        return false;
    }

    default Vec3[] getQuadLeashOffsets() {
        return createQuadLeashOffsets((Entity)this, 0.0, 0.5, 0.5, 0.5);
    }

    static Vec3[] createQuadLeashOffsets(Entity pEntity, double pZOffset, double pZ, double pX, double pY) {
        float f = pEntity.getBbWidth();
        double d0 = pZOffset * f;
        double d1 = pZ * f;
        double d2 = pX * f;
        double d3 = pY * pEntity.getBbHeight();
        return new Vec3[]{new Vec3(-d2, d3, d1 + d0), new Vec3(-d2, d3, -d1 + d0), new Vec3(d2, d3, -d1 + d0), new Vec3(d2, d3, d1 + d0)};
    }

    default Vec3 getLeashOffset(float pPartialTick) {
        return this.getLeashOffset();
    }

    default Vec3 getLeashOffset() {
        Entity entity = (Entity)this;
        return new Vec3(0.0, entity.getEyeHeight(), entity.getBbWidth() * 0.4F);
    }

    default void setLeashedTo(Entity pLeashHolder, boolean pBroadcastPacket) {
        if (this != pLeashHolder) {
            setLeashedTo((Entity & Leashable)this, pLeashHolder, pBroadcastPacket);
        }
    }

    private static <E extends Entity & Leashable> void setLeashedTo(E pEntity, Entity pLeashHolder, boolean pBroadcastPacket) {
        Leashable.LeashData leashable$leashdata = pEntity.getLeashData();
        if (leashable$leashdata == null) {
            leashable$leashdata = new Leashable.LeashData(pLeashHolder);
            pEntity.setLeashData(leashable$leashdata);
        } else {
            Entity entity = leashable$leashdata.leashHolder;
            leashable$leashdata.setLeashHolder(pLeashHolder);
            if (entity != null && entity != pLeashHolder) {
                entity.notifyLeasheeRemoved(pEntity);
            }
        }

        if (pBroadcastPacket && pEntity.level() instanceof ServerLevel serverlevel) {
            serverlevel.getChunkSource().broadcast(pEntity, new ClientboundSetEntityLinkPacket(pEntity, pLeashHolder));
        }

        if (pEntity.isPassenger()) {
            pEntity.stopRiding();
        }
    }

    @Nullable
    default Entity getLeashHolder() {
        return getLeashHolder((Entity & Leashable)this);
    }

    @Nullable
    private static <E extends Entity & Leashable> Entity getLeashHolder(E pEntity) {
        Leashable.LeashData leashable$leashdata = pEntity.getLeashData();
        if (leashable$leashdata == null) {
            return null;
        } else {
            if (leashable$leashdata.delayedLeashHolderId != 0 && pEntity.level().isClientSide) {
                Entity entity = pEntity.level().getEntity(leashable$leashdata.delayedLeashHolderId);
                if (entity instanceof Entity) {
                    leashable$leashdata.setLeashHolder(entity);
                }
            }

            return leashable$leashdata.leashHolder;
        }
    }

    static List<Leashable> leashableLeashedTo(Entity pEntity) {
        return leashableInArea(pEntity, p_409624_ -> p_409624_.getLeashHolder() == pEntity);
    }

    static List<Leashable> leashableInArea(Entity pEntity, Predicate<Leashable> pPredicate) {
        return leashableInArea(pEntity.level(), pEntity.getBoundingBox().getCenter(), pPredicate);
    }

    static List<Leashable> leashableInArea(Level pLevel, Vec3 pPos, Predicate<Leashable> pPredicate) {
        double d0 = 32.0;
        AABB aabb = AABB.ofSize(pPos, 32.0, 32.0, 32.0);
        return pLevel.getEntitiesOfClass(Entity.class, aabb, p_410526_ -> p_410526_ instanceof Leashable leashable && pPredicate.test(leashable))
            .stream()
            .map(Leashable.class::cast)
            .toList();
    }

    public static final class LeashData {
        public static final Codec<Leashable.LeashData> CODEC = Codec.xor(UUIDUtil.CODEC.fieldOf("UUID").codec(), BlockPos.CODEC)
            .xmap(
                Leashable.LeashData::new,
                p_394128_ -> {
                    if (p_394128_.leashHolder instanceof LeashFenceKnotEntity leashfenceknotentity) {
                        return Either.right(leashfenceknotentity.getPos());
                    } else {
                        return p_394128_.leashHolder != null
                            ? Either.left(p_394128_.leashHolder.getUUID())
                            : Objects.requireNonNull(p_394128_.delayedLeashInfo, "Invalid LeashData had no attachment");
                    }
                }
            );
        int delayedLeashHolderId;
        @Nullable
        public Entity leashHolder;
        @Nullable
        public Either<UUID, BlockPos> delayedLeashInfo;
        public double angularMomentum;

        private LeashData(Either<UUID, BlockPos> pDelayedLeashInfo) {
            this.delayedLeashInfo = pDelayedLeashInfo;
        }

        LeashData(Entity pLeashHolder) {
            this.leashHolder = pLeashHolder;
        }

        LeashData(int pDelayedLeashInfoId) {
            this.delayedLeashHolderId = pDelayedLeashInfoId;
        }

        public void setLeashHolder(Entity pLeashHolder) {
            this.leashHolder = pLeashHolder;
            this.delayedLeashInfo = null;
            this.delayedLeashHolderId = 0;
        }
    }

    public record Wrench(Vec3 force, double torque) {
        static Leashable.Wrench ZERO = new Leashable.Wrench(Vec3.ZERO, 0.0);

        static double torqueFromForce(Vec3 pAttachmentPoint, Vec3 pForce) {
            return pAttachmentPoint.z * pForce.x - pAttachmentPoint.x * pForce.z;
        }

        static Leashable.Wrench accumulate(List<Leashable.Wrench> pWrenches) {
            if (pWrenches.isEmpty()) {
                return ZERO;
            } else {
                double d0 = 0.0;
                double d1 = 0.0;
                double d2 = 0.0;
                double d3 = 0.0;

                for (Leashable.Wrench leashable$wrench : pWrenches) {
                    Vec3 vec3 = leashable$wrench.force;
                    d0 += vec3.x;
                    d1 += vec3.y;
                    d2 += vec3.z;
                    d3 += leashable$wrench.torque;
                }

                return new Leashable.Wrench(new Vec3(d0, d1, d2), d3);
            }
        }

        public Leashable.Wrench scale(double pScale) {
            return new Leashable.Wrench(this.force.scale(pScale), this.torque * pScale);
        }
    }
}
