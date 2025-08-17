package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.IOException;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CubeMapTexture extends ReloadableTexture {
    private static final String[] SUFFIXES = new String[]{"_1.png", "_3.png", "_5.png", "_4.png", "_0.png", "_2.png"};

    public CubeMapTexture(ResourceLocation p_406700_) {
        super(p_406700_);
    }

    @Override
    public TextureContents loadContents(ResourceManager p_407535_) throws IOException {
        ResourceLocation resourcelocation = this.resourceId();

        TextureContents texturecontents2;
        try (TextureContents texturecontents = TextureContents.load(p_407535_, resourcelocation.withSuffix(SUFFIXES[0]))) {
            int i = texturecontents.image().getWidth();
            int j = texturecontents.image().getHeight();
            NativeImage nativeimage = new NativeImage(i, j * 6, false);
            texturecontents.image().copyRect(nativeimage, 0, 0, 0, 0, i, j, false, true);

            for (int k = 1; k < 6; k++) {
                try (TextureContents texturecontents1 = TextureContents.load(p_407535_, resourcelocation.withSuffix(SUFFIXES[k]))) {
                    if (texturecontents1.image().getWidth() != i || texturecontents1.image().getHeight() != j) {
                        throw new IOException(
                            "Image dimensions of cubemap '"
                                + resourcelocation
                                + "' sides do not match: part 0 is "
                                + i
                                + "x"
                                + j
                                + ", but part "
                                + k
                                + " is "
                                + texturecontents1.image().getWidth()
                                + "x"
                                + texturecontents1.image().getHeight()
                        );
                    }

                    texturecontents1.image().copyRect(nativeimage, 0, 0, 0, k * j, i, j, false, true);
                }
            }

            texturecontents2 = new TextureContents(nativeimage, new TextureMetadataSection(true, false));
        }

        return texturecontents2;
    }

    @Override
    protected void doLoad(NativeImage p_408893_, boolean p_408110_, boolean p_407721_) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        int i = p_408893_.getWidth();
        int j = p_408893_.getHeight() / 6;
        this.close();
        this.texture = gpudevice.createTexture(this.resourceId()::toString, 21, TextureFormat.RGBA8, i, j, 6, 1);
        this.textureView = gpudevice.createTextureView(this.texture);
        this.setFilter(p_408110_, false);
        this.setClamp(p_407721_);

        for (int k = 0; k < 6; k++) {
            gpudevice.createCommandEncoder().writeToTexture(this.texture, p_408893_, 0, k, 0, 0, i, j, 0, j * k);
        }
    }
}