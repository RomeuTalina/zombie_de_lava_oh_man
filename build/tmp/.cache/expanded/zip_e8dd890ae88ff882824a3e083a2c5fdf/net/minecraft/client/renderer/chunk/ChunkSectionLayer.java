package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum ChunkSectionLayer {
    SOLID(RenderPipelines.SOLID, 4194304, true, false),
    CUTOUT_MIPPED(RenderPipelines.CUTOUT_MIPPED, 4194304, true, false),
    CUTOUT(RenderPipelines.CUTOUT, 786432, false, false),
    TRANSLUCENT(RenderPipelines.TRANSLUCENT, 786432, true, true),
    TRIPWIRE(RenderPipelines.TRIPWIRE, 1536, true, true);

    private final RenderPipeline pipeline;
    private final int bufferSize;
    private final boolean useMipmaps;
    private final boolean sortOnUpload;
    private final String label;

    private ChunkSectionLayer(final RenderPipeline pPipeline, final int pBufferSize, final boolean pUseMipmaps, final boolean pSortOnUpload) {
        this.pipeline = pPipeline;
        this.bufferSize = pBufferSize;
        this.useMipmaps = pUseMipmaps;
        this.sortOnUpload = pSortOnUpload;
        this.label = this.toString().toLowerCase(Locale.ROOT);
    }

    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    public int bufferSize() {
        return this.bufferSize;
    }

    public String label() {
        return this.label;
    }

    public boolean sortOnUpload() {
        return this.sortOnUpload;
    }

    public GpuTextureView textureView() {
        TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
        AbstractTexture abstracttexture = texturemanager.getTexture(TextureAtlas.LOCATION_BLOCKS);
        abstracttexture.setUseMipmaps(this.useMipmaps);
        return abstracttexture.getTextureView();
    }

    public RenderTarget outputTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        switch (this) {
            case TRANSLUCENT:
                RenderTarget rendertarget1 = minecraft.levelRenderer.getTranslucentTarget();
                return rendertarget1 != null ? rendertarget1 : minecraft.getMainRenderTarget();
            case TRIPWIRE:
                RenderTarget rendertarget = minecraft.levelRenderer.getWeatherTarget();
                return rendertarget != null ? rendertarget : minecraft.getMainRenderTarget();
            default:
                return minecraft.getMainRenderTarget();
        }
    }
}