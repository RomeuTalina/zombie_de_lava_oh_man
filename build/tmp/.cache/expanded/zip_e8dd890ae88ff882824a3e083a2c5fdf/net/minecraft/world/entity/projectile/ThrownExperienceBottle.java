package net.minecraft.world.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ThrownExperienceBottle extends ThrowableItemProjectile {
    public ThrownExperienceBottle(EntityType<? extends ThrownExperienceBottle> p_37510_, Level p_37511_) {
        super(p_37510_, p_37511_);
    }

    public ThrownExperienceBottle(Level pLevel, LivingEntity pOwner, ItemStack pItem) {
        super(EntityType.EXPERIENCE_BOTTLE, pOwner, pLevel, pItem);
    }

    public ThrownExperienceBottle(Level pLevel, double pX, double pY, double pZ, ItemStack pItem) {
        super(EntityType.EXPERIENCE_BOTTLE, pX, pY, pZ, pLevel, pItem);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.EXPERIENCE_BOTTLE;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.07;
    }

    @Override
    protected void onHit(HitResult pResult) {
        super.onHit(pResult);
        if (this.level() instanceof ServerLevel serverlevel) {
            serverlevel.levelEvent(2002, this.blockPosition(), -13083194);
            int i = 3 + serverlevel.random.nextInt(5) + serverlevel.random.nextInt(5);
            if (pResult instanceof BlockHitResult blockhitresult) {
                Vec3 vec3 = blockhitresult.getDirection().getUnitVec3();
                ExperienceOrb.awardWithDirection(serverlevel, pResult.getLocation(), vec3, i);
            } else {
                ExperienceOrb.awardWithDirection(serverlevel, pResult.getLocation(), this.getDeltaMovement().scale(-1.0), i);
            }

            this.discard();
        }
    }
}