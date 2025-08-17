package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class CachedOrthoProjectionMatrixBuffer implements AutoCloseable {
    private final GpuBuffer buffer;
    private final GpuBufferSlice bufferSlice;
    private final float zNear;
    private final float zFar;
    private final boolean invertY;
    private float width;
    private float height;

    public CachedOrthoProjectionMatrixBuffer(String pLabel, float pZNear, float pZFar, boolean pInvertY) {
        this.zNear = pZNear;
        this.zFar = pZFar;
        this.invertY = pInvertY;
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.buffer = gpudevice.createBuffer(() -> "Projection matrix UBO " + pLabel, 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
        this.bufferSlice = this.buffer.slice(0, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
    }

    public GpuBufferSlice getBuffer(float pWidth, float pHeight) {
        if (this.width != pWidth || this.height != pHeight) {
            Matrix4f matrix4f = this.createProjectionMatrix(pWidth, pHeight);

            try (MemoryStack memorystack = MemoryStack.stackPush()) {
                ByteBuffer bytebuffer = Std140Builder.onStack(memorystack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE).putMat4f(matrix4f).get();
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), bytebuffer);
            }

            this.width = pWidth;
            this.height = pHeight;
        }

        return this.bufferSlice;
    }

    private Matrix4f createProjectionMatrix(float pWidth, float pHeight) {
        return new Matrix4f().setOrtho(0.0F, pWidth, this.invertY ? pHeight : 0.0F, this.invertY ? 0.0F : pHeight, this.zNear, this.zFar);
    }

    @Override
    public void close() {
        this.buffer.close();
    }
}