package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ConduitBlockEntity extends BlockEntity {
    private static final int BLOCK_REFRESH_RATE = 2;
    private static final int EFFECT_DURATION = 13;
    private static final float ROTATION_SPEED = -0.0375F;
    private static final int MIN_ACTIVE_SIZE = 16;
    private static final int MIN_KILL_SIZE = 42;
    private static final int KILL_RANGE = 8;
    private static final Block[] VALID_BLOCKS = new Block[]{Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN, Blocks.DARK_PRISMARINE};
    public int tickCount;
    private float activeRotation;
    private boolean isActive;
    private boolean isHunting;
    private final List<BlockPos> effectBlocks = Lists.newArrayList();
    @Nullable
    private EntityReference<LivingEntity> destroyTarget;
    private long nextAmbientSoundActivation;

    public ConduitBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.CONDUIT, pPos, pBlockState);
    }

    @Override
    protected void loadAdditional(ValueInput p_405930_) {
        super.loadAdditional(p_405930_);
        this.destroyTarget = EntityReference.read(p_405930_, "Target");
    }

    @Override
    protected void saveAdditional(ValueOutput p_409807_) {
        super.saveAdditional(p_409807_);
        EntityReference.store(this.destroyTarget, p_409807_, "Target");
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_327672_) {
        return this.saveCustomOnly(p_327672_);
    }

    public static void clientTick(Level pLevel, BlockPos pPos, BlockState pState, ConduitBlockEntity pBlockEntity) {
        pBlockEntity.tickCount++;
        long i = pLevel.getGameTime();
        List<BlockPos> list = pBlockEntity.effectBlocks;
        if (i % 40L == 0L) {
            pBlockEntity.isActive = updateShape(pLevel, pPos, list);
            updateHunting(pBlockEntity, list);
        }

        LivingEntity livingentity = EntityReference.get(pBlockEntity.destroyTarget, pLevel, LivingEntity.class);
        animationTick(pLevel, pPos, list, livingentity, pBlockEntity.tickCount);
        if (pBlockEntity.isActive()) {
            pBlockEntity.activeRotation++;
        }
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, ConduitBlockEntity pBlockEntity) {
        pBlockEntity.tickCount++;
        long i = pLevel.getGameTime();
        List<BlockPos> list = pBlockEntity.effectBlocks;
        if (i % 40L == 0L) {
            boolean flag = updateShape(pLevel, pPos, list);
            if (flag != pBlockEntity.isActive) {
                SoundEvent soundevent = flag ? SoundEvents.CONDUIT_ACTIVATE : SoundEvents.CONDUIT_DEACTIVATE;
                pLevel.playSound(null, pPos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            pBlockEntity.isActive = flag;
            updateHunting(pBlockEntity, list);
            if (flag) {
                applyEffects(pLevel, pPos, list);
                updateAndAttackTarget((ServerLevel)pLevel, pPos, pState, pBlockEntity, list.size() >= 42);
            }
        }

        if (pBlockEntity.isActive()) {
            if (i % 80L == 0L) {
                pLevel.playSound(null, pPos, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            if (i > pBlockEntity.nextAmbientSoundActivation) {
                pBlockEntity.nextAmbientSoundActivation = i + 60L + pLevel.getRandom().nextInt(40);
                pLevel.playSound(null, pPos, SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }

    private static void updateHunting(ConduitBlockEntity pBlockEntity, List<BlockPos> pPositions) {
        pBlockEntity.setHunting(pPositions.size() >= 42);
    }

    private static boolean updateShape(Level pLevel, BlockPos pPos, List<BlockPos> pPositions) {
        pPositions.clear();

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    BlockPos blockpos = pPos.offset(i, j, k);
                    if (!pLevel.isWaterAt(blockpos)) {
                        return false;
                    }
                }
            }
        }

        for (int j1 = -2; j1 <= 2; j1++) {
            for (int k1 = -2; k1 <= 2; k1++) {
                for (int l1 = -2; l1 <= 2; l1++) {
                    int i2 = Math.abs(j1);
                    int l = Math.abs(k1);
                    int i1 = Math.abs(l1);
                    if ((i2 > 1 || l > 1 || i1 > 1) && (j1 == 0 && (l == 2 || i1 == 2) || k1 == 0 && (i2 == 2 || i1 == 2) || l1 == 0 && (i2 == 2 || l == 2))) {
                        BlockPos blockpos1 = pPos.offset(j1, k1, l1);
                        BlockState blockstate = pLevel.getBlockState(blockpos1);

                        {
                            if (blockstate.isConduitFrame(pLevel, blockpos1, pPos)) {
                                pPositions.add(blockpos1);
                            }
                        }
                    }
                }
            }
        }

        return pPositions.size() >= 16;
    }

    private static void applyEffects(Level pLevel, BlockPos pPos, List<BlockPos> pPositions) {
        int i = pPositions.size();
        int j = i / 7 * 16;
        int k = pPos.getX();
        int l = pPos.getY();
        int i1 = pPos.getZ();
        AABB aabb = new AABB(k, l, i1, k + 1, l + 1, i1 + 1).inflate(j).expandTowards(0.0, pLevel.getHeight(), 0.0);
        List<Player> list = pLevel.getEntitiesOfClass(Player.class, aabb);
        if (!list.isEmpty()) {
            for (Player player : list) {
                if (pPos.closerThan(player.blockPosition(), j) && player.isInWaterOrRain()) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 260, 0, true, true));
                }
            }
        }
    }

    private static void updateAndAttackTarget(ServerLevel pLevel, BlockPos pPos, BlockState pState, ConduitBlockEntity pBlockEntity, boolean pCanDestroy) {
        EntityReference<LivingEntity> entityreference = updateDestroyTarget(pBlockEntity.destroyTarget, pLevel, pPos, pCanDestroy);
        LivingEntity livingentity = EntityReference.get(entityreference, pLevel, LivingEntity.class);
        if (livingentity != null) {
            pLevel.playSound(
                null, livingentity.getX(), livingentity.getY(), livingentity.getZ(), SoundEvents.CONDUIT_ATTACK_TARGET, SoundSource.BLOCKS, 1.0F, 1.0F
            );
            livingentity.hurtServer(pLevel, pLevel.damageSources().magic(), 4.0F);
        }

        if (!Objects.equals(entityreference, pBlockEntity.destroyTarget)) {
            pBlockEntity.destroyTarget = entityreference;
            pLevel.sendBlockUpdated(pPos, pState, pState, 2);
        }
    }

    @Nullable
    private static EntityReference<LivingEntity> updateDestroyTarget(
        @Nullable EntityReference<LivingEntity> pDestroyTarget, ServerLevel pLevel, BlockPos pPos, boolean pCanDestroy
    ) {
        if (!pCanDestroy) {
            return null;
        } else if (pDestroyTarget == null) {
            return selectNewTarget(pLevel, pPos);
        } else {
            LivingEntity livingentity = EntityReference.get(pDestroyTarget, pLevel, LivingEntity.class);
            return livingentity != null && livingentity.isAlive() && pPos.closerThan(livingentity.blockPosition(), 8.0) ? pDestroyTarget : null;
        }
    }

    @Nullable
    private static EntityReference<LivingEntity> selectNewTarget(ServerLevel pLevel, BlockPos pPos) {
        List<LivingEntity> list = pLevel.getEntitiesOfClass(LivingEntity.class, getDestroyRangeAABB(pPos), p_405706_ -> p_405706_ instanceof Enemy && p_405706_.isInWaterOrRain());
        return list.isEmpty() ? null : new EntityReference<>(Util.getRandom(list, pLevel.random));
    }

    private static AABB getDestroyRangeAABB(BlockPos pPos) {
        return new AABB(pPos).inflate(8.0);
    }

    private static void animationTick(Level pLevel, BlockPos pPos, List<BlockPos> pPositions, @Nullable Entity pEntity, int pTickCount) {
        RandomSource randomsource = pLevel.random;
        double d0 = Mth.sin((pTickCount + 35) * 0.1F) / 2.0F + 0.5F;
        d0 = (d0 * d0 + d0) * 0.3F;
        Vec3 vec3 = new Vec3(pPos.getX() + 0.5, pPos.getY() + 1.5 + d0, pPos.getZ() + 0.5);

        for (BlockPos blockpos : pPositions) {
            if (randomsource.nextInt(50) == 0) {
                BlockPos blockpos1 = blockpos.subtract(pPos);
                float f = -0.5F + randomsource.nextFloat() + blockpos1.getX();
                float f1 = -2.0F + randomsource.nextFloat() + blockpos1.getY();
                float f2 = -0.5F + randomsource.nextFloat() + blockpos1.getZ();
                pLevel.addParticle(ParticleTypes.NAUTILUS, vec3.x, vec3.y, vec3.z, f, f1, f2);
            }
        }

        if (pEntity != null) {
            Vec3 vec31 = new Vec3(pEntity.getX(), pEntity.getEyeY(), pEntity.getZ());
            float f3 = (-0.5F + randomsource.nextFloat()) * (3.0F + pEntity.getBbWidth());
            float f4 = -1.0F + randomsource.nextFloat() * pEntity.getBbHeight();
            float f5 = (-0.5F + randomsource.nextFloat()) * (3.0F + pEntity.getBbWidth());
            Vec3 vec32 = new Vec3(f3, f4, f5);
            pLevel.addParticle(ParticleTypes.NAUTILUS, vec31.x, vec31.y, vec31.z, vec32.x, vec32.y, vec32.z);
        }
    }

    public boolean isActive() {
        return this.isActive;
    }

    public boolean isHunting() {
        return this.isHunting;
    }

    private void setHunting(boolean pIsHunting) {
        this.isHunting = pIsHunting;
    }

    public float getActiveRotation(float pPartialTick) {
        return (this.activeRotation + pPartialTick) * -0.0375F;
    }
}
