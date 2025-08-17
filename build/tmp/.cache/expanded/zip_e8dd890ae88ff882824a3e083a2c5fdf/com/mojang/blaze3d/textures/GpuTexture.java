package com.mojang.blaze3d.textures;

import com.mojang.blaze3d.DontObfuscate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public abstract class GpuTexture implements AutoCloseable, net.minecraftforge.client.extensions.IForgeGpuTexture {
    public static final int USAGE_COPY_DST = 1;
    public static final int USAGE_COPY_SRC = 2;
    public static final int USAGE_TEXTURE_BINDING = 4;
    public static final int USAGE_RENDER_ATTACHMENT = 8;
    public static final int USAGE_CUBEMAP_COMPATIBLE = 16;
    private final TextureFormat format;
    private final int width;
    private final int height;
    private final int depthOrLayers;
    private final int mipLevels;
    private final int usage;
    private final String label;
    protected AddressMode addressModeU = AddressMode.REPEAT;
    protected AddressMode addressModeV = AddressMode.REPEAT;
    protected FilterMode minFilter = FilterMode.NEAREST;
    protected FilterMode magFilter = FilterMode.LINEAR;
    protected boolean useMipmaps = true;

    public GpuTexture(int pUsage, String pLabel, TextureFormat pFormat, int pWidth, int pHeight, int pDepthOrLayers, int pMipLevels) {
        this.usage = pUsage;
        this.label = pLabel;
        this.format = pFormat;
        this.width = pWidth;
        this.height = pHeight;
        this.depthOrLayers = pDepthOrLayers;
        this.mipLevels = pMipLevels;
    }

    public int getWidth(int pMipLevel) {
        return this.width >> pMipLevel;
    }

    public int getHeight(int pMipLevel) {
        return this.height >> pMipLevel;
    }

    public int getDepthOrLayers() {
        return this.depthOrLayers;
    }

    public int getMipLevels() {
        return this.mipLevels;
    }

    public TextureFormat getFormat() {
        return this.format;
    }

    public int usage() {
        return this.usage;
    }

    public void setAddressMode(AddressMode pAddressMode) {
        this.setAddressMode(pAddressMode, pAddressMode);
    }

    public void setAddressMode(AddressMode pAddressModeU, AddressMode pAddressModeV) {
        this.addressModeU = pAddressModeU;
        this.addressModeV = pAddressModeV;
    }

    public void setTextureFilter(FilterMode pFilter, boolean pUseMipmaps) {
        this.setTextureFilter(pFilter, pFilter, pUseMipmaps);
    }

    public void setTextureFilter(FilterMode pMinFilter, FilterMode pMagFilter, boolean pUseMipmaps) {
        this.minFilter = pMinFilter;
        this.magFilter = pMagFilter;
        this.setUseMipmaps(pUseMipmaps);
    }

    public void setUseMipmaps(boolean pUseMipmaps) {
        this.useMipmaps = pUseMipmaps;
    }

    public String getLabel() {
        return this.label;
    }

    @Override
    public abstract void close();

    public abstract boolean isClosed();
}
