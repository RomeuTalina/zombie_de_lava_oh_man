package com.mojang.blaze3d.textures;

import com.mojang.blaze3d.DontObfuscate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public abstract class GpuTextureView implements AutoCloseable {
    private final GpuTexture texture;
    private final int baseMipLevel;
    private final int mipLevels;

    public GpuTextureView(GpuTexture pTexture, int pBaseMipLevel, int pMipLevels) {
        this.texture = pTexture;
        this.baseMipLevel = pBaseMipLevel;
        this.mipLevels = pMipLevels;
    }

    @Override
    public abstract void close();

    public GpuTexture texture() {
        return this.texture;
    }

    public int baseMipLevel() {
        return this.baseMipLevel;
    }

    public int mipLevels() {
        return this.mipLevels;
    }

    public int getWidth(int pMipLevel) {
        return this.texture.getWidth(pMipLevel + this.baseMipLevel);
    }

    public int getHeight(int pMipLevel) {
        return this.texture.getHeight(pMipLevel + this.baseMipLevel);
    }

    public abstract boolean isClosed();
}