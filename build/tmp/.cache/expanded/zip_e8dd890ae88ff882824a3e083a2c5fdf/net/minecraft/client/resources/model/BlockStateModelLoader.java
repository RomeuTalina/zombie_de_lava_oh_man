package net.minecraft.client.resources.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class BlockStateModelLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter BLOCKSTATE_LISTER = FileToIdConverter.json("blockstates");

    public static CompletableFuture<BlockStateModelLoader.LoadedModels> loadBlockStates(ResourceManager pResourceManager, Executor pExecutor) {
        Function<ResourceLocation, StateDefinition<Block, BlockState>> function = BlockStateDefinitions.definitionLocationToBlockStateMapper();
        return CompletableFuture.<Map<ResourceLocation, List<Resource>>>supplyAsync(() -> BLOCKSTATE_LISTER.listMatchingResourceStacks(pResourceManager), pExecutor)
            .thenCompose(
                p_389570_ -> {
                    List<CompletableFuture<BlockStateModelLoader.LoadedModels>> list = new ArrayList<>(p_389570_.size());

                    for (Entry<ResourceLocation, List<Resource>> entry : p_389570_.entrySet()) {
                        list.add(
                            CompletableFuture.supplyAsync(
                                () -> {
                                    ResourceLocation resourcelocation = BLOCKSTATE_LISTER.fileToId(entry.getKey());
                                    StateDefinition<Block, BlockState> statedefinition = function.apply(resourcelocation);
                                    if (statedefinition == null) {
                                        LOGGER.debug("Discovered unknown block state definition {}, ignoring", resourcelocation);
                                        return null;
                                    } else {
                                        List<Resource> list1 = entry.getValue();
                                        List<BlockStateModelLoader.LoadedBlockModelDefinition> list2 = new ArrayList<>(list1.size());

                                        for (Resource resource : list1) {
                                            try (Reader reader = resource.openAsReader()) {
                                                JsonElement jsonelement = StrictJsonParser.parse(reader);
                                                BlockModelDefinition blockmodeldefinition = BlockModelDefinition.CODEC
                                                    .parse(JsonOps.INSTANCE, jsonelement)
                                                    .getOrThrow(JsonParseException::new);
                                                list2.add(new BlockStateModelLoader.LoadedBlockModelDefinition(resource.sourcePackId(), blockmodeldefinition));
                                            } catch (Exception exception1) {
                                                LOGGER.error(
                                                    "Failed to load blockstate definition {} from pack {}", resourcelocation, resource.sourcePackId(), exception1
                                                );
                                            }
                                        }

                                        try {
                                            return loadBlockStateDefinitionStack(resourcelocation, statedefinition, list2);
                                        } catch (Exception exception) {
                                            LOGGER.error("Failed to load blockstate definition {}", resourcelocation, exception);
                                            return null;
                                        }
                                    }
                                },
                                pExecutor
                            )
                        );
                    }

                    return Util.sequence(list).thenApply(p_389567_ -> {
                        Map<BlockState, BlockStateModel.UnbakedRoot> map = new IdentityHashMap<>();

                        for (BlockStateModelLoader.LoadedModels blockstatemodelloader$loadedmodels : p_389567_) {
                            if (blockstatemodelloader$loadedmodels != null) {
                                map.putAll(blockstatemodelloader$loadedmodels.models());
                            }
                        }

                        return new BlockStateModelLoader.LoadedModels(map);
                    });
                }
            );
    }

    private static BlockStateModelLoader.LoadedModels loadBlockStateDefinitionStack(
        ResourceLocation pBlockStateId, StateDefinition<Block, BlockState> pStateDefinition, List<BlockStateModelLoader.LoadedBlockModelDefinition> pModelDefinitions
    ) {
        Map<BlockState, BlockStateModel.UnbakedRoot> map = new IdentityHashMap<>();

        for (BlockStateModelLoader.LoadedBlockModelDefinition blockstatemodelloader$loadedblockmodeldefinition : pModelDefinitions) {
            map.putAll(
                blockstatemodelloader$loadedblockmodeldefinition.contents
                    .instantiate(pStateDefinition, () -> pBlockStateId + "/" + blockstatemodelloader$loadedblockmodeldefinition.source)
            );
        }

        return new BlockStateModelLoader.LoadedModels(map);
    }

    @OnlyIn(Dist.CLIENT)
    record LoadedBlockModelDefinition(String source, BlockModelDefinition contents) {
    }

    @OnlyIn(Dist.CLIENT)
    public record LoadedModels(Map<BlockState, BlockStateModel.UnbakedRoot> models) {
    }
}