package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EffectsInInventory {
    private static final ResourceLocation EFFECT_BACKGROUND_LARGE_SPRITE = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_large");
    private static final ResourceLocation EFFECT_BACKGROUND_SMALL_SPRITE = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_small");
    private final AbstractContainerScreen<?> screen;
    private final Minecraft minecraft;
    @Nullable
    private MobEffectInstance hoveredEffect;

    public EffectsInInventory(AbstractContainerScreen<?> pScreen) {
        this.screen = pScreen;
        this.minecraft = Minecraft.getInstance();
    }

    public boolean canSeeEffects() {
        int i = this.screen.leftPos + this.screen.imageWidth + 2;
        int j = this.screen.width - i;
        return j >= 32;
    }

    public void renderEffects(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        this.hoveredEffect = null;
        int i = this.screen.leftPos + this.screen.imageWidth + 2;
        int j = this.screen.width - i;
        Collection<MobEffectInstance> collection = this.minecraft.player.getActiveEffects();
        if (!collection.isEmpty() && j >= 32) {
            boolean flag = j >= 120;
            var event = net.minecraftforge.client.event.ForgeEventFactoryClient.onScreenEffectSize(this.screen, j, !flag, i);
            if (event == null) return;
            flag = !event.isCompact();
            i = event.getHorizontalOffset();
            int k = 33;
            if (collection.size() > 5) {
                k = 132 / (collection.size() - 1);
            }

            Iterable<MobEffectInstance> iterable = collection.stream().filter(net.minecraftforge.client.ForgeHooksClient::shouldRenderEffect).sorted().toList();
            this.renderBackgrounds(pGuiGraphics, i, k, iterable, flag);
            this.renderIcons(pGuiGraphics, i, k, iterable, flag);
            if (flag) {
                this.renderLabels(pGuiGraphics, i, k, iterable);
            } else if (pMouseX >= i && pMouseX <= i + 33) {
                int l = this.screen.topPos;

                for (MobEffectInstance mobeffectinstance : iterable) {
                    if (pMouseY >= l && pMouseY <= l + k) {
                        this.hoveredEffect = mobeffectinstance;
                    }

                    l += k;
                }
            }
        }
    }

    public void renderTooltip(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        if (this.hoveredEffect != null) {
            List<Component> list = List.of(
                this.getEffectName(this.hoveredEffect), MobEffectUtil.formatDuration(this.hoveredEffect, 1.0F, this.minecraft.level.tickRateManager().tickrate())
            );
            pGuiGraphics.setTooltipForNextFrame(this.screen.getFont(), list, Optional.empty(), pMouseX, pMouseY);
        }
    }

    private void renderBackgrounds(GuiGraphics pGuiGraphics, int pX, int pY, Iterable<MobEffectInstance> pActiveEffects, boolean pLarge) {
        int i = this.screen.topPos;

        for (MobEffectInstance mobeffectinstance : pActiveEffects) {
            if (pLarge) {
                pGuiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND_LARGE_SPRITE, pX, i, 120, 32);
            } else {
                pGuiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND_SMALL_SPRITE, pX, i, 32, 32);
            }

            i += pY;
        }
    }

    private void renderIcons(GuiGraphics pGuiGraphics, int pX, int pY, Iterable<MobEffectInstance> pActiveEffects, boolean pLarge) {
        int i = this.screen.topPos;

        for (MobEffectInstance mobeffectinstance : pActiveEffects) {
            var renderer = net.minecraftforge.client.extensions.common.IClientMobEffectExtensions.of(mobeffectinstance);
            if (renderer.renderInventoryIcon(mobeffectinstance, this, pGuiGraphics, pX + (pLarge ? 6 : 7), i, 0)) {
                i += pY;
                continue;
            }
            Holder<MobEffect> holder = mobeffectinstance.getEffect();
            ResourceLocation resourcelocation = Gui.getMobEffectSprite(holder);
            pGuiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, pX + (pLarge ? 6 : 7), i + 7, 18, 18);
            i += pY;
        }
    }

    private void renderLabels(GuiGraphics pGuiGraphics, int pX, int pY, Iterable<MobEffectInstance> pActiveEffects) {
        int i = this.screen.topPos;

        for (MobEffectInstance mobeffectinstance : pActiveEffects) {
            var renderer = net.minecraftforge.client.extensions.common.IClientMobEffectExtensions.of(mobeffectinstance);
            if (renderer.renderInventoryText(mobeffectinstance, this, pGuiGraphics, pX, i, 0)) {
                i += pY;
                continue;
            }
            Component component = this.getEffectName(mobeffectinstance);
            pGuiGraphics.drawString(this.screen.getFont(), component, pX + 10 + 18, i + 6, -1);
            Component component1 = MobEffectUtil.formatDuration(mobeffectinstance, 1.0F, this.minecraft.level.tickRateManager().tickrate());
            pGuiGraphics.drawString(this.screen.getFont(), component1, pX + 10 + 18, i + 6 + 10, -8421505);
            i += pY;
        }
    }

    private Component getEffectName(MobEffectInstance pEffect) {
        MutableComponent mutablecomponent = pEffect.getEffect().value().getDisplayName().copy();
        if (pEffect.getAmplifier() >= 1 && pEffect.getAmplifier() <= 9) {
            mutablecomponent.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (pEffect.getAmplifier() + 1)));
        }

        return mutablecomponent;
    }

    public AbstractContainerScreen<?> getScreen() {
        return this.screen;
    }
}
