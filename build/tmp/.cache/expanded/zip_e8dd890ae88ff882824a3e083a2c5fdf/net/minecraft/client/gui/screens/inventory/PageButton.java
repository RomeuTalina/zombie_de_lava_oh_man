package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PageButton extends Button {
    private static final ResourceLocation PAGE_FORWARD_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_forward_highlighted");
    private static final ResourceLocation PAGE_FORWARD_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_forward");
    private static final ResourceLocation PAGE_BACKWARD_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_backward_highlighted");
    private static final ResourceLocation PAGE_BACKWARD_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_backward");
    private static final Component PAGE_BUTTON_NEXT = Component.translatable("book.page_button.next");
    private static final Component PAGE_BUTTON_PREVIOUS = Component.translatable("book.page_button.previous");
    private final boolean isForward;
    private final boolean playTurnSound;

    public PageButton(int pX, int pY, boolean pIsForward, Button.OnPress pOnPress, boolean pPlayTurnSound) {
        super(pX, pY, 23, 13, pIsForward ? PAGE_BUTTON_NEXT : PAGE_BUTTON_PREVIOUS, pOnPress, DEFAULT_NARRATION);
        this.isForward = pIsForward;
        this.playTurnSound = pPlayTurnSound;
    }

    @Override
    public void renderWidget(GuiGraphics p_283468_, int p_282922_, int p_283637_, float p_282459_) {
        ResourceLocation resourcelocation;
        if (this.isForward) {
            resourcelocation = this.isHoveredOrFocused() ? PAGE_FORWARD_HIGHLIGHTED_SPRITE : PAGE_FORWARD_SPRITE;
        } else {
            resourcelocation = this.isHoveredOrFocused() ? PAGE_BACKWARD_HIGHLIGHTED_SPRITE : PAGE_BACKWARD_SPRITE;
        }

        p_283468_.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, this.getX(), this.getY(), 23, 13);
    }

    @Override
    public void playDownSound(SoundManager pHandler) {
        if (this.playTurnSound) {
            pHandler.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }
    }
}