package net.minecraft.world.entity.animal;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LandOnOwnersShoulderGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class Parrot extends ShoulderRidingEntity implements FlyingAnimal {
    private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(Parrot.class, EntityDataSerializers.INT);
    private static final Predicate<Mob> NOT_PARROT_PREDICATE = new Predicate<Mob>() {
        public boolean test(@Nullable Mob p_29453_) {
            return p_29453_ != null && Parrot.MOB_SOUND_MAP.containsKey(p_29453_.getType());
        }
    };
    static final Map<EntityType<?>, SoundEvent> MOB_SOUND_MAP = Util.make(Maps.newHashMap(), p_405459_ -> {
        p_405459_.put(EntityType.BLAZE, SoundEvents.PARROT_IMITATE_BLAZE);
        p_405459_.put(EntityType.BOGGED, SoundEvents.PARROT_IMITATE_BOGGED);
        p_405459_.put(EntityType.BREEZE, SoundEvents.PARROT_IMITATE_BREEZE);
        p_405459_.put(EntityType.CAVE_SPIDER, SoundEvents.PARROT_IMITATE_SPIDER);
        p_405459_.put(EntityType.CREAKING, SoundEvents.PARROT_IMITATE_CREAKING);
        p_405459_.put(EntityType.CREEPER, SoundEvents.PARROT_IMITATE_CREEPER);
        p_405459_.put(EntityType.DROWNED, SoundEvents.PARROT_IMITATE_DROWNED);
        p_405459_.put(EntityType.ELDER_GUARDIAN, SoundEvents.PARROT_IMITATE_ELDER_GUARDIAN);
        p_405459_.put(EntityType.ENDER_DRAGON, SoundEvents.PARROT_IMITATE_ENDER_DRAGON);
        p_405459_.put(EntityType.ENDERMITE, SoundEvents.PARROT_IMITATE_ENDERMITE);
        p_405459_.put(EntityType.EVOKER, SoundEvents.PARROT_IMITATE_EVOKER);
        p_405459_.put(EntityType.GHAST, SoundEvents.PARROT_IMITATE_GHAST);
        p_405459_.put(EntityType.HAPPY_GHAST, SoundEvents.EMPTY);
        p_405459_.put(EntityType.GUARDIAN, SoundEvents.PARROT_IMITATE_GUARDIAN);
        p_405459_.put(EntityType.HOGLIN, SoundEvents.PARROT_IMITATE_HOGLIN);
        p_405459_.put(EntityType.HUSK, SoundEvents.PARROT_IMITATE_HUSK);
        p_405459_.put(EntityType.ILLUSIONER, SoundEvents.PARROT_IMITATE_ILLUSIONER);
        p_405459_.put(EntityType.MAGMA_CUBE, SoundEvents.PARROT_IMITATE_MAGMA_CUBE);
        p_405459_.put(EntityType.PHANTOM, SoundEvents.PARROT_IMITATE_PHANTOM);
        p_405459_.put(EntityType.PIGLIN, SoundEvents.PARROT_IMITATE_PIGLIN);
        p_405459_.put(EntityType.PIGLIN_BRUTE, SoundEvents.PARROT_IMITATE_PIGLIN_BRUTE);
        p_405459_.put(EntityType.PILLAGER, SoundEvents.PARROT_IMITATE_PILLAGER);
        p_405459_.put(EntityType.RAVAGER, SoundEvents.PARROT_IMITATE_RAVAGER);
        p_405459_.put(EntityType.SHULKER, SoundEvents.PARROT_IMITATE_SHULKER);
        p_405459_.put(EntityType.SILVERFISH, SoundEvents.PARROT_IMITATE_SILVERFISH);
        p_405459_.put(EntityType.SKELETON, SoundEvents.PARROT_IMITATE_SKELETON);
        p_405459_.put(EntityType.SLIME, SoundEvents.PARROT_IMITATE_SLIME);
        p_405459_.put(EntityType.SPIDER, SoundEvents.PARROT_IMITATE_SPIDER);
        p_405459_.put(EntityType.STRAY, SoundEvents.PARROT_IMITATE_STRAY);
        p_405459_.put(EntityType.VEX, SoundEvents.PARROT_IMITATE_VEX);
        p_405459_.put(EntityType.VINDICATOR, SoundEvents.PARROT_IMITATE_VINDICATOR);
        p_405459_.put(EntityType.WARDEN, SoundEvents.PARROT_IMITATE_WARDEN);
        p_405459_.put(EntityType.WITCH, SoundEvents.PARROT_IMITATE_WITCH);
        p_405459_.put(EntityType.WITHER, SoundEvents.PARROT_IMITATE_WITHER);
        p_405459_.put(EntityType.WITHER_SKELETON, SoundEvents.PARROT_IMITATE_WITHER_SKELETON);
        p_405459_.put(EntityType.ZOGLIN, SoundEvents.PARROT_IMITATE_ZOGLIN);
        p_405459_.put(EntityType.ZOMBIE, SoundEvents.PARROT_IMITATE_ZOMBIE);
        p_405459_.put(EntityType.ZOMBIE_VILLAGER, SoundEvents.PARROT_IMITATE_ZOMBIE_VILLAGER);
    });
    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    private float flapping = 1.0F;
    private float nextFlap = 1.0F;
    private boolean partyParrot;
    @Nullable
    private BlockPos jukebox;

    public Parrot(EntityType<? extends Parrot> p_29362_, Level p_29363_) {
        super(p_29362_, p_29363_);
        this.moveControl = new FlyingMoveControl(this, 10, false);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.COCOA, -1.0F);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_29389_, DifficultyInstance p_29390_, EntitySpawnReason p_366524_, @Nullable SpawnGroupData p_29392_) {
        this.setVariant(Util.getRandom(Parrot.Variant.values(), p_29389_.getRandom()));
        if (p_29392_ == null) {
            p_29392_ = new AgeableMob.AgeableMobGroupData(false);
        }

        return super.finalizeSpawn(p_29389_, p_29390_, p_366524_, p_29392_);
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new TamableAnimal.TamableAnimalPanicGoal(1.25));
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0, 5.0F, 1.0F));
        this.goalSelector.addGoal(2, new Parrot.ParrotWanderGoal(this, 1.0));
        this.goalSelector.addGoal(3, new LandOnOwnersShoulderGoal(this));
        this.goalSelector.addGoal(3, new FollowMobGoal(this, 1.0, 3.0F, 7.0F));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 6.0)
            .add(Attributes.FLYING_SPEED, 0.4F)
            .add(Attributes.MOVEMENT_SPEED, 0.2F)
            .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, pLevel);
        flyingpathnavigation.setCanOpenDoors(false);
        flyingpathnavigation.setCanFloat(true);
        return flyingpathnavigation;
    }

    @Override
    public void aiStep() {
        if (this.jukebox == null || !this.jukebox.closerToCenterThan(this.position(), 3.46) || !this.level().getBlockState(this.jukebox).is(Blocks.JUKEBOX)) {
            this.partyParrot = false;
            this.jukebox = null;
        }

        if (this.level().random.nextInt(400) == 0) {
            imitateNearbyMobs(this.level(), this);
        }

        super.aiStep();
        this.calculateFlapping();
    }

    @Override
    public void setRecordPlayingNearby(BlockPos pPos, boolean pIsPartying) {
        this.jukebox = pPos;
        this.partyParrot = pIsPartying;
    }

    public boolean isPartyParrot() {
        return this.partyParrot;
    }

    private void calculateFlapping() {
        this.oFlap = this.flap;
        this.oFlapSpeed = this.flapSpeed;
        this.flapSpeed = this.flapSpeed + (!this.onGround() && !this.isPassenger() ? 4 : -1) * 0.3F;
        this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0F, 1.0F);
        if (!this.onGround() && this.flapping < 1.0F) {
            this.flapping = 1.0F;
        }

        this.flapping *= 0.9F;
        Vec3 vec3 = this.getDeltaMovement();
        if (!this.onGround() && vec3.y < 0.0) {
            this.setDeltaMovement(vec3.multiply(1.0, 0.6, 1.0));
        }

        this.flap = this.flap + this.flapping * 2.0F;
    }

    public static boolean imitateNearbyMobs(Level pLevel, Entity pParrot) {
        if (pParrot.isAlive() && !pParrot.isSilent() && pLevel.random.nextInt(2) == 0) {
            List<Mob> list = pLevel.getEntitiesOfClass(Mob.class, pParrot.getBoundingBox().inflate(20.0), NOT_PARROT_PREDICATE);
            if (!list.isEmpty()) {
                Mob mob = list.get(pLevel.random.nextInt(list.size()));
                if (!mob.isSilent()) {
                    SoundEvent soundevent = getImitatedSound(mob.getType());
                    pLevel.playSound(
                        null, pParrot.getX(), pParrot.getY(), pParrot.getZ(), soundevent, pParrot.getSoundSource(), 0.7F, getPitch(pLevel.random)
                    );
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (!this.isTame() && itemstack.is(ItemTags.PARROT_FOOD)) {
            this.usePlayerItem(pPlayer, pHand, itemstack);
            if (!this.isSilent()) {
                this.level()
                    .playSound(
                        null,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        SoundEvents.PARROT_EAT,
                        this.getSoundSource(),
                        1.0F,
                        1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
                    );
            }

            if (!this.level().isClientSide) {
                if (this.random.nextInt(10) == 0 && !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, pPlayer)) {
                    this.tame(pPlayer);
                    this.level().broadcastEntityEvent(this, (byte)7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte)6);
                }
            }

            return InteractionResult.SUCCESS;
        } else if (!itemstack.is(ItemTags.PARROT_POISONOUS_FOOD)) {
            if (!this.isFlying() && this.isTame() && this.isOwnedBy(pPlayer)) {
                if (!this.level().isClientSide) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                }

                return InteractionResult.SUCCESS;
            } else {
                return super.mobInteract(pPlayer, pHand);
            }
        } else {
            this.usePlayerItem(pPlayer, pHand, itemstack);
            this.addEffect(new MobEffectInstance(MobEffects.POISON, 900));
            if (pPlayer.isCreative() || !this.isInvulnerable()) {
                this.hurt(this.damageSources().playerAttack(pPlayer), Float.MAX_VALUE);
            }

            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public boolean isFood(ItemStack pStack) {
        return false;
    }

    public static boolean checkParrotSpawnRules(
        EntityType<Parrot> pEntityType, LevelAccessor pLevel, EntitySpawnReason pSpawnReason, BlockPos pPos, RandomSource pRandom
    ) {
        return pLevel.getBlockState(pPos.below()).is(BlockTags.PARROTS_SPAWNABLE_ON) && isBrightEnoughToSpawn(pLevel, pPos);
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
    }

    @Override
    public boolean canMate(Animal pOtherAnimal) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_148993_, AgeableMob p_148994_) {
        return null;
    }

    @Nullable
    @Override
    public SoundEvent getAmbientSound() {
        return getAmbient(this.level(), this.level().random);
    }

    public static SoundEvent getAmbient(Level pLevel, RandomSource pRandom) {
        if (pLevel.getDifficulty() != Difficulty.PEACEFUL && pRandom.nextInt(1000) == 0) {
            List<EntityType<?>> list = Lists.newArrayList(MOB_SOUND_MAP.keySet());
            return getImitatedSound(list.get(pRandom.nextInt(list.size())));
        } else {
            return SoundEvents.PARROT_AMBIENT;
        }
    }

    private static SoundEvent getImitatedSound(EntityType<?> pType) {
        return MOB_SOUND_MAP.getOrDefault(pType, SoundEvents.PARROT_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pBlock) {
        this.playSound(SoundEvents.PARROT_STEP, 0.15F, 1.0F);
    }

    @Override
    protected boolean isFlapping() {
        return this.flyDist > this.nextFlap;
    }

    @Override
    protected void onFlap() {
        this.playSound(SoundEvents.PARROT_FLY, 0.15F, 1.0F);
        this.nextFlap = this.flyDist + this.flapSpeed / 2.0F;
    }

    @Override
    public float getVoicePitch() {
        return getPitch(this.random);
    }

    public static float getPitch(RandomSource pRandom) {
        return (pRandom.nextFloat() - pRandom.nextFloat()) * 0.2F + 1.0F;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected void doPush(Entity pEntity) {
        if (!(pEntity instanceof Player)) {
            super.doPush(pEntity);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel p_368472_, DamageSource p_364880_, float p_366649_) {
        if (this.isInvulnerableTo(p_368472_, p_364880_)) {
            return false;
        } else {
            this.setOrderedToSit(false);
            return super.hurtServer(p_368472_, p_364880_, p_366649_);
        }
    }

    public Parrot.Variant getVariant() {
        return Parrot.Variant.byId(this.entityData.get(DATA_VARIANT_ID));
    }

    private void setVariant(Parrot.Variant pVariant) {
        this.entityData.set(DATA_VARIANT_ID, pVariant.id);
    }

    @Nullable
    @Override
    public <T> T get(DataComponentType<? extends T> p_397996_) {
        return p_397996_ == DataComponents.PARROT_VARIANT ? castComponentValue((DataComponentType<T>)p_397996_, this.getVariant()) : super.get(p_397996_);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_397312_) {
        this.applyImplicitComponentIfPresent(p_397312_, DataComponents.PARROT_VARIANT);
        super.applyImplicitComponents(p_397312_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_394047_, T p_397115_) {
        if (p_394047_ == DataComponents.PARROT_VARIANT) {
            this.setVariant(castComponentValue(DataComponents.PARROT_VARIANT, p_397115_));
            return true;
        } else {
            return super.applyImplicitComponent(p_394047_, p_397115_);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_335317_) {
        super.defineSynchedData(p_335317_);
        p_335317_.define(DATA_VARIANT_ID, Parrot.Variant.DEFAULT.id);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_407726_) {
        super.addAdditionalSaveData(p_407726_);
        p_407726_.store("Variant", Parrot.Variant.LEGACY_CODEC, this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_405878_) {
        super.readAdditionalSaveData(p_405878_);
        this.setVariant(p_405878_.read("Variant", Parrot.Variant.LEGACY_CODEC).orElse(Parrot.Variant.DEFAULT));
    }

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    protected boolean canFlyToOwner() {
        return true;
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.5F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
    }

    static class ParrotWanderGoal extends WaterAvoidingRandomFlyingGoal {
        public ParrotWanderGoal(PathfinderMob p_186224_, double p_186225_) {
            super(p_186224_, p_186225_);
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            Vec3 vec3 = null;
            if (this.mob.isInWater()) {
                vec3 = LandRandomPos.getPos(this.mob, 15, 15);
            }

            if (this.mob.getRandom().nextFloat() >= this.probability) {
                vec3 = this.getTreePos();
            }

            return vec3 == null ? super.getPosition() : vec3;
        }

        @Nullable
        private Vec3 getTreePos() {
            BlockPos blockpos = this.mob.blockPosition();
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();

            for (BlockPos blockpos1 : BlockPos.betweenClosed(
                Mth.floor(this.mob.getX() - 3.0),
                Mth.floor(this.mob.getY() - 6.0),
                Mth.floor(this.mob.getZ() - 3.0),
                Mth.floor(this.mob.getX() + 3.0),
                Mth.floor(this.mob.getY() + 6.0),
                Mth.floor(this.mob.getZ() + 3.0)
            )) {
                if (!blockpos.equals(blockpos1)) {
                    BlockState blockstate = this.mob.level().getBlockState(blockpos$mutableblockpos1.setWithOffset(blockpos1, Direction.DOWN));
                    boolean flag = blockstate.getBlock() instanceof LeavesBlock || blockstate.is(BlockTags.LOGS);
                    if (flag
                        && this.mob.level().isEmptyBlock(blockpos1)
                        && this.mob.level().isEmptyBlock(blockpos$mutableblockpos.setWithOffset(blockpos1, Direction.UP))) {
                        return Vec3.atBottomCenterOf(blockpos1);
                    }
                }
            }

            return null;
        }
    }

    public static enum Variant implements StringRepresentable {
        RED_BLUE(0, "red_blue"),
        BLUE(1, "blue"),
        GREEN(2, "green"),
        YELLOW_BLUE(3, "yellow_blue"),
        GRAY(4, "gray");

        public static final Parrot.Variant DEFAULT = RED_BLUE;
        private static final IntFunction<Parrot.Variant> BY_ID = ByIdMap.continuous(Parrot.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
        public static final Codec<Parrot.Variant> CODEC = StringRepresentable.fromEnum(Parrot.Variant::values);
        @Deprecated
        public static final Codec<Parrot.Variant> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, Parrot.Variant::getId);
        public static final StreamCodec<ByteBuf, Parrot.Variant> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Parrot.Variant::getId);
        final int id;
        private final String name;

        private Variant(final int pId, final String pName) {
            this.id = pId;
            this.name = pName;
        }

        public int getId() {
            return this.id;
        }

        public static Parrot.Variant byId(int pId) {
            return BY_ID.apply(pId);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
