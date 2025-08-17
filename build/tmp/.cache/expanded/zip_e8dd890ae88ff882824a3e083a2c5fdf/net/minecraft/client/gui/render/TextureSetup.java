package net.minecraft.client.gui.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record TextureSetup(@Nullable GpuTextureView texure0, @Nullable GpuTextureView texure1, @Nullable GpuTextureView texure2) {
    private static final TextureSetup NO_TEXTURE_SETUP = new TextureSetup(null, null, null);
    private static int sortKeySeed;

    public static TextureSetup singleTexture(GpuTextureView pTexture) {
        return new TextureSetup(pTexture, null, null);
    }

    public static TextureSetup singleTextureWithLightmap(GpuTextureView pTexture) {
        return new TextureSetup(pTexture, null, Minecraft.getInstance().gameRenderer.lightTexture().getTextureView());
    }

    public static TextureSetup doubleTexture(GpuTextureView pTexture1, GpuTextureView pTexture2) {
        return new TextureSetup(pTexture1, pTexture2, null);
    }

    public static TextureSetup noTexture() {
        return NO_TEXTURE_SETUP;
    }

    public int getSortKey() {
        return this.hashCode();
    }

    public static void updateSortKeySeed() {
        sortKeySeed = Math.round(100000.0F * (float)Math.random());
    }
}