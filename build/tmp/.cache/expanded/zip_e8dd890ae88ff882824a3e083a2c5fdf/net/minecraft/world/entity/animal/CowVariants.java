package net.minecraft.world.entity.animal;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;

public class CowVariants {
    public static final ResourceKey<CowVariant> TEMPERATE = createKey(TemperatureVariants.TEMPERATE);
    public static final ResourceKey<CowVariant> WARM = createKey(TemperatureVariants.WARM);
    public static final ResourceKey<CowVariant> COLD = createKey(TemperatureVariants.COLD);
    public static final ResourceKey<CowVariant> DEFAULT = TEMPERATE;

    private static ResourceKey<CowVariant> createKey(ResourceLocation pName) {
        return ResourceKey.create(Registries.COW_VARIANT, pName);
    }

    public static void bootstrap(BootstrapContext<CowVariant> pContext) {
        register(pContext, TEMPERATE, CowVariant.ModelType.NORMAL, "temperate_cow", SpawnPrioritySelectors.fallback(0));
        register(pContext, WARM, CowVariant.ModelType.WARM, "warm_cow", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
        register(pContext, COLD, CowVariant.ModelType.COLD, "cold_cow", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
    }

    private static void register(
        BootstrapContext<CowVariant> pContext, ResourceKey<CowVariant> pKey, CowVariant.ModelType pModelType, String pAssetId, TagKey<Biome> pBiomes
    ) {
        HolderSet<Biome> holderset = pContext.lookup(Registries.BIOME).getOrThrow(pBiomes);
        register(pContext, pKey, pModelType, pAssetId, SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1));
    }

    private static void register(
        BootstrapContext<CowVariant> pContext,
        ResourceKey<CowVariant> pKey,
        CowVariant.ModelType pModelType,
        String pAssetId,
        SpawnPrioritySelectors pSpawnConditions
    ) {
        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace("entity/cow/" + pAssetId);
        pContext.register(pKey, new CowVariant(new ModelAndTexture<>(pModelType, resourcelocation), pSpawnConditions));
    }
}