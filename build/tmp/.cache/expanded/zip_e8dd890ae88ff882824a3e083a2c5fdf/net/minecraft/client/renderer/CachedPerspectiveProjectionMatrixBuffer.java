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
public class CachedPerspectiveProjectionMatrixBuffer implements AutoCloseable {
    private final GpuBuffer buffer;
    private final GpuBufferSlice bufferSlice;
    private final float zNear;
    private final float zFar;
    private int width;
    private int height;
    private float fov;

    public CachedPerspectiveProjectionMatrixBuffer(String pLabel, float pZNear, float pZFar) {
        this.zNear = pZNear;
        this.zFar = pZFar;
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.buffer = gpudevice.createBuffer(() -> "Projection matrix UBO " + pLabel, 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
        this.bufferSlice = this.buffer.slice(0, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
    }

    public GpuBufferSlice getBuffer(int pWidth, int pHeight, float pFov) {
        if (this.width != pWidth || this.height != pHeight || this.fov != pFov) {
            Matrix4f matrix4f = this.createProjectionMatrix(pWidth, pHeight, pFov);

            try (MemoryStack memorystack = MemoryStack.stackPush()) {
                ByteBuffer bytebuffer = Std140Builder.onStack(memorystack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE).putMat4f(matrix4f).get();
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), bytebuffer);
            }

            this.width = pWidth;
            this.height = pHeight;
            this.fov = pFov;
        }

        return this.bufferSlice;
    }

    private Matrix4f createProjectionMatrix(int pWidth, int pHeight, float pFov) {
        return new Matrix4f().perspective(pFov * (float) (Math.PI / 180.0), (float)pWidth / pHeight, this.zNear, this.zFar);
    }

    @Override
    public void close() {
        this.buffer.close();
    }
}