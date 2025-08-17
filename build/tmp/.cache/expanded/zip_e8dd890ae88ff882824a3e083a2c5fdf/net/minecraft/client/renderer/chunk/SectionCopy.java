package net.minecraft.client.renderer.chunk;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
class SectionCopy {
    private final Map<BlockPos, BlockEntity> blockEntities;
    @Nullable
    private final PalettedContainer<BlockState> section;
    private final boolean debug;
    private final LevelHeightAccessor levelHeightAccessor;

    SectionCopy(LevelChunk pChunk, int pSectionIndex) {
        this.levelHeightAccessor = pChunk;
        this.debug = pChunk.getLevel().isDebug();
        this.blockEntities = ImmutableMap.copyOf(pChunk.getBlockEntities());
        if (pChunk instanceof EmptyLevelChunk) {
            this.section = null;
        } else {
            LevelChunkSection[] alevelchunksection = pChunk.getSections();
            if (pSectionIndex >= 0 && pSectionIndex < alevelchunksection.length) {
                LevelChunkSection levelchunksection = alevelchunksection[pSectionIndex];
                this.section = levelchunksection.hasOnlyAir() ? null : levelchunksection.getStates().copy();
            } else {
                this.section = null;
            }
        }
    }

    @Nullable
    public BlockEntity getBlockEntity(BlockPos pPos) {
        return this.blockEntities.get(pPos);
    }

    public BlockState getBlockState(BlockPos pPos) {
        int i = pPos.getX();
        int j = pPos.getY();
        int k = pPos.getZ();
        if (this.debug) {
            BlockState blockstate = null;
            if (j == 60) {
                blockstate = Blocks.BARRIER.defaultBlockState();
            }

            if (j == 70) {
                blockstate = DebugLevelSource.getBlockStateFor(i, k);
            }

            return blockstate == null ? Blocks.AIR.defaultBlockState() : blockstate;
        } else if (this.section == null) {
            return Blocks.AIR.defaultBlockState();
        } else {
            try {
                return this.section.get(i & 15, j & 15, k & 15);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting block state");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Block being got");
                crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this.levelHeightAccessor, i, j, k));
                throw new ReportedException(crashreport);
            }
        }
    }
}