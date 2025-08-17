package net.minecraft.client.gui.contextualbar;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ContextualBarRenderer {
    int WIDTH = 182;
    int HEIGHT = 5;
    int MARGIN_BOTTOM = 24;
    ContextualBarRenderer EMPTY = new ContextualBarRenderer() {
        @Override
        public void renderBackground(GuiGraphics p_408134_, DeltaTracker p_409772_) {
        }

        @Override
        public void render(GuiGraphics p_409095_, DeltaTracker p_407067_) {
        }
    };

    default int left(Window pWindow) {
        return (pWindow.getGuiScaledWidth() - 182) / 2;
    }

    default int top(Window pWindow) {
        return pWindow.getGuiScaledHeight() - 24 - 5;
    }

    void renderBackground(GuiGraphics pGuiGraphics, DeltaTracker pDeltaTracker);

    void render(GuiGraphics pGuiGraphics, DeltaTracker pDeltaTracker);

    static void renderExperienceLevel(GuiGraphics pGuiGraphics, Font pFont, int pLevel) {
        Component component = Component.translatable("gui.experience.level", pLevel);
        int i = (pGuiGraphics.guiWidth() - pFont.width(component)) / 2;
        int j = pGuiGraphics.guiHeight() - 24 - 9 - 2;
        pGuiGraphics.drawString(pFont, component, i + 1, j, -16777216, false);
        pGuiGraphics.drawString(pFont, component, i - 1, j, -16777216, false);
        pGuiGraphics.drawString(pFont, component, i, j + 1, -16777216, false);
        pGuiGraphics.drawString(pFont, component, i, j - 1, -16777216, false);
        pGuiGraphics.drawString(pFont, component, i, j, -8323296, false);
    }
}