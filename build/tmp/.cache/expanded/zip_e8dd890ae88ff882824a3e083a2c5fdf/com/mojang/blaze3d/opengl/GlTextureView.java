package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlTextureView extends GpuTextureView {
    private boolean closed;

    protected GlTextureView(GlTexture pTexture, int pBaseMipLevel, int pMipLevels) {
        super(pTexture, pBaseMipLevel, pMipLevels);
        pTexture.addViews();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.texture().removeViews();
        }
    }

    public GlTexture texture() {
        return (GlTexture)super.texture();
    }
}