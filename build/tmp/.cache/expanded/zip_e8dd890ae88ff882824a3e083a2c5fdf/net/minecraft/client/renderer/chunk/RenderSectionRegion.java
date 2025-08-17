package net.minecraft.client.renderer.chunk;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderSectionRegion implements BlockAndTintGetter {
    public static final int RADIUS = 1;
    public static final int SIZE = 3;
    private final int minSectionX;
    private final int minSectionY;
    private final int minSectionZ;
    private final SectionCopy[] sections;
    private final Level level;

    RenderSectionRegion(Level pLevel, int pMinSectionX, int pMinSectionY, int pMinSectionZ, SectionCopy[] pSections) {
        this.level = pLevel;
        this.minSectionX = pMinSectionX;
        this.minSectionY = pMinSectionY;
        this.minSectionZ = pMinSectionZ;
        this.sections = pSections;
    }

    @Override
    public BlockState getBlockState(BlockPos p_406800_) {
        return this.getSection(
                SectionPos.blockToSectionCoord(p_406800_.getX()), SectionPos.blockToSectionCoord(p_406800_.getY()), SectionPos.blockToSectionCoord(p_406800_.getZ())
            )
            .getBlockState(p_406800_);
    }

    @Override
    public FluidState getFluidState(BlockPos p_410161_) {
        return this.getSection(
                SectionPos.blockToSectionCoord(p_410161_.getX()), SectionPos.blockToSectionCoord(p_410161_.getY()), SectionPos.blockToSectionCoord(p_410161_.getZ())
            )
            .getBlockState(p_410161_)
            .getFluidState();
    }

    @Override
    public float getShade(Direction p_407825_, boolean p_407266_) {
        return this.level.getShade(p_407825_, p_407266_);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos p_408091_) {
        return this.getSection(
                SectionPos.blockToSectionCoord(p_408091_.getX()), SectionPos.blockToSectionCoord(p_408091_.getY()), SectionPos.blockToSectionCoord(p_408091_.getZ())
            )
            .getBlockEntity(p_408091_);
    }

    private SectionCopy getSection(int pX, int pY, int pZ) {
        return this.sections[index(this.minSectionX, this.minSectionY, this.minSectionZ, pX, pY, pZ)];
    }

    @Override
    public int getBlockTint(BlockPos p_407872_, ColorResolver p_407807_) {
        return this.level.getBlockTint(p_407872_, p_407807_);
    }

    @Override
    public int getMinY() {
        return this.level.getMinY();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }

    public static int index(int pMinX, int pMinY, int pMinZ, int pX, int pY, int pZ) {
        return pX - pMinX + (pY - pMinY) * 3 + (pZ - pMinZ) * 3 * 3;
    }

    @Override
    public net.minecraftforge.client.model.data.ModelDataManager getModelDataManager() {
       return level.getModelDataManager();
    }
}
