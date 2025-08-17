package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SectionCompiler {
    private final BlockRenderDispatcher blockRenderer;
    private final BlockEntityRenderDispatcher blockEntityRenderer;

    public SectionCompiler(BlockRenderDispatcher pBlockRenderer, BlockEntityRenderDispatcher pBlockEntityRenderer) {
        this.blockRenderer = pBlockRenderer;
        this.blockEntityRenderer = pBlockEntityRenderer;
    }

    public SectionCompiler.Results compile(SectionPos pSectionPos, RenderSectionRegion pRegion, VertexSorting pVertexSorting, SectionBufferBuilderPack pSectionBufferBuilderPack) {
        var modelDataMap = net.minecraft.client.Minecraft.getInstance().level.getModelDataManager().getAt(pSectionPos);
        SectionCompiler.Results sectioncompiler$results = new SectionCompiler.Results();
        BlockPos blockpos = pSectionPos.origin();
        BlockPos blockpos1 = blockpos.offset(15, 15, 15);
        VisGraph visgraph = new VisGraph();
        PoseStack posestack = new PoseStack();
        ModelBlockRenderer.enableCaching();
        Map<ChunkSectionLayer, BufferBuilder> map = new EnumMap<>(ChunkSectionLayer.class);
        RandomSource randomsource = RandomSource.create();
        List<BlockModelPart> list = new ObjectArrayList<>();

        for (BlockPos blockpos2 : BlockPos.betweenClosed(blockpos, blockpos1)) {
            BlockState blockstate = pRegion.getBlockState(blockpos2);
            if (blockstate.isSolidRender()) {
                visgraph.setOpaque(blockpos2);
            }

            if (blockstate.hasBlockEntity()) {
                BlockEntity blockentity = pRegion.getBlockEntity(blockpos2);
                if (blockentity != null) {
                    this.handleBlockEntity(sectioncompiler$results, blockentity);
                }
            }

            FluidState fluidstate = blockstate.getFluidState();
            if (!fluidstate.isEmpty()) {
                ChunkSectionLayer chunksectionlayer = ItemBlockRenderTypes.getRenderLayer(fluidstate);
                BufferBuilder bufferbuilder = this.getOrBeginLayer(map, pSectionBufferBuilderPack, chunksectionlayer);
                this.blockRenderer.renderLiquid(blockpos2, pRegion, bufferbuilder, blockstate, fluidstate);
            }

            if (blockstate.getRenderShape() == RenderShape.MODEL) {
                var model = this.blockRenderer.getBlockModel(blockstate);
                var data = modelDataMap.getOrDefault(blockpos2, net.minecraftforge.client.model.data.ModelData.EMPTY);
                data = model.getModelData(pRegion, blockpos2, blockstate, data);
                randomsource.setSeed(blockstate.getSeed(blockpos2)); // Forge: We set this on purpose twice so that getRenderTypes can have the same RNG as collectParts
                for (ChunkSectionLayer chunksectionlayer2 : model.getRenderTypes(blockstate, randomsource, data)) {
                BufferBuilder bufferbuilder1 = this.getOrBeginLayer(map, pSectionBufferBuilderPack, chunksectionlayer2);
                randomsource.setSeed(blockstate.getSeed(blockpos2));
                model.collectParts(randomsource, list, data, chunksectionlayer2);
                posestack.pushPose();
                posestack.translate(
                    SectionPos.sectionRelative(blockpos2.getX()), SectionPos.sectionRelative(blockpos2.getY()), SectionPos.sectionRelative(blockpos2.getZ())
                );
                this.blockRenderer.renderBatched(blockstate, blockpos2, pRegion, posestack, bufferbuilder1, true, list);
                posestack.popPose();
                list.clear();
                }
            }
        }

        for (Entry<ChunkSectionLayer, BufferBuilder> entry : map.entrySet()) {
            ChunkSectionLayer chunksectionlayer1 = entry.getKey();
            MeshData meshdata = entry.getValue().build();
            if (meshdata != null) {
                if (chunksectionlayer1 == ChunkSectionLayer.TRANSLUCENT) {
                    sectioncompiler$results.transparencyState = meshdata.sortQuads(pSectionBufferBuilderPack.buffer(chunksectionlayer1), pVertexSorting);
                }

                sectioncompiler$results.renderedLayers.put(chunksectionlayer1, meshdata);
            }
        }

        ModelBlockRenderer.clearCache();
        sectioncompiler$results.visibilitySet = visgraph.resolve();
        return sectioncompiler$results;
    }

    private BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> pBuilders, SectionBufferBuilderPack pSectionBufferBuilderPack, ChunkSectionLayer pLayer) {
        BufferBuilder bufferbuilder = pBuilders.get(pLayer);
        if (bufferbuilder == null) {
            ByteBufferBuilder bytebufferbuilder = pSectionBufferBuilderPack.buffer(pLayer);
            bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            pBuilders.put(pLayer, bufferbuilder);
        }

        return bufferbuilder;
    }

    private <E extends BlockEntity> void handleBlockEntity(SectionCompiler.Results pResults, E pBlockEntity) {
        BlockEntityRenderer<E> blockentityrenderer = this.blockEntityRenderer.getRenderer(pBlockEntity);
        if (blockentityrenderer != null && !blockentityrenderer.shouldRenderOffScreen()) {
            pResults.blockEntities.add(pBlockEntity);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class Results {
        public final List<BlockEntity> blockEntities = new ArrayList<>();
        public final Map<ChunkSectionLayer, MeshData> renderedLayers = new EnumMap<>(ChunkSectionLayer.class);
        public VisibilitySet visibilitySet = new VisibilitySet();
        @Nullable
        public MeshData.SortState transparencyState;

        public void release() {
            this.renderedLayers.values().forEach(MeshData::close);
        }
    }
}
