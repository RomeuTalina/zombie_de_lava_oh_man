package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiPartModel implements BlockStateModel {
    private final MultiPartModel.SharedBakedState shared;
    private final BlockState blockState;
    @Nullable
    private List<BlockStateModel> models;
    private boolean cacheKey = false;
    @Nullable
    private java.util.Collection<net.minecraft.client.renderer.chunk.ChunkSectionLayer> cache;

    MultiPartModel(MultiPartModel.SharedBakedState pShared, BlockState pBlockState) {
        this.shared = pShared;
        this.blockState = pBlockState;
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return this.shared.particleIcon;
    }

    @Override
    public TextureAtlasSprite particleIcon(net.minecraftforge.client.model.data.ModelData data) {
        return this.shared.firstModel.particleIcon(data);
    }

    @Override
    public void collectParts(RandomSource p_391247_, List<BlockModelPart> p_397207_) {
        if (this.models == null) {
            this.models = this.shared.selectModels(this.blockState);
        }

        long i = p_391247_.nextLong();

        for (BlockStateModel blockstatemodel : this.models) {
            p_391247_.setSeed(i);
            blockstatemodel.collectParts(p_391247_, p_397207_);
        }
    }

    @Override
    public void collectParts(RandomSource rand, List<BlockModelPart> dest, net.minecraftforge.client.model.data.ModelData extraData, net.minecraft.client.renderer.chunk.ChunkSectionLayer renderType) {
        if (this.models == null) {
            this.models = this.shared.selectModels(this.blockState);
        }

        long i = rand.nextLong();

        for (BlockStateModel blockstatemodel : this.models) {
            rand.setSeed(i);
            blockstatemodel.collectParts(rand, dest, extraData, renderType);
        }
    }

    @Override
    public java.util.Collection<net.minecraft.client.renderer.chunk.ChunkSectionLayer> getRenderTypes(BlockState state, RandomSource rand, net.minecraftforge.client.model.data.ModelData data) {
        if (this.models == null)
            this.models = this.shared.selectModels(this.blockState);
        if (this.cache == null || this.cacheKey != net.minecraft.client.renderer.ItemBlockRenderTypes.isFancy()) {
            var tmp = java.util.EnumSet.noneOf(net.minecraft.client.renderer.chunk.ChunkSectionLayer.class);
            for (var model : this.models)
                tmp.addAll(model.getRenderTypes(state, rand, data));
            this.cache = tmp;
        }
        return !this.cache.isEmpty() ? this.cache : BlockStateModel.super.getRenderTypes(state, rand, data);
    }

    @OnlyIn(Dist.CLIENT)
    public record Selector<T>(Predicate<BlockState> condition, T model) {
        public <S> MultiPartModel.Selector<S> with(S pModel) {
            return new MultiPartModel.Selector<>(this.condition, pModel);
        }
    }

    @OnlyIn(Dist.CLIENT)
    static final class SharedBakedState {
        private final List<MultiPartModel.Selector<BlockStateModel>> selectors;
        final TextureAtlasSprite particleIcon;
        final BlockStateModel firstModel;
        private final Map<BitSet, List<BlockStateModel>> subsets = new ConcurrentHashMap<>();

        private static BlockStateModel getFirstModel(List<MultiPartModel.Selector<BlockStateModel>> pSelectors) {
            if (pSelectors.isEmpty()) {
                throw new IllegalArgumentException("Model must have at least one selector");
            } else {
                return pSelectors.getFirst().model();
            }
        }

        public SharedBakedState(List<MultiPartModel.Selector<BlockStateModel>> pSelectors) {
            this.selectors = pSelectors;
            this.firstModel = getFirstModel(pSelectors);
            this.particleIcon = firstModel.particleIcon();
        }

        public List<BlockStateModel> selectModels(BlockState pState) {
            BitSet bitset = new BitSet();

            for (int i = 0; i < this.selectors.size(); i++) {
                if (this.selectors.get(i).condition.test(pState)) {
                    bitset.set(i);
                }
            }

            return this.subsets.computeIfAbsent(bitset, p_394548_ -> {
                Builder<BlockStateModel> builder = ImmutableList.builder();

                for (int j = 0; j < this.selectors.size(); j++) {
                    if (p_394548_.get(j)) {
                        builder.add((BlockStateModel)this.selectors.get(j).model);
                    }
                }

                return builder.build();
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Unbaked implements BlockStateModel.UnbakedRoot {
        final List<MultiPartModel.Selector<BlockStateModel.Unbaked>> selectors;
        private final ModelBaker.SharedOperationKey<MultiPartModel.SharedBakedState> sharedStateKey = new ModelBaker.SharedOperationKey<MultiPartModel.SharedBakedState>(
            
        ) {
            public MultiPartModel.SharedBakedState compute(ModelBaker p_391510_) {
                Builder<MultiPartModel.Selector<BlockStateModel>> builder = ImmutableList.builderWithExpectedSize(Unbaked.this.selectors.size());

                for (MultiPartModel.Selector<BlockStateModel.Unbaked> selector : Unbaked.this.selectors) {
                    builder.add(selector.with(selector.model.bake(p_391510_)));
                }

                return new MultiPartModel.SharedBakedState(builder.build());
            }
        };

        public Unbaked(List<MultiPartModel.Selector<BlockStateModel.Unbaked>> pSelectors) {
            this.selectors = pSelectors;
        }

        @Override
        public Object visualEqualityGroup(BlockState p_395674_) {
            IntList intlist = new IntArrayList();

            for (int i = 0; i < this.selectors.size(); i++) {
                if (this.selectors.get(i).condition.test(p_395674_)) {
                    intlist.add(i);
                }
            }

            @OnlyIn(Dist.CLIENT)
            record Key(MultiPartModel.Unbaked model, IntList selectors) {
            }

            return new Key(this, intlist);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_397107_) {
            this.selectors.forEach(p_394441_ -> p_394441_.model.resolveDependencies(p_397107_));
        }

        @Override
        public BlockStateModel bake(BlockState p_397109_, ModelBaker p_397052_) {
            MultiPartModel.SharedBakedState multipartmodel$sharedbakedstate = p_397052_.compute(this.sharedStateKey);
            return new MultiPartModel(multipartmodel$sharedbakedstate, p_397109_);
        }
    }
}
