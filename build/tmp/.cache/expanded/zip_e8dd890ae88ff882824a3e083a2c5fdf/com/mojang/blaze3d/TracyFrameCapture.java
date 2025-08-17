package com.mojang.blaze3d;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.jtracy.TracyClient;
import java.util.OptionalInt;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TracyFrameCapture implements AutoCloseable {
    private static final int MAX_WIDTH = 320;
    private static final int MAX_HEIGHT = 180;
    private static final int BYTES_PER_PIXEL = 4;
    private int targetWidth;
    private int targetHeight;
    private int width;
    private int height;
    private GpuTexture frameBuffer;
    private GpuTextureView frameBufferView;
    private GpuBuffer pixelbuffer;
    private int lastCaptureDelay;
    private boolean capturedThisFrame;
    private TracyFrameCapture.Status status = TracyFrameCapture.Status.WAITING_FOR_CAPTURE;

    public TracyFrameCapture() {
        this.width = 320;
        this.height = 180;
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.frameBuffer = gpudevice.createTexture("Tracy Frame Capture", 10, TextureFormat.RGBA8, this.width, this.height, 1, 1);
        this.frameBufferView = gpudevice.createTextureView(this.frameBuffer);
        this.pixelbuffer = gpudevice.createBuffer(() -> "Tracy Frame Capture buffer", 9, this.width * this.height * 4);
    }

    private void resize(int pWidth, int pHeight) {
        float f = (float)pWidth / pHeight;
        if (pWidth > 320) {
            pWidth = 320;
            pHeight = (int)(320.0F / f);
        }

        if (pHeight > 180) {
            pWidth = (int)(180.0F * f);
            pHeight = 180;
        }

        pWidth = pWidth / 4 * 4;
        pHeight = pHeight / 4 * 4;
        if (this.width != pWidth || this.height != pHeight) {
            this.width = pWidth;
            this.height = pHeight;
            GpuDevice gpudevice = RenderSystem.getDevice();
            this.frameBuffer.close();
            this.frameBuffer = gpudevice.createTexture("Tracy Frame Capture", 10, TextureFormat.RGBA8, pWidth, pHeight, 1, 1);
            this.frameBufferView.close();
            this.frameBufferView = gpudevice.createTextureView(this.frameBuffer);
            this.pixelbuffer.close();
            this.pixelbuffer = gpudevice.createBuffer(() -> "Tracy Frame Capture buffer", 9, pWidth * pHeight * 4);
        }
    }

    public void capture(RenderTarget pRenderTarget) {
        if (this.status == TracyFrameCapture.Status.WAITING_FOR_CAPTURE && !this.capturedThisFrame && pRenderTarget.getColorTexture() != null) {
            this.capturedThisFrame = true;
            if (pRenderTarget.width != this.targetWidth || pRenderTarget.height != this.targetHeight) {
                this.targetWidth = pRenderTarget.width;
                this.targetHeight = pRenderTarget.height;
                this.resize(this.targetWidth, this.targetHeight);
            }

            this.status = TracyFrameCapture.Status.WAITING_FOR_COPY;
            CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
            RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            GpuBuffer gpubuffer = rendersystem$autostorageindexbuffer.getBuffer(6);

            try (RenderPass renderpass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "Tracy blit", this.frameBufferView, OptionalInt.empty())) {
                renderpass.setPipeline(RenderPipelines.TRACY_BLIT);
                renderpass.setVertexBuffer(0, RenderSystem.getQuadVertexBuffer());
                renderpass.setIndexBuffer(gpubuffer, rendersystem$autostorageindexbuffer.type());
                renderpass.bindSampler("InSampler", pRenderTarget.getColorTextureView());
                renderpass.drawIndexed(0, 0, 6, 1);
            }

            commandencoder.copyTextureToBuffer(this.frameBuffer, this.pixelbuffer, 0, () -> this.status = TracyFrameCapture.Status.WAITING_FOR_UPLOAD, 0);
            this.lastCaptureDelay = 0;
        }
    }

    public void upload() {
        if (this.status == TracyFrameCapture.Status.WAITING_FOR_UPLOAD) {
            this.status = TracyFrameCapture.Status.WAITING_FOR_CAPTURE;

            try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.pixelbuffer, true, false)) {
                TracyClient.frameImage(gpubuffer$mappedview.data(), this.width, this.height, this.lastCaptureDelay, true);
            }
        }
    }

    public void endFrame() {
        this.lastCaptureDelay++;
        this.capturedThisFrame = false;
        TracyClient.markFrame();
    }

    @Override
    public void close() {
        this.frameBuffer.close();
        this.frameBufferView.close();
        this.pixelbuffer.close();
    }

    @OnlyIn(Dist.CLIENT)
    static enum Status {
        WAITING_FOR_CAPTURE,
        WAITING_FOR_COPY,
        WAITING_FOR_UPLOAD;
    }
}