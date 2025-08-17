package net.minecraft.world.entity.variant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;

public record SpawnContext(BlockPos pos, ServerLevelAccessor level, Holder<Biome> biome) {
    public static SpawnContext create(ServerLevelAccessor pLevel, BlockPos pPos) {
        Holder<Biome> holder = pLevel.getBiome(pPos);
        return new SpawnContext(pPos, pLevel, holder);
    }
}