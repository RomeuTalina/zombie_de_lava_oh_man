package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Ghast extends Mob implements Enemy {
    private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING = SynchedEntityData.defineId(Ghast.class, EntityDataSerializers.BOOLEAN);
    private static final byte DEFAULT_EXPLOSION_POWER = 1;
    private int explosionPower = 1;

    public Ghast(EntityType<? extends Ghast> p_32725_, Level p_32726_) {
        super(p_32725_, p_32726_);
        this.xpReward = 5;
        this.moveControl = new Ghast.GhastMoveControl(this, false, () -> false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new Ghast.RandomFloatAroundGoal(this));
        this.goalSelector.addGoal(7, new Ghast.GhastLookGoal(this));
        this.goalSelector.addGoal(7, new Ghast.GhastShootFireballGoal(this));
        this.targetSelector
            .addGoal(
                1,
                new NearestAttackableTargetGoal<>(
                    this, Player.class, 10, true, false, (p_405504_, p_405505_) -> Math.abs(p_405504_.getY() - this.getY()) <= 4.0
                )
            );
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_IS_CHARGING);
    }

    public void setCharging(boolean pCharging) {
        this.entityData.set(DATA_IS_CHARGING, pCharging);
    }

    public int getExplosionPower() {
        return this.explosionPower;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    private static boolean isReflectedFireball(DamageSource pDamageSource) {
        return pDamageSource.getDirectEntity() instanceof LargeFireball && pDamageSource.getEntity() instanceof Player;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel p_363213_, DamageSource p_238289_) {
        return this.isInvulnerable() && !p_238289_.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !isReflectedFireball(p_238289_) && super.isInvulnerableTo(p_363213_, p_238289_);
    }

    @Override
    protected void checkFallDamage(double p_410052_, boolean p_410300_, BlockState p_407336_, BlockPos p_409059_) {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public void travel(Vec3 p_407013_) {
        this.travelFlying(p_407013_, 0.02F);
    }

    @Override
    public boolean hurtServer(ServerLevel p_365264_, DamageSource p_366880_, float p_369426_) {
        if (isReflectedFireball(p_366880_)) {
            super.hurtServer(p_365264_, p_366880_, 1000.0F);
            return true;
        } else {
            return this.isInvulnerableTo(p_365264_, p_366880_) ? false : super.hurtServer(p_365264_, p_366880_, p_369426_);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_334321_) {
        super.defineSynchedData(p_334321_);
        p_334321_.define(DATA_IS_CHARGING, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.FOLLOW_RANGE, 100.0)
            .add(Attributes.CAMERA_DISTANCE, 8.0)
            .add(Attributes.FLYING_SPEED, 0.06);
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GHAST_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.GHAST_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GHAST_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 5.0F;
    }

    public static boolean checkGhastSpawnRules(
        EntityType<Ghast> pEntityType, LevelAccessor pLevel, EntitySpawnReason pSpawnReason, BlockPos pPos, RandomSource pRandom
    ) {
        return pLevel.getDifficulty() != Difficulty.PEACEFUL && pRandom.nextInt(20) == 0 && checkMobSpawnRules(pEntityType, pLevel, pSpawnReason, pPos, pRandom);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_407589_) {
        super.addAdditionalSaveData(p_407589_);
        p_407589_.putByte("ExplosionPower", (byte)this.explosionPower);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_409268_) {
        super.readAdditionalSaveData(p_409268_);
        this.explosionPower = p_409268_.getByteOr("ExplosionPower", (byte)1);
    }

    @Override
    public boolean supportQuadLeashAsHolder() {
        return true;
    }

    @Override
    public double leashElasticDistance() {
        return 10.0;
    }

    @Override
    public double leashSnapDistance() {
        return 16.0;
    }

    public static void faceMovementDirection(Mob pMob) {
        if (pMob.getTarget() == null) {
            Vec3 vec3 = pMob.getDeltaMovement();
            pMob.setYRot(-((float)Mth.atan2(vec3.x, vec3.z)) * (180.0F / (float)Math.PI));
            pMob.yBodyRot = pMob.getYRot();
        } else {
            LivingEntity livingentity = pMob.getTarget();
            double d0 = 64.0;
            if (livingentity.distanceToSqr(pMob) < 4096.0) {
                double d1 = livingentity.getX() - pMob.getX();
                double d2 = livingentity.getZ() - pMob.getZ();
                pMob.setYRot(-((float)Mth.atan2(d1, d2)) * (180.0F / (float)Math.PI));
                pMob.yBodyRot = pMob.getYRot();
            }
        }
    }

    public static class GhastLookGoal extends Goal {
        private final Mob ghast;

        public GhastLookGoal(Mob pGhast) {
            this.ghast = pGhast;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Ghast.faceMovementDirection(this.ghast);
        }
    }

    public static class GhastMoveControl extends MoveControl {
        private final Mob ghast;
        private int floatDuration;
        private final boolean careful;
        private final BooleanSupplier shouldBeStopped;

        public GhastMoveControl(Mob pGhast, boolean pCareful, BooleanSupplier pShouldBeStopped) {
            super(pGhast);
            this.ghast = pGhast;
            this.careful = pCareful;
            this.shouldBeStopped = pShouldBeStopped;
        }

        @Override
        public void tick() {
            if (this.shouldBeStopped.getAsBoolean()) {
                this.operation = MoveControl.Operation.WAIT;
                this.ghast.stopInPlace();
            }

            if (this.operation == MoveControl.Operation.MOVE_TO) {
                if (this.floatDuration-- <= 0) {
                    this.floatDuration = this.floatDuration + this.ghast.getRandom().nextInt(5) + 2;
                    Vec3 vec3 = new Vec3(
                        this.wantedX - this.ghast.getX(), this.wantedY - this.ghast.getY(), this.wantedZ - this.ghast.getZ()
                    );
                    if (this.canReach(vec3)) {
                        this.ghast
                            .setDeltaMovement(this.ghast.getDeltaMovement().add(vec3.normalize().scale(this.ghast.getAttributeValue(Attributes.FLYING_SPEED) * 5.0 / 3.0)));
                    } else {
                        this.operation = MoveControl.Operation.WAIT;
                    }
                }
            }
        }

        private boolean canReach(Vec3 pDelta) {
            AABB aabb = this.ghast.getBoundingBox();
            AABB aabb1 = aabb.move(pDelta);
            if (this.careful) {
                for (BlockPos blockpos : BlockPos.betweenClosed(aabb1.inflate(1.0))) {
                    if (!this.blockTraversalPossible(this.ghast.level(), null, null, blockpos, false, false)) {
                        return false;
                    }
                }
            }

            boolean flag = this.ghast.isInWater();
            boolean flag1 = this.ghast.isInLava();
            Vec3 vec3 = this.ghast.position();
            Vec3 vec31 = vec3.add(pDelta);
            return BlockGetter.forEachBlockIntersectedBetween(
                vec3,
                vec31,
                aabb1,
                (p_408139_, p_408003_) -> aabb.intersects(p_408139_) ? true : this.blockTraversalPossible(this.ghast.level(), vec3, vec31, p_408139_, flag, flag1)
            );
        }

        private boolean blockTraversalPossible(
            BlockGetter pLevel, @Nullable Vec3 pFrom, @Nullable Vec3 pTo, BlockPos pPos, boolean pIsInLava, boolean pIsInWater
        ) {
            BlockState blockstate = pLevel.getBlockState(pPos);
            if (blockstate.isAir()) {
                return true;
            } else {
                boolean flag = pFrom != null && pTo != null;
                boolean flag1 = flag
                    ? !this.ghast.collidedWithShapeMovingFrom(pFrom, pTo, blockstate.getCollisionShape(pLevel, pPos).move(new Vec3(pPos)).toAabbs())
                    : blockstate.getCollisionShape(pLevel, pPos).isEmpty();
                if (!this.careful) {
                    return flag1;
                } else if (blockstate.is(BlockTags.HAPPY_GHAST_AVOIDS)) {
                    return false;
                } else {
                    FluidState fluidstate = pLevel.getFluidState(pPos);
                    if (!fluidstate.isEmpty() && (!flag || this.ghast.collidedWithFluid(fluidstate, pPos, pFrom, pTo))) {
                        if (fluidstate.is(FluidTags.WATER)) {
                            return pIsInLava;
                        }

                        if (fluidstate.is(FluidTags.LAVA)) {
                            return pIsInWater;
                        }
                    }

                    return flag1;
                }
            }
        }
    }

    static class GhastShootFireballGoal extends Goal {
        private final Ghast ghast;
        public int chargeTime;

        public GhastShootFireballGoal(Ghast pGhast) {
            this.ghast = pGhast;
        }

        @Override
        public boolean canUse() {
            return this.ghast.getTarget() != null;
        }

        @Override
        public void start() {
            this.chargeTime = 0;
        }

        @Override
        public void stop() {
            this.ghast.setCharging(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity livingentity = this.ghast.getTarget();
            if (livingentity != null) {
                double d0 = 64.0;
                if (livingentity.distanceToSqr(this.ghast) < 4096.0 && this.ghast.hasLineOfSight(livingentity)) {
                    Level level = this.ghast.level();
                    this.chargeTime++;
                    if (this.chargeTime == 10 && !this.ghast.isSilent()) {
                        level.levelEvent(null, 1015, this.ghast.blockPosition(), 0);
                    }

                    if (this.chargeTime == 20) {
                        double d1 = 4.0;
                        Vec3 vec3 = this.ghast.getViewVector(1.0F);
                        double d2 = livingentity.getX() - (this.ghast.getX() + vec3.x * 4.0);
                        double d3 = livingentity.getY(0.5) - (0.5 + this.ghast.getY(0.5));
                        double d4 = livingentity.getZ() - (this.ghast.getZ() + vec3.z * 4.0);
                        Vec3 vec31 = new Vec3(d2, d3, d4);
                        if (!this.ghast.isSilent()) {
                            level.levelEvent(null, 1016, this.ghast.blockPosition(), 0);
                        }

                        LargeFireball largefireball = new LargeFireball(level, this.ghast, vec31.normalize(), this.ghast.getExplosionPower());
                        largefireball.setPos(
                            this.ghast.getX() + vec3.x * 4.0, this.ghast.getY(0.5) + 0.5, largefireball.getZ() + vec3.z * 4.0
                        );
                        level.addFreshEntity(largefireball);
                        this.chargeTime = -40;
                    }
                } else if (this.chargeTime > 0) {
                    this.chargeTime--;
                }

                this.ghast.setCharging(this.chargeTime > 10);
            }
        }
    }

    public static class RandomFloatAroundGoal extends Goal {
        private static final int MAX_ATTEMPTS = 64;
        private final Mob ghast;
        private final int distanceToBlocks;

        public RandomFloatAroundGoal(Mob pGhast) {
            this(pGhast, 0);
        }

        public RandomFloatAroundGoal(Mob pGhast, int pDistanceToBlocks) {
            this.ghast = pGhast;
            this.distanceToBlocks = pDistanceToBlocks;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            MoveControl movecontrol = this.ghast.getMoveControl();
            if (!movecontrol.hasWanted()) {
                return true;
            } else {
                double d0 = movecontrol.getWantedX() - this.ghast.getX();
                double d1 = movecontrol.getWantedY() - this.ghast.getY();
                double d2 = movecontrol.getWantedZ() - this.ghast.getZ();
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                return d3 < 1.0 || d3 > 3600.0;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            Vec3 vec3 = getSuitableFlyToPosition(this.ghast, this.distanceToBlocks);
            this.ghast.getMoveControl().setWantedPosition(vec3.x(), vec3.y(), vec3.z(), 1.0);
        }

        public static Vec3 getSuitableFlyToPosition(Mob pMob, int pDistanceToBlocks) {
            Level level = pMob.level();
            RandomSource randomsource = pMob.getRandom();
            Vec3 vec3 = pMob.position();
            Vec3 vec31 = null;

            for (int i = 0; i < 64; i++) {
                vec31 = chooseRandomPositionWithRestriction(pMob, vec3, randomsource);
                if (vec31 != null && isGoodTarget(level, vec31, pDistanceToBlocks)) {
                    return vec31;
                }
            }

            if (vec31 == null) {
                vec31 = chooseRandomPosition(vec3, randomsource);
            }

            BlockPos blockpos = BlockPos.containing(vec31);
            int j = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockpos.getX(), blockpos.getZ());
            if (j < blockpos.getY() && j > level.getMinY()) {
                vec31 = new Vec3(vec31.x(), pMob.getY() - Math.abs(pMob.getY() - vec31.y()), vec31.z());
            }

            return vec31;
        }

        private static boolean isGoodTarget(Level pLevel, Vec3 pPos, int pDistanceToBlocks) {
            if (pDistanceToBlocks <= 0) {
                return true;
            } else {
                BlockPos blockpos = BlockPos.containing(pPos);
                if (!pLevel.getBlockState(blockpos).isAir()) {
                    return false;
                } else {
                    for (Direction direction : Direction.values()) {
                        for (int i = 1; i < pDistanceToBlocks; i++) {
                            BlockPos blockpos1 = blockpos.relative(direction, i);
                            if (!pLevel.getBlockState(blockpos1).isAir()) {
                                return true;
                            }
                        }
                    }

                    return false;
                }
            }
        }

        private static Vec3 chooseRandomPosition(Vec3 pPos, RandomSource pRandom) {
            double d0 = pPos.x() + (pRandom.nextFloat() * 2.0F - 1.0F) * 16.0F;
            double d1 = pPos.y() + (pRandom.nextFloat() * 2.0F - 1.0F) * 16.0F;
            double d2 = pPos.z() + (pRandom.nextFloat() * 2.0F - 1.0F) * 16.0F;
            return new Vec3(d0, d1, d2);
        }

        @Nullable
        private static Vec3 chooseRandomPositionWithRestriction(Mob pMob, Vec3 pPos, RandomSource pRandom) {
            Vec3 vec3 = chooseRandomPosition(pPos, pRandom);
            return pMob.hasHome() && !pMob.isWithinHome(vec3) ? null : vec3;
        }
    }
}