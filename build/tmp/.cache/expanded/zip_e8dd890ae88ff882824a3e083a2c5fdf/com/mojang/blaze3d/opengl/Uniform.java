package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public sealed interface Uniform extends AutoCloseable permits Uniform.Ubo, Uniform.Utb, Uniform.Sampler {
    @Override
    default void close() {
    }

    @OnlyIn(Dist.CLIENT)
    public record Sampler(int location, int samplerIndex) implements Uniform {
    }

    @OnlyIn(Dist.CLIENT)
    public record Ubo(int blockBinding) implements Uniform {
    }

    @OnlyIn(Dist.CLIENT)
    public record Utb(int location, int samplerIndex, TextureFormat format, int texture) implements Uniform {
        public Utb(int pLocation, int pSamplerIndex, TextureFormat pFormat) {
            this(pLocation, pSamplerIndex, pFormat, GlStateManager._genTexture());
        }

        @Override
        public void close() {
            GlStateManager._deleteTexture(this.texture);
        }
    }
}