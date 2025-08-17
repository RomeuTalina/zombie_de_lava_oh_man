package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemDisplayWidget extends AbstractWidget {
    private final Minecraft minecraft;
    private final int offsetX;
    private final int offsetY;
    private final ItemStack itemStack;
    private final boolean decorations;
    private final boolean tooltip;

    public ItemDisplayWidget(
        Minecraft pMinecraft,
        int pOffsetX,
        int pOffsetY,
        int pWidth,
        int pHeight,
        Component pMessage,
        ItemStack pItemStack,
        boolean pDecorations,
        boolean pTooltip
    ) {
        super(0, 0, pWidth, pHeight, pMessage);
        this.minecraft = pMinecraft;
        this.offsetX = pOffsetX;
        this.offsetY = pOffsetY;
        this.itemStack = pItemStack;
        this.decorations = pDecorations;
        this.tooltip = pTooltip;
    }

    @Override
    protected void renderWidget(GuiGraphics p_406193_, int p_406793_, int p_407747_, float p_407971_) {
        p_406193_.renderItem(this.itemStack, this.getX() + this.offsetX, this.getY() + this.offsetY, 0);
        if (this.decorations) {
            p_406193_.renderItemDecorations(this.minecraft.font, this.itemStack, this.getX() + this.offsetX, this.getY() + this.offsetY, null);
        }

        if (this.isFocused()) {
            p_406193_.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), -1);
        }

        if (this.tooltip && this.isHoveredOrFocused()) {
            p_406193_.setTooltipForNextFrame(this.minecraft.font, this.itemStack, p_406793_, p_407747_);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_406080_) {
        p_406080_.add(NarratedElementType.TITLE, Component.translatable("narration.item", this.itemStack.getHoverName()));
    }
}