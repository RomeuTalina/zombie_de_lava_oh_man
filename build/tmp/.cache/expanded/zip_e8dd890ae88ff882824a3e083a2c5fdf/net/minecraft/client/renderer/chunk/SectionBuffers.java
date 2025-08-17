package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SectionBuffers implements AutoCloseable {
    private GpuBuffer vertexBuffer;
    @Nullable
    private GpuBuffer indexBuffer;
    private int indexCount;
    private VertexFormat.IndexType indexType;

    public SectionBuffers(GpuBuffer pVertexBuffer, @Nullable GpuBuffer pIndexBuffer, int pIndexCount, VertexFormat.IndexType pIndexType) {
        this.vertexBuffer = pVertexBuffer;
        this.indexBuffer = pIndexBuffer;
        this.indexCount = pIndexCount;
        this.indexType = pIndexType;
    }

    public GpuBuffer getVertexBuffer() {
        return this.vertexBuffer;
    }

    @Nullable
    public GpuBuffer getIndexBuffer() {
        return this.indexBuffer;
    }

    public void setIndexBuffer(@Nullable GpuBuffer pIndexBuffer) {
        this.indexBuffer = pIndexBuffer;
    }

    public int getIndexCount() {
        return this.indexCount;
    }

    public VertexFormat.IndexType getIndexType() {
        return this.indexType;
    }

    public void setIndexType(VertexFormat.IndexType pIndexType) {
        this.indexType = pIndexType;
    }

    public void setIndexCount(int pIndexCount) {
        this.indexCount = pIndexCount;
    }

    public void setVertexBuffer(GpuBuffer pVertexBuffer) {
        this.vertexBuffer = pVertexBuffer;
    }

    @Override
    public void close() {
        this.vertexBuffer.close();
        if (this.indexBuffer != null) {
            this.indexBuffer.close();
        }
    }
}