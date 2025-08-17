package net.minecraft.world.entity.animal.wolf;

import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class WolfVariants {
    public static final ResourceKey<WolfVariant> PALE = createKey("pale");
    public static final ResourceKey<WolfVariant> SPOTTED = createKey("spotted");
    public static final ResourceKey<WolfVariant> SNOWY = createKey("snowy");
    public static final ResourceKey<WolfVariant> BLACK = createKey("black");
    public static final ResourceKey<WolfVariant> ASHEN = createKey("ashen");
    public static final ResourceKey<WolfVariant> RUSTY = createKey("rusty");
    public static final ResourceKey<WolfVariant> WOODS = createKey("woods");
    public static final ResourceKey<WolfVariant> CHESTNUT = createKey("chestnut");
    public static final ResourceKey<WolfVariant> STRIPED = createKey("striped");
    public static final ResourceKey<WolfVariant> DEFAULT = PALE;

    private static ResourceKey<WolfVariant> createKey(String pName) {
        return ResourceKey.create(Registries.WOLF_VARIANT, ResourceLocation.withDefaultNamespace(pName));
    }

    private static void register(BootstrapContext<WolfVariant> pContext, ResourceKey<WolfVariant> pKey, String pName, ResourceKey<Biome> pBiome) {
        register(pContext, pKey, pName, highPrioBiome(HolderSet.direct(pContext.lookup(Registries.BIOME).getOrThrow(pBiome))));
    }

    private static void register(BootstrapContext<WolfVariant> pContext, ResourceKey<WolfVariant> pKey, String pName, TagKey<Biome> pBiomes) {
        register(pContext, pKey, pName, highPrioBiome(pContext.lookup(Registries.BIOME).getOrThrow(pBiomes)));
    }

    private static SpawnPrioritySelectors highPrioBiome(HolderSet<Biome> pBiomes) {
        return SpawnPrioritySelectors.single(new BiomeCheck(pBiomes), 1);
    }

    private static void register(
        BootstrapContext<WolfVariant> pContext, ResourceKey<WolfVariant> pKey, String pName, SpawnPrioritySelectors pSpawnConditions
    ) {
        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace("entity/wolf/" + pName);
        ResourceLocation resourcelocation1 = ResourceLocation.withDefaultNamespace("entity/wolf/" + pName + "_tame");
        ResourceLocation resourcelocation2 = ResourceLocation.withDefaultNamespace("entity/wolf/" + pName + "_angry");
        pContext.register(
            pKey,
            new WolfVariant(
                new WolfVariant.AssetInfo(new ClientAsset(resourcelocation), new ClientAsset(resourcelocation1), new ClientAsset(resourcelocation2)), pSpawnConditions
            )
        );
    }

    public static void bootstrap(BootstrapContext<WolfVariant> pContext) {
        register(pContext, PALE, "wolf", SpawnPrioritySelectors.fallback(0));
        register(pContext, SPOTTED, "wolf_spotted", BiomeTags.IS_SAVANNA);
        register(pContext, SNOWY, "wolf_snowy", Biomes.GROVE);
        register(pContext, BLACK, "wolf_black", Biomes.OLD_GROWTH_PINE_TAIGA);
        register(pContext, ASHEN, "wolf_ashen", Biomes.SNOWY_TAIGA);
        register(pContext, RUSTY, "wolf_rusty", BiomeTags.IS_JUNGLE);
        register(pContext, WOODS, "wolf_woods", Biomes.FOREST);
        register(pContext, CHESTNUT, "wolf_chestnut", Biomes.OLD_GROWTH_SPRUCE_TAIGA);
        register(pContext, STRIPED, "wolf_striped", BiomeTags.IS_BADLANDS);
    }
}