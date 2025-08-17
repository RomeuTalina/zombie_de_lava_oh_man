package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CompiledSectionMesh implements SectionMesh {
    public static final SectionMesh UNCOMPILED = new SectionMesh() {
        @Override
        public boolean facesCanSeeEachother(Direction p_407632_, Direction p_406501_) {
            return false;
        }
    };
    public static final SectionMesh EMPTY = new SectionMesh() {
        @Override
        public boolean facesCanSeeEachother(Direction p_406904_, Direction p_406261_) {
            return true;
        }
    };
    private final List<BlockEntity> renderableBlockEntities;
    private final VisibilitySet visibilitySet;
    @Nullable
    private final MeshData.SortState transparencyState;
    @Nullable
    private TranslucencyPointOfView translucencyPointOfView;
    private final Map<ChunkSectionLayer, SectionBuffers> buffers = new EnumMap<>(ChunkSectionLayer.class);

    public CompiledSectionMesh(TranslucencyPointOfView pTranslucencyPointOfView, SectionCompiler.Results pResults) {
        this.translucencyPointOfView = pTranslucencyPointOfView;
        this.visibilitySet = pResults.visibilitySet;
        this.renderableBlockEntities = pResults.blockEntities;
        this.transparencyState = pResults.transparencyState;
    }

    public void setTranslucencyPointOfView(TranslucencyPointOfView pTranslucencyPointOfView) {
        this.translucencyPointOfView = pTranslucencyPointOfView;
    }

    @Override
    public boolean isDifferentPointOfView(TranslucencyPointOfView p_407674_) {
        return !p_407674_.equals(this.translucencyPointOfView);
    }

    @Override
    public boolean hasRenderableLayers() {
        return !this.buffers.isEmpty();
    }

    @Override
    public boolean isEmpty(ChunkSectionLayer p_409252_) {
        return !this.buffers.containsKey(p_409252_);
    }

    @Override
    public List<BlockEntity> getRenderableBlockEntities() {
        return this.renderableBlockEntities;
    }

    @Override
    public boolean facesCanSeeEachother(Direction p_410263_, Direction p_407059_) {
        return this.visibilitySet.visibilityBetween(p_410263_, p_407059_);
    }

    @Nullable
    @Override
    public SectionBuffers getBuffers(ChunkSectionLayer p_409484_) {
        return this.buffers.get(p_409484_);
    }

    public void uploadMeshLayer(ChunkSectionLayer pLayer, MeshData pMeshData, long pSectionNode) {
        CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
        SectionBuffers sectionbuffers = this.getBuffers(pLayer);
        if (sectionbuffers != null) {
            if (sectionbuffers.getVertexBuffer().size() < pMeshData.vertexBuffer().remaining()) {
                sectionbuffers.getVertexBuffer().close();
                sectionbuffers.setVertexBuffer(
                    RenderSystem.getDevice()
                        .createBuffer(
                            () -> "Section vertex buffer - layer: "
                                + pLayer.label()
                                + "; cords: "
                                + SectionPos.x(pSectionNode)
                                + ", "
                                + SectionPos.y(pSectionNode)
                                + ", "
                                + SectionPos.z(pSectionNode),
                            40,
                            pMeshData.vertexBuffer()
                        )
                );
            } else if (!sectionbuffers.getVertexBuffer().isClosed()) {
                commandencoder.writeToBuffer(sectionbuffers.getVertexBuffer().slice(), pMeshData.vertexBuffer());
            }

            ByteBuffer bytebuffer = pMeshData.indexBuffer();
            if (bytebuffer != null) {
                if (sectionbuffers.getIndexBuffer() != null && sectionbuffers.getIndexBuffer().size() >= bytebuffer.remaining()) {
                    if (!sectionbuffers.getIndexBuffer().isClosed()) {
                        commandencoder.writeToBuffer(sectionbuffers.getIndexBuffer().slice(), bytebuffer);
                    }
                } else {
                    if (sectionbuffers.getIndexBuffer() != null) {
                        sectionbuffers.getIndexBuffer().close();
                    }

                    sectionbuffers.setIndexBuffer(
                        RenderSystem.getDevice()
                            .createBuffer(
                                () -> "Section index buffer - layer: "
                                    + pLayer.label()
                                    + "; cords: "
                                    + SectionPos.x(pSectionNode)
                                    + ", "
                                    + SectionPos.y(pSectionNode)
                                    + ", "
                                    + SectionPos.z(pSectionNode),
                                72,
                                bytebuffer
                            )
                    );
                }
            } else if (sectionbuffers.getIndexBuffer() != null) {
                sectionbuffers.getIndexBuffer().close();
                sectionbuffers.setIndexBuffer(null);
            }

            sectionbuffers.setIndexCount(pMeshData.drawState().indexCount());
            sectionbuffers.setIndexType(pMeshData.drawState().indexType());
        } else {
            GpuBuffer gpubuffer1 = RenderSystem.getDevice()
                .createBuffer(
                    () -> "Section vertex buffer - layer: "
                        + pLayer.label()
                        + "; cords: "
                        + SectionPos.x(pSectionNode)
                        + ", "
                        + SectionPos.y(pSectionNode)
                        + ", "
                        + SectionPos.z(pSectionNode),
                    40,
                    pMeshData.vertexBuffer()
                );
            ByteBuffer bytebuffer1 = pMeshData.indexBuffer();
            GpuBuffer gpubuffer = bytebuffer1 != null
                ? RenderSystem.getDevice()
                    .createBuffer(
                        () -> "Section index buffer - layer: "
                            + pLayer.label()
                            + "; cords: "
                            + SectionPos.x(pSectionNode)
                            + ", "
                            + SectionPos.y(pSectionNode)
                            + ", "
                            + SectionPos.z(pSectionNode),
                        72,
                        bytebuffer1
                    )
                : null;
            SectionBuffers sectionbuffers1 = new SectionBuffers(gpubuffer1, gpubuffer, pMeshData.drawState().indexCount(), pMeshData.drawState().indexType());
            this.buffers.put(pLayer, sectionbuffers1);
        }
    }

    public void uploadLayerIndexBuffer(ChunkSectionLayer pLayer, ByteBufferBuilder.Result pResult, long pSectionNode) {
        SectionBuffers sectionbuffers = this.getBuffers(pLayer);
        if (sectionbuffers != null) {
            if (sectionbuffers.getIndexBuffer() == null) {
                sectionbuffers.setIndexBuffer(
                    RenderSystem.getDevice()
                        .createBuffer(
                            () -> "Section index buffer - layer: "
                                + pLayer.label()
                                + "; cords: "
                                + SectionPos.x(pSectionNode)
                                + ", "
                                + SectionPos.y(pSectionNode)
                                + ", "
                                + SectionPos.z(pSectionNode),
                            72,
                            pResult.byteBuffer()
                        )
                );
            } else {
                CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
                if (!sectionbuffers.getIndexBuffer().isClosed()) {
                    commandencoder.writeToBuffer(sectionbuffers.getIndexBuffer().slice(), pResult.byteBuffer());
                }
            }
        }
    }

    @Override
    public boolean hasTranslucentGeometry() {
        return this.buffers.containsKey(ChunkSectionLayer.TRANSLUCENT);
    }

    @Nullable
    public MeshData.SortState getTransparencyState() {
        return this.transparencyState;
    }

    @Override
    public void close() {
        this.buffers.values().forEach(SectionBuffers::close);
        this.buffers.clear();
    }
}