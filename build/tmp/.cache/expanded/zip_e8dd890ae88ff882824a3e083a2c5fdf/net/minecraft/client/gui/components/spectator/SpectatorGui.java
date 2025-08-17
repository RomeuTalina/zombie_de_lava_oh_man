package net.minecraft.client.gui.components.spectator;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.gui.spectator.SpectatorMenuListener;
import net.minecraft.client.gui.spectator.categories.SpectatorPage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpectatorGui implements SpectatorMenuListener {
    private static final ResourceLocation HOTBAR_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation HOTBAR_SELECTION_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
    private static final long FADE_OUT_DELAY = 5000L;
    private static final long FADE_OUT_TIME = 2000L;
    private final Minecraft minecraft;
    private long lastSelectionTime;
    @Nullable
    private SpectatorMenu menu;

    public SpectatorGui(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
    }

    public void onHotbarSelected(int pSlot) {
        this.lastSelectionTime = Util.getMillis();
        if (this.menu != null) {
            this.menu.selectSlot(pSlot);
        } else {
            this.menu = new SpectatorMenu(this);
        }
    }

    private float getHotbarAlpha() {
        long i = this.lastSelectionTime - Util.getMillis() + 5000L;
        return Mth.clamp((float)i / 2000.0F, 0.0F, 1.0F);
    }

    public void renderHotbar(GuiGraphics pGuiGraphics) {
        if (this.menu != null) {
            float f = this.getHotbarAlpha();
            if (f <= 0.0F) {
                this.menu.exit();
            } else {
                int i = pGuiGraphics.guiWidth() / 2;
                int j = Mth.floor(pGuiGraphics.guiHeight() - 22.0F * f);
                SpectatorPage spectatorpage = this.menu.getCurrentPage();
                this.renderPage(pGuiGraphics, f, i, j, spectatorpage);
            }
        }
    }

    protected void renderPage(GuiGraphics pGuiGraphics, float pAlpha, int pX, int pY, SpectatorPage pSpectatorPage) {
        int i = ARGB.white(pAlpha);
        pGuiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, pX - 91, pY, 182, 22, i);
        if (pSpectatorPage.getSelectedSlot() >= 0) {
            pGuiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE, pX - 91 - 1 + pSpectatorPage.getSelectedSlot() * 20, pY - 1, 24, 23, i);
        }

        for (int j = 0; j < 9; j++) {
            this.renderSlot(pGuiGraphics, j, pGuiGraphics.guiWidth() / 2 - 90 + j * 20 + 2, pY + 3, pAlpha, pSpectatorPage.getItem(j));
        }
    }

    private void renderSlot(GuiGraphics pGuiGraphics, int pSlot, int pX, float pY, float pAlpha, SpectatorMenuItem pSpectatorMenuItem) {
        if (pSpectatorMenuItem != SpectatorMenu.EMPTY_SLOT) {
            pGuiGraphics.pose().pushMatrix();
            pGuiGraphics.pose().translate(pX, pY);
            float f = pSpectatorMenuItem.isEnabled() ? 1.0F : 0.25F;
            pSpectatorMenuItem.renderIcon(pGuiGraphics, f, pAlpha);
            pGuiGraphics.pose().popMatrix();
            if (pAlpha > 0.0F && pSpectatorMenuItem.isEnabled()) {
                Component component = this.minecraft.options.keyHotbarSlots[pSlot].getTranslatedKeyMessage();
                pGuiGraphics.drawString(
                    this.minecraft.font,
                    component,
                    pX + 19 - 2 - this.minecraft.font.width(component),
                    (int)pY + 6 + 3,
                    ARGB.color(pAlpha, -1)
                );
            }
        }
    }

    public void renderAction(GuiGraphics pGuiGraphics) {
        float f = this.getHotbarAlpha();
        if (f > 0.0F && this.menu != null) {
            SpectatorMenuItem spectatormenuitem = this.menu.getSelectedItem();
            Component component = spectatormenuitem == SpectatorMenu.EMPTY_SLOT ? this.menu.getSelectedCategory().getPrompt() : spectatormenuitem.getName();
            int i = this.minecraft.font.width(component);
            int j = (pGuiGraphics.guiWidth() - i) / 2;
            int k = pGuiGraphics.guiHeight() - 35;
            pGuiGraphics.drawStringWithBackdrop(this.minecraft.font, component, j, k, i, ARGB.color(f, -1));
        }
    }

    @Override
    public void onSpectatorMenuClosed(SpectatorMenu pMenu) {
        this.menu = null;
        this.lastSelectionTime = 0L;
    }

    public boolean isMenuActive() {
        return this.menu != null;
    }

    public void onMouseScrolled(int pAmount) {
        int i = this.menu.getSelectedSlot() + pAmount;

        while (i >= 0 && i <= 8 && (this.menu.getItem(i) == SpectatorMenu.EMPTY_SLOT || !this.menu.getItem(i).isEnabled())) {
            i += pAmount;
        }

        if (i >= 0 && i <= 8) {
            this.menu.selectSlot(i);
            this.lastSelectionTime = Util.getMillis();
        }
    }

    public void onMouseMiddleClick() {
        this.lastSelectionTime = Util.getMillis();
        if (this.isMenuActive()) {
            int i = this.menu.getSelectedSlot();
            if (i != -1) {
                this.menu.selectSlot(i);
            }
        } else {
            this.menu = new SpectatorMenu(this);
        }
    }
}