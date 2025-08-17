package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public interface DispensibleContainerItem extends net.minecraftforge.common.extensions.IForgeDispensibleContainerItem {
    default void checkExtraContent(@Nullable LivingEntity pEntity, Level pLevel, ItemStack pStack, BlockPos pPos) {
    }

    @Deprecated //Forge: use the ItemStack sensitive version
    boolean emptyContents(@Nullable LivingEntity pEntity, Level pLevel, BlockPos pPos, @Nullable BlockHitResult pHitResult);
}
