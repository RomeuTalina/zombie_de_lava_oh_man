package net.minecraft.client.gui.components;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SplashRenderer {
    public static final SplashRenderer CHRISTMAS = new SplashRenderer("Merry X-mas!");
    public static final SplashRenderer NEW_YEAR = new SplashRenderer("Happy new year!");
    public static final SplashRenderer HALLOWEEN = new SplashRenderer("OOoooOOOoooo! Spooky!");
    private static final int WIDTH_OFFSET = 123;
    private static final int HEIGH_OFFSET = 69;
    private final String splash;

    public SplashRenderer(String pSplash) {
        this.splash = pSplash;
    }

    public void render(GuiGraphics pGuiGraphics, int pWidth, Font pFont, float pFade) {
        pGuiGraphics.pose().pushMatrix();
        pGuiGraphics.pose().translate(pWidth / 2.0F + 123.0F, 69.0F);
        pGuiGraphics.pose().rotate((float) (-Math.PI / 9));
        float f = 1.8F - Mth.abs(Mth.sin((float)(Util.getMillis() % 1000L) / 1000.0F * (float) (Math.PI * 2)) * 0.1F);
        f = f * 100.0F / (pFont.width(this.splash) + 32);
        pGuiGraphics.pose().scale(f, f);
        pGuiGraphics.drawCenteredString(pFont, this.splash, 0, -8, ARGB.color(pFade, -256));
        pGuiGraphics.pose().popMatrix();
    }
}