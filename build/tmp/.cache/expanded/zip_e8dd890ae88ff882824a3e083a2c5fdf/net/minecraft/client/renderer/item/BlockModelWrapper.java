package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class BlockModelWrapper implements ItemModel {
    private final List<ItemTintSource> tints;
    private final List<BakedQuad> quads;
    private final Supplier<Vector3f[]> extents;
    private final ModelRenderProperties properties;
    private final boolean animated;

    public BlockModelWrapper(List<ItemTintSource> pTints, List<BakedQuad> pQuads, ModelRenderProperties pProperties) {
        this.tints = pTints;
        this.quads = pQuads;
        this.properties = pProperties;
        this.extents = Suppliers.memoize(() -> computeExtents(this.quads));
        boolean flag = false;

        for (BakedQuad bakedquad : pQuads) {
            if (bakedquad.sprite().isAnimated()) {
                flag = true;
                break;
            }
        }

        this.animated = flag;
    }

    public static Vector3f[] computeExtents(List<BakedQuad> pQuads) {
        Set<Vector3f> set = new HashSet<>();

        for (BakedQuad bakedquad : pQuads) {
            FaceBakery.extractPositions(bakedquad.vertices(), set::add);
        }

        return set.toArray(Vector3f[]::new);
    }

    @Override
    public void update(
        ItemStackRenderState p_377049_,
        ItemStack p_378482_,
        ItemModelResolver p_377214_,
        ItemDisplayContext p_375691_,
        @Nullable ClientLevel p_376532_,
        @Nullable LivingEntity p_376906_,
        int p_377340_
    ) {
        p_377049_.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = p_377049_.newLayer();
        if (p_378482_.hasFoil()) {
            ItemStackRenderState.FoilType itemstackrenderstate$foiltype = hasSpecialAnimatedTexture(p_378482_)
                ? ItemStackRenderState.FoilType.SPECIAL
                : ItemStackRenderState.FoilType.STANDARD;
            itemstackrenderstate$layerrenderstate.setFoilType(itemstackrenderstate$foiltype);
            p_377049_.setAnimated();
            p_377049_.appendModelIdentityElement(itemstackrenderstate$foiltype);
        }

        int k = this.tints.size();
        int[] aint = itemstackrenderstate$layerrenderstate.prepareTintLayers(k);

        for (int i = 0; i < k; i++) {
            int j = this.tints.get(i).calculate(p_378482_, p_376532_, p_376906_);
            aint[i] = j;
            p_377049_.appendModelIdentityElement(j);
        }

        itemstackrenderstate$layerrenderstate.setExtents(this.extents);
        itemstackrenderstate$layerrenderstate.setRenderType(ItemBlockRenderTypes.getRenderType(p_378482_));
        this.properties.applyToLayer(itemstackrenderstate$layerrenderstate, p_375691_);
        itemstackrenderstate$layerrenderstate.prepareQuadList().addAll(this.quads);
        if (this.animated) {
            p_377049_.setAnimated();
        }
    }

    private static boolean hasSpecialAnimatedTexture(ItemStack pStack) {
        return pStack.is(ItemTags.COMPASSES) || pStack.is(Items.CLOCK);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(ResourceLocation model, List<ItemTintSource> tints) implements ItemModel.Unbaked {
        public static final MapCodec<BlockModelWrapper.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_376987_ -> p_376987_.group(
                    ResourceLocation.CODEC.fieldOf("model").forGetter(BlockModelWrapper.Unbaked::model),
                    ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(BlockModelWrapper.Unbaked::tints)
                )
                .apply(p_376987_, BlockModelWrapper.Unbaked::new)
        );

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_375708_) {
            p_375708_.markDependency(this.model);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_375857_) {
            ModelBaker modelbaker = p_375857_.blockModelBaker();
            ResolvedModel resolvedmodel = modelbaker.getModel(this.model);
            TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
            List<BakedQuad> list = resolvedmodel.bakeTopGeometry(textureslots, modelbaker, BlockModelRotation.X0_Y0).getAll();
            ModelRenderProperties modelrenderproperties = ModelRenderProperties.fromResolvedModel(modelbaker, resolvedmodel, textureslots);
            return new BlockModelWrapper(this.tints, list, modelrenderproperties);
        }

        @Override
        public MapCodec<BlockModelWrapper.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}