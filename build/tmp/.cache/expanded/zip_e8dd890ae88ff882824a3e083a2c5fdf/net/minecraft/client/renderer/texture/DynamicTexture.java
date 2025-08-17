package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DynamicTexture extends AbstractTexture implements Dumpable {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private NativeImage pixels;

    public DynamicTexture(Supplier<String> pLabel, NativeImage pPixels) {
        this.pixels = pPixels;
        this.createTexture(pLabel);
        this.upload();
    }

    public DynamicTexture(String pLabel, int pWidth, int pHeight, boolean pUseCalloc) {
        this.pixels = new NativeImage(pWidth, pHeight, pUseCalloc);
        this.createTexture(pLabel);
    }

    public DynamicTexture(Supplier<String> pLabel, int pWidth, int pHeight, boolean pUseCalloc) {
        this.pixels = new NativeImage(pWidth, pHeight, pUseCalloc);
        this.createTexture(pLabel);
    }

    private void createTexture(Supplier<String> pLabel) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.texture = gpudevice.createTexture(pLabel, 5, TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
        this.texture.setTextureFilter(FilterMode.NEAREST, false);
        this.textureView = gpudevice.createTextureView(this.texture);
    }

    private void createTexture(String pLabel) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.texture = gpudevice.createTexture(pLabel, 5, TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
        this.texture.setTextureFilter(FilterMode.NEAREST, false);
        this.textureView = gpudevice.createTextureView(this.texture);
    }

    public void upload() {
        if (this.pixels != null && this.texture != null) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.pixels);
        } else {
            LOGGER.warn("Trying to upload disposed texture {}", this.getTexture().getLabel());
        }
    }

    @Nullable
    public NativeImage getPixels() {
        return this.pixels;
    }

    public void setPixels(NativeImage pPixels) {
        if (this.pixels != null) {
            this.pixels.close();
        }

        this.pixels = pPixels;
    }

    @Override
    public void close() {
        if (this.pixels != null) {
            this.pixels.close();
            this.pixels = null;
        }

        super.close();
    }

    @Override
    public void dumpContents(ResourceLocation p_276119_, Path p_276105_) throws IOException {
        if (this.pixels != null) {
            String s = p_276119_.toDebugFileName() + ".png";
            Path path = p_276105_.resolve(s);
            this.pixels.writeToFile(path);
        }
    }
}