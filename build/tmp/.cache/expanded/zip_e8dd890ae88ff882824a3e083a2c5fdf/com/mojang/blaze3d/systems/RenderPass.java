package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public interface RenderPass extends AutoCloseable {
    void pushDebugGroup(Supplier<String> pName);

    void popDebugGroup();

    void setPipeline(RenderPipeline pPipeline);

    void bindSampler(String pName, @Nullable GpuTextureView pTexture);

    void setUniform(String pName, GpuBuffer pBuffer);

    void setUniform(String pName, GpuBufferSlice pBufferSlice);

    void enableScissor(int pX, int pY, int pWidth, int pHeight);

    void disableScissor();

    void setVertexBuffer(int pIndex, GpuBuffer pBuffer);

    void setIndexBuffer(GpuBuffer pIndexBuffer, VertexFormat.IndexType pIndexType);

    void drawIndexed(int pFirstIndex, int pIndex, int pIndexCount, int pPrimCount);

    <T> void drawMultipleIndexed(
        Collection<RenderPass.Draw<T>> pDraws,
        @Nullable GpuBuffer pIndexBuffer,
        @Nullable VertexFormat.IndexType pIndexType,
        Collection<String> pUniformNames,
        T pUserData
    );

    void draw(int pFirstIndex, int pIndexCount);

    @Override
    void close();

    @OnlyIn(Dist.CLIENT)
    public record Draw<T>(
        int slot,
        GpuBuffer vertexBuffer,
        @Nullable GpuBuffer indexBuffer,
        @Nullable VertexFormat.IndexType indexType,
        int firstIndex,
        int indexCount,
        @Nullable BiConsumer<T, RenderPass.UniformUploader> uniformUploaderConsumer
    ) {
        public Draw(int pSlot, GpuBuffer pVertexBuffer, GpuBuffer pIndexBuffer, VertexFormat.IndexType pIndexType, int pFirstIndex, int pIndexCount) {
            this(pSlot, pVertexBuffer, pIndexBuffer, pIndexType, pFirstIndex, pIndexCount, null);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface UniformUploader {
        void upload(String pName, GpuBufferSlice pBufferSlice);
    }
}