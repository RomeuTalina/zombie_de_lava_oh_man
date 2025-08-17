package net.minecraft.world.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LargeFireball extends Fireball {
    private static final byte DEFAULT_EXPLOSION_POWER = 1;
    private int explosionPower = 1;

    public LargeFireball(EntityType<? extends LargeFireball> p_37199_, Level p_37200_) {
        super(p_37199_, p_37200_);
    }

    public LargeFireball(Level p_181151_, LivingEntity p_181152_, Vec3 p_342986_, int p_181156_) {
        super(EntityType.FIREBALL, p_181152_, p_342986_, p_181151_);
        this.explosionPower = p_181156_;
    }

    @Override
    protected void onHit(HitResult p_37218_) {
        super.onHit(p_37218_);
        if (this.level() instanceof ServerLevel serverlevel) {
            boolean flag = net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(serverlevel, this.getOwner());
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.explosionPower, flag, Level.ExplosionInteraction.MOB);
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult p_37216_) {
        super.onHitEntity(p_37216_);
        if (this.level() instanceof ServerLevel serverlevel) {
            Entity entity1 = p_37216_.getEntity();
            Entity $$4 = this.getOwner();
            DamageSource $$5 = this.damageSources().fireball(this, $$4);
            entity1.hurtServer(serverlevel, $$5, 6.0F);
            EnchantmentHelper.doPostAttackEffects(serverlevel, entity1, $$5);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_409727_) {
        super.addAdditionalSaveData(p_409727_);
        p_409727_.putByte("ExplosionPower", (byte)this.explosionPower);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_410726_) {
        super.readAdditionalSaveData(p_410726_);
        this.explosionPower = p_410726_.getByteOr("ExplosionPower", (byte)1);
    }
}
