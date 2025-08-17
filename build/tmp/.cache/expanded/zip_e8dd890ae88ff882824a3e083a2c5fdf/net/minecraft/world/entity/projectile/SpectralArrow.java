package net.minecraft.world.entity.projectile;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SpectralArrow extends AbstractArrow {
    private static final int DEFAULT_DURATION = 200;
    private int duration = 200;

    public SpectralArrow(EntityType<? extends SpectralArrow> p_37411_, Level p_37412_) {
        super(p_37411_, p_37412_);
    }

    public SpectralArrow(Level pLevel, LivingEntity pOwner, ItemStack pPickupItemStack, @Nullable ItemStack pFiredFromWeapon) {
        super(EntityType.SPECTRAL_ARROW, pOwner, pLevel, pPickupItemStack, pFiredFromWeapon);
    }

    public SpectralArrow(Level pLevel, double pX, double pY, double pZ, ItemStack pPickupItemStack, @Nullable ItemStack pFiredFromWeapon) {
        super(EntityType.SPECTRAL_ARROW, pX, pY, pZ, pLevel, pPickupItemStack, pFiredFromWeapon);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.isInGround()) {
            this.level().addParticle(ParticleTypes.INSTANT_EFFECT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity pLiving) {
        super.doPostHurtEffects(pLiving);
        MobEffectInstance mobeffectinstance = new MobEffectInstance(MobEffects.GLOWING, this.duration, 0);
        pLiving.addEffect(mobeffectinstance, this.getEffectSource());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_409154_) {
        super.readAdditionalSaveData(p_409154_);
        this.duration = p_409154_.getIntOr("Duration", 200);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_407438_) {
        super.addAdditionalSaveData(p_407438_);
        p_407438_.putInt("Duration", this.duration);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.SPECTRAL_ARROW);
    }
}