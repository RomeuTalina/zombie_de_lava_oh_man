package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PanoramaRenderer {
    public static final ResourceLocation PANORAMA_OVERLAY = ResourceLocation.withDefaultNamespace("textures/gui/title/background/panorama_overlay.png");
    private final Minecraft minecraft;
    private final CubeMap cubeMap;
    private float spin;

    public PanoramaRenderer(CubeMap pCubeMap) {
        this.cubeMap = pCubeMap;
        this.minecraft = Minecraft.getInstance();
    }

    public void render(GuiGraphics pGuiGraphics, int pWidth, int pHeight, boolean pSpin) {
        if (pSpin) {
            float f = this.minecraft.getDeltaTracker().getRealtimeDeltaTicks();
            float f1 = (float)(f * this.minecraft.options.panoramaSpeed().get());
            this.spin = wrap(this.spin + f1 * 0.1F, 360.0F);
        }

        this.cubeMap.render(this.minecraft, 10.0F, -this.spin);
        pGuiGraphics.blit(RenderPipelines.GUI_TEXTURED, PANORAMA_OVERLAY, 0, 0, 0.0F, 0.0F, pWidth, pHeight, 16, 128, 16, 128);
    }

    private static float wrap(float pValue, float pMax) {
        return pValue > pMax ? pValue - pMax : pValue;
    }

    public void registerTextures(TextureManager pTextureManager) {
        this.cubeMap.registerTextures(pTextureManager);
    }
}