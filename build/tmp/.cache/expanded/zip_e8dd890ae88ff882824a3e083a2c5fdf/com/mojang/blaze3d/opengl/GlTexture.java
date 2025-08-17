package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlTexture extends GpuTexture {
    protected final int id;
    private final Int2IntMap fboCache = new Int2IntOpenHashMap();
    protected boolean closed;
    protected boolean modesDirty = true;
    private int views;

    protected GlTexture(int pUsage, String pLabel, TextureFormat pFormat, int pWidth, int pHeight, int pDepthOrLayers, int pMipLevels, int pId) {
        this(pUsage, pLabel, pFormat, pWidth, pHeight, pDepthOrLayers, pMipLevels, pId, false);
    }

    protected GlTexture(int pUsage, String pLabel, TextureFormat pFormat, int pWidth, int pHeight, int pDepthOrLayers, int pMipLevels, int pId, boolean stencil) {
        super(pUsage, pLabel, pFormat, pWidth, pHeight, pDepthOrLayers, pMipLevels);
        this.id = pId;
        this.stencilEnabled = stencil;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            if (this.views == 0) {
                this.destroyImmediately();
            }
        }
    }

    private void destroyImmediately() {
        GlStateManager._deleteTexture(this.id);

        for (int i : this.fboCache.values()) {
            GlStateManager._glDeleteFramebuffers(i);
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    public int getFbo(DirectStateAccess pDirectStateAccess, @Nullable GpuTexture pTexture) {
        int i = pTexture == null ? 0 : ((GlTexture)pTexture).id;
        return this.fboCache.computeIfAbsent(i, p_393965_ -> {
            int j = pDirectStateAccess.createFrameBufferObject();
            pDirectStateAccess.bindFrameBufferTextures(j, this.id, i, 0, 0);
            return j;
        });
    }

    public void flushModeChanges(int pTarget) {
        if (this.modesDirty) {
            GlStateManager._texParameter(pTarget, 10242, GlConst.toGl(this.addressModeU));
            GlStateManager._texParameter(pTarget, 10243, GlConst.toGl(this.addressModeV));
            switch (this.minFilter) {
                case NEAREST:
                    GlStateManager._texParameter(pTarget, 10241, this.useMipmaps ? 9986 : 9728);
                    break;
                case LINEAR:
                    GlStateManager._texParameter(pTarget, 10241, this.useMipmaps ? 9987 : 9729);
            }

            switch (this.magFilter) {
                case NEAREST:
                    GlStateManager._texParameter(pTarget, 10240, 9728);
                    break;
                case LINEAR:
                    GlStateManager._texParameter(pTarget, 10240, 9729);
            }

            this.modesDirty = false;
        }
    }

    public int glId() {
        return this.id;
    }

    @Override
    public void setAddressMode(AddressMode p_393262_, AddressMode p_392605_) {
        super.setAddressMode(p_393262_, p_392605_);
        this.modesDirty = true;
    }

    @Override
    public void setTextureFilter(FilterMode p_394977_, FilterMode p_397175_, boolean p_393558_) {
        super.setTextureFilter(p_394977_, p_397175_, p_393558_);
        this.modesDirty = true;
    }


    /* FORGE START */

    private final boolean stencilEnabled;

    @Override
    public boolean isStencilEnabled() {
        return this.stencilEnabled;
    }

    @Override
    public void setUseMipmaps(boolean p_406183_) {
        super.setUseMipmaps(p_406183_);
        this.modesDirty = true;
    }

    public void addViews() {
        this.views++;
    }

    public void removeViews() {
        this.views--;
        if (this.closed && this.views == 0) {
            this.destroyImmediately();
        }
    }
}
