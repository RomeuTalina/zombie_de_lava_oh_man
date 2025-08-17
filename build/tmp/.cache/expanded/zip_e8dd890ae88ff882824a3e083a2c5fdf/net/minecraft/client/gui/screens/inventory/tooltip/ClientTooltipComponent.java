package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ClientTooltipComponent {
    static ClientTooltipComponent create(FormattedCharSequence pText) {
        return new ClientTextTooltip(pText);
    }

    static ClientTooltipComponent create(TooltipComponent pVisualTooltipComponent) {
        return (ClientTooltipComponent)(switch (pVisualTooltipComponent) {
            case BundleTooltip bundletooltip -> new ClientBundleTooltip(bundletooltip.contents());
            case ClientActivePlayersTooltip.ActivePlayersTooltip clientactiveplayerstooltip$activeplayerstooltip -> new ClientActivePlayersTooltip(
                clientactiveplayerstooltip$activeplayerstooltip
            );
            default -> net.minecraftforge.client.gui.ClientTooltipComponentManager.createClientTooltipComponent(pVisualTooltipComponent);
        });
    }

    int getHeight(Font pFont);

    int getWidth(Font pFont);

    default boolean showTooltipWithItemInHand() {
        return false;
    }

    default void renderText(GuiGraphics pGuiGraphics, Font pFont, int pX, int pY) {
    }

    default void renderImage(Font pFont, int pX, int pY, int pWidth, int pHeight, GuiGraphics pGuiGraphics) {
    }
}
