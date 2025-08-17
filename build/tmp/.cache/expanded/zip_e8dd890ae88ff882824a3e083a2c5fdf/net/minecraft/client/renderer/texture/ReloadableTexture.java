package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.IOException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ReloadableTexture extends AbstractTexture {
    private final ResourceLocation resourceId;

    public ReloadableTexture(ResourceLocation pResourceId) {
        this.resourceId = pResourceId;
    }

    public ResourceLocation resourceId() {
        return this.resourceId;
    }

    public void apply(TextureContents pTextureContents) {
        boolean flag = pTextureContents.clamp();
        boolean flag1 = pTextureContents.blur();

        try (NativeImage nativeimage = pTextureContents.image()) {
            this.doLoad(nativeimage, flag1, flag);
        }
    }

    protected void doLoad(NativeImage pImage, boolean pBlur, boolean pClamp) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.close();
        this.texture = gpudevice.createTexture(this.resourceId::toString, 5, TextureFormat.RGBA8, pImage.getWidth(), pImage.getHeight(), 1, 1);
        this.textureView = gpudevice.createTextureView(this.texture);
        this.setFilter(pBlur, false);
        this.setClamp(pClamp);
        gpudevice.createCommandEncoder().writeToTexture(this.texture, pImage);
    }

    public abstract TextureContents loadContents(ResourceManager pResourceManager) throws IOException;
}