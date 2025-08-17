package net.minecraft.world.entity;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class AreaEffectCloud extends Entity implements TraceableEntity {
    private static final int TIME_BETWEEN_APPLICATIONS = 5;
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_WAITING = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ParticleOptions> DATA_PARTICLE = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.PARTICLE);
    private static final float MAX_RADIUS = 32.0F;
    private static final int DEFAULT_AGE = 0;
    private static final int DEFAULT_DURATION_ON_USE = 0;
    private static final float DEFAULT_RADIUS_ON_USE = 0.0F;
    private static final float DEFAULT_RADIUS_PER_TICK = 0.0F;
    private static final float DEFAULT_POTION_DURATION_SCALE = 1.0F;
    private static final float MINIMAL_RADIUS = 0.5F;
    private static final float DEFAULT_RADIUS = 3.0F;
    public static final float DEFAULT_WIDTH = 6.0F;
    public static final float HEIGHT = 0.5F;
    public static final int INFINITE_DURATION = -1;
    public static final int DEFAULT_LINGERING_DURATION = 600;
    private static final int DEFAULT_WAIT_TIME = 20;
    private static final int DEFAULT_REAPPLICATION_DELAY = 20;
    private static final ColorParticleOption DEFAULT_PARTICLE = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, -1);
    @Nullable
    private ParticleOptions customParticle;
    private PotionContents potionContents = PotionContents.EMPTY;
    private float potionDurationScale = 1.0F;
    private final Map<Entity, Integer> victims = Maps.newHashMap();
    private int duration = -1;
    private int waitTime = 20;
    private int reapplicationDelay = 20;
    private int durationOnUse = 0;
    private float radiusOnUse = 0.0F;
    private float radiusPerTick = 0.0F;
    @Nullable
    private EntityReference<LivingEntity> owner;

    public AreaEffectCloud(EntityType<? extends AreaEffectCloud> p_19704_, Level p_19705_) {
        super(p_19704_, p_19705_);
        this.noPhysics = true;
    }

    public AreaEffectCloud(Level pLevel, double pX, double pY, double pZ) {
        this(EntityType.AREA_EFFECT_CLOUD, pLevel);
        this.setPos(pX, pY, pZ);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_330412_) {
        p_330412_.define(DATA_RADIUS, 3.0F);
        p_330412_.define(DATA_WAITING, false);
        p_330412_.define(DATA_PARTICLE, DEFAULT_PARTICLE);
    }

    public void setRadius(float pRadius) {
        if (!this.level().isClientSide) {
            this.getEntityData().set(DATA_RADIUS, Mth.clamp(pRadius, 0.0F, 32.0F));
        }
    }

    @Override
    public void refreshDimensions() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.refreshDimensions();
        this.setPos(d0, d1, d2);
    }

    public float getRadius() {
        return this.getEntityData().get(DATA_RADIUS);
    }

    public void setPotionContents(PotionContents pPotionContents) {
        this.potionContents = pPotionContents;
        this.updateParticle();
    }

    public void setCustomParticle(@Nullable ParticleOptions pCustomParticle) {
        this.customParticle = pCustomParticle;
        this.updateParticle();
    }

    public void setPotionDurationScale(float pPotionDurationScale) {
        this.potionDurationScale = pPotionDurationScale;
    }

    private void updateParticle() {
        if (this.customParticle != null) {
            this.entityData.set(DATA_PARTICLE, this.customParticle);
        } else {
            int i = ARGB.opaque(this.potionContents.getColor());
            this.entityData.set(DATA_PARTICLE, ColorParticleOption.create(DEFAULT_PARTICLE.getType(), i));
        }
    }

    public void addEffect(MobEffectInstance pEffectInstance) {
        this.setPotionContents(this.potionContents.withEffectAdded(pEffectInstance));
    }

    public ParticleOptions getParticle() {
        return this.getEntityData().get(DATA_PARTICLE);
    }

    protected void setWaiting(boolean pWaiting) {
        this.getEntityData().set(DATA_WAITING, pWaiting);
    }

    public boolean isWaiting() {
        return this.getEntityData().get(DATA_WAITING);
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int pDuration) {
        this.duration = pDuration;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverlevel) {
            this.serverTick(serverlevel);
        } else {
            this.clientTick();
        }
    }

    private void clientTick() {
        boolean flag = this.isWaiting();
        float f = this.getRadius();
        if (!flag || !this.random.nextBoolean()) {
            ParticleOptions particleoptions = this.getParticle();
            int i;
            float f1;
            if (flag) {
                i = 2;
                f1 = 0.2F;
            } else {
                i = Mth.ceil((float) Math.PI * f * f);
                f1 = f;
            }

            for (int j = 0; j < i; j++) {
                float f2 = this.random.nextFloat() * (float) (Math.PI * 2);
                float f3 = Mth.sqrt(this.random.nextFloat()) * f1;
                double d0 = this.getX() + Mth.cos(f2) * f3;
                double d1 = this.getY();
                double d2 = this.getZ() + Mth.sin(f2) * f3;
                if (particleoptions.getType() == ParticleTypes.ENTITY_EFFECT) {
                    if (flag && this.random.nextBoolean()) {
                        this.level().addAlwaysVisibleParticle(DEFAULT_PARTICLE, d0, d1, d2, 0.0, 0.0, 0.0);
                    } else {
                        this.level().addAlwaysVisibleParticle(particleoptions, d0, d1, d2, 0.0, 0.0, 0.0);
                    }
                } else if (flag) {
                    this.level().addAlwaysVisibleParticle(particleoptions, d0, d1, d2, 0.0, 0.0, 0.0);
                } else {
                    this.level()
                        .addAlwaysVisibleParticle(particleoptions, d0, d1, d2, (0.5 - this.random.nextDouble()) * 0.15, 0.01F, (0.5 - this.random.nextDouble()) * 0.15);
                }
            }
        }
    }

    private void serverTick(ServerLevel pLevel) {
        if (this.duration != -1 && this.tickCount - this.waitTime >= this.duration) {
            this.discard();
        } else {
            boolean flag = this.isWaiting();
            boolean flag1 = this.tickCount < this.waitTime;
            if (flag != flag1) {
                this.setWaiting(flag1);
            }

            if (!flag1) {
                float f = this.getRadius();
                if (this.radiusPerTick != 0.0F) {
                    f += this.radiusPerTick;
                    if (f < 0.5F) {
                        this.discard();
                        return;
                    }

                    this.setRadius(f);
                }

                if (this.tickCount % 5 == 0) {
                    this.victims.entrySet().removeIf(p_287380_ -> this.tickCount >= p_287380_.getValue());
                    if (!this.potionContents.hasEffects()) {
                        this.victims.clear();
                    } else {
                        List<MobEffectInstance> list = new ArrayList<>();
                        this.potionContents.forEachEffect(list::add, this.potionDurationScale);
                        List<LivingEntity> list1 = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox());
                        if (!list1.isEmpty()) {
                            for (LivingEntity livingentity : list1) {
                                if (!this.victims.containsKey(livingentity) && livingentity.isAffectedByPotions() && !list.stream().noneMatch(livingentity::canBeAffected)) {
                                    double d0 = livingentity.getX() - this.getX();
                                    double d1 = livingentity.getZ() - this.getZ();
                                    double d2 = d0 * d0 + d1 * d1;
                                    if (d2 <= f * f) {
                                        this.victims.put(livingentity, this.tickCount + this.reapplicationDelay);

                                        for (MobEffectInstance mobeffectinstance : list) {
                                            if (mobeffectinstance.getEffect().value().isInstantenous()) {
                                                mobeffectinstance.getEffect()
                                                    .value()
                                                    .applyInstantenousEffect(pLevel, this, this.getOwner(), livingentity, mobeffectinstance.getAmplifier(), 0.5);
                                            } else {
                                                livingentity.addEffect(new MobEffectInstance(mobeffectinstance), this);
                                            }
                                        }

                                        if (this.radiusOnUse != 0.0F) {
                                            f += this.radiusOnUse;
                                            if (f < 0.5F) {
                                                this.discard();
                                                return;
                                            }

                                            this.setRadius(f);
                                        }

                                        if (this.durationOnUse != 0 && this.duration != -1) {
                                            this.duration = this.duration + this.durationOnUse;
                                            if (this.duration <= 0) {
                                                this.discard();
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public float getRadiusOnUse() {
        return this.radiusOnUse;
    }

    public void setRadiusOnUse(float pRadiusOnUse) {
        this.radiusOnUse = pRadiusOnUse;
    }

    public float getRadiusPerTick() {
        return this.radiusPerTick;
    }

    public void setRadiusPerTick(float pRadiusPerTick) {
        this.radiusPerTick = pRadiusPerTick;
    }

    public int getDurationOnUse() {
        return this.durationOnUse;
    }

    public void setDurationOnUse(int pDurationOnUse) {
        this.durationOnUse = pDurationOnUse;
    }

    public int getWaitTime() {
        return this.waitTime;
    }

    public void setWaitTime(int pWaitTime) {
        this.waitTime = pWaitTime;
    }

    public void setOwner(@Nullable LivingEntity pOwner) {
        this.owner = pOwner != null ? new EntityReference<>(pOwner) : null;
    }

    @Nullable
    public LivingEntity getOwner() {
        return EntityReference.get(this.owner, this.level(), LivingEntity.class);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_409186_) {
        this.tickCount = p_409186_.getIntOr("Age", 0);
        this.duration = p_409186_.getIntOr("Duration", -1);
        this.waitTime = p_409186_.getIntOr("WaitTime", 20);
        this.reapplicationDelay = p_409186_.getIntOr("ReapplicationDelay", 20);
        this.durationOnUse = p_409186_.getIntOr("DurationOnUse", 0);
        this.radiusOnUse = p_409186_.getFloatOr("RadiusOnUse", 0.0F);
        this.radiusPerTick = p_409186_.getFloatOr("RadiusPerTick", 0.0F);
        this.setRadius(p_409186_.getFloatOr("Radius", 3.0F));
        this.owner = EntityReference.read(p_409186_, "Owner");
        this.setCustomParticle(p_409186_.read("custom_particle", ParticleTypes.CODEC).orElse(null));
        this.setPotionContents(p_409186_.read("potion_contents", PotionContents.CODEC).orElse(PotionContents.EMPTY));
        this.potionDurationScale = p_409186_.getFloatOr("potion_duration_scale", 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_409199_) {
        p_409199_.putInt("Age", this.tickCount);
        p_409199_.putInt("Duration", this.duration);
        p_409199_.putInt("WaitTime", this.waitTime);
        p_409199_.putInt("ReapplicationDelay", this.reapplicationDelay);
        p_409199_.putInt("DurationOnUse", this.durationOnUse);
        p_409199_.putFloat("RadiusOnUse", this.radiusOnUse);
        p_409199_.putFloat("RadiusPerTick", this.radiusPerTick);
        p_409199_.putFloat("Radius", this.getRadius());
        p_409199_.storeNullable("custom_particle", ParticleTypes.CODEC, this.customParticle);
        EntityReference.store(this.owner, p_409199_, "Owner");
        if (!this.potionContents.equals(PotionContents.EMPTY)) {
            p_409199_.store("potion_contents", PotionContents.CODEC, this.potionContents);
        }

        if (this.potionDurationScale != 1.0F) {
            p_409199_.putFloat("potion_duration_scale", this.potionDurationScale);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (DATA_RADIUS.equals(pKey)) {
            this.refreshDimensions();
        }

        super.onSyncedDataUpdated(pKey);
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        return EntityDimensions.scalable(this.getRadius() * 2.0F, 0.5F);
    }

    @Override
    public final boolean hurtServer(ServerLevel p_360854_, DamageSource p_364045_, float p_363449_) {
        return false;
    }

    @Nullable
    @Override
    public <T> T get(DataComponentType<? extends T> p_394344_) {
        if (p_394344_ == DataComponents.POTION_CONTENTS) {
            return castComponentValue((DataComponentType<T>)p_394344_, this.potionContents);
        } else {
            return p_394344_ == DataComponents.POTION_DURATION_SCALE ? castComponentValue((DataComponentType<T>)p_394344_, this.potionDurationScale) : super.get(p_394344_);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_396444_) {
        this.applyImplicitComponentIfPresent(p_396444_, DataComponents.POTION_CONTENTS);
        this.applyImplicitComponentIfPresent(p_396444_, DataComponents.POTION_DURATION_SCALE);
        super.applyImplicitComponents(p_396444_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_392513_, T p_395434_) {
        if (p_392513_ == DataComponents.POTION_CONTENTS) {
            this.setPotionContents(castComponentValue(DataComponents.POTION_CONTENTS, p_395434_));
            return true;
        } else if (p_392513_ == DataComponents.POTION_DURATION_SCALE) {
            this.setPotionDurationScale(castComponentValue(DataComponents.POTION_DURATION_SCALE, p_395434_));
            return true;
        } else {
            return super.applyImplicitComponent(p_392513_, p_395434_);
        }
    }
}