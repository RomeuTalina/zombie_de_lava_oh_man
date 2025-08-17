package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderRegionCache {
    private final Long2ObjectMap<SectionCopy> sectionCopyCache = new Long2ObjectOpenHashMap<>();

    public RenderSectionRegion createRegion(Level pLevel, long pChunkPos) {
        int i = SectionPos.x(pChunkPos);
        int j = SectionPos.y(pChunkPos);
        int k = SectionPos.z(pChunkPos);
        int l = i - 1;
        int i1 = j - 1;
        int j1 = k - 1;
        int k1 = i + 1;
        int l1 = j + 1;
        int i2 = k + 1;
        SectionCopy[] asectioncopy = new SectionCopy[27];

        for (int j2 = j1; j2 <= i2; j2++) {
            for (int k2 = i1; k2 <= l1; k2++) {
                for (int l2 = l; l2 <= k1; l2++) {
                    int i3 = RenderSectionRegion.index(l, i1, j1, l2, k2, j2);
                    asectioncopy[i3] = this.getSectionDataCopy(pLevel, l2, k2, j2);
                }
            }
        }

        return new RenderSectionRegion(pLevel, l, i1, j1, asectioncopy);
    }

    private SectionCopy getSectionDataCopy(Level pLevel, int pX, int pY, int pZ) {
        return this.sectionCopyCache.computeIfAbsent(SectionPos.asLong(pX, pY, pZ), p_404989_ -> {
            LevelChunk levelchunk = pLevel.getChunk(pX, pZ);
            return new SectionCopy(levelchunk, levelchunk.getSectionIndexFromSectionY(pY));
        });
    }
}