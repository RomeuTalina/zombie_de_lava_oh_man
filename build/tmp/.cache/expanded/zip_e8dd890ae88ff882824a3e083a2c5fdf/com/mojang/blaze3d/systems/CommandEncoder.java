package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public interface CommandEncoder {
    RenderPass createRenderPass(Supplier<String> pDebugGroup, GpuTextureView pColorTexture, OptionalInt pClearColor);

    RenderPass createRenderPass(
        Supplier<String> pDebugGroup, GpuTextureView pColorTexture, OptionalInt pClearColor, @Nullable GpuTextureView pDepthTexture, OptionalDouble pClearDepth
    );

    void clearColorTexture(GpuTexture pTexture, int pColor);

    void clearColorAndDepthTextures(GpuTexture pColorTexture, int pClearColor, GpuTexture pDepthTexture, double pClearDepth);

    void clearColorAndDepthTextures(
        GpuTexture pColorTexture, int pClearColor, GpuTexture pDepthTexture, double pClearDepth, int pScissorX, int pScissorY, int pScissorWidth, int pScissorHeight
    );

    void clearDepthTexture(GpuTexture pDepthTexture, double pClearDepth);

    void writeToBuffer(GpuBufferSlice pSlice, ByteBuffer pBuffer);

    GpuBuffer.MappedView mapBuffer(GpuBuffer pBuffer, boolean pRead, boolean pWrite);

    GpuBuffer.MappedView mapBuffer(GpuBufferSlice pSlice, boolean pRead, boolean pWrite);

    void copyToBuffer(GpuBufferSlice pSource, GpuBufferSlice pTarget);

    void writeToTexture(GpuTexture pTexture, NativeImage pImage);

    void writeToTexture(
        GpuTexture pTexture,
        NativeImage pImage,
        int pMipLevel,
        int pDepthOrLayer,
        int pX,
        int pY,
        int pWidth,
        int pHeight,
        int pSourceX,
        int pSourceY
    );

    void writeToTexture(
        GpuTexture pTexture,
        IntBuffer pBuffer,
        NativeImage.Format pFormat,
        int pMipLevel,
        int pDepthOrLayer,
        int pX,
        int pY,
        int pWidth,
        int pHeight
    );

    void copyTextureToBuffer(GpuTexture pTexture, GpuBuffer pBuffer, int pOffset, Runnable pTask, int pMipLevel);

    void copyTextureToBuffer(
        GpuTexture pTexture, GpuBuffer pBuffer, int pOffset, Runnable pTask, int pMipLevel, int pX, int pY, int pWidth, int pHeight
    );

    void copyTextureToTexture(
        GpuTexture pSource, GpuTexture pDestination, int pMipLevel, int pX, int pY, int pSourceX, int pSourceY, int pWidth, int pHeight
    );

    void presentTexture(GpuTextureView pTexture);

    GpuFence createFence();
}