package net.minecraft.client.gui.components;

import java.util.OptionalInt;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.SingleKeyCache;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiLineTextWidget extends AbstractStringWidget {
    private OptionalInt maxWidth = OptionalInt.empty();
    private OptionalInt maxRows = OptionalInt.empty();
    private final SingleKeyCache<MultiLineTextWidget.CacheKey, MultiLineLabel> cache;
    private boolean centered = false;
    private boolean allowHoverComponents = false;
    @Nullable
    private Consumer<Style> componentClickHandler = null;

    public MultiLineTextWidget(Component pMessage, Font pFont) {
        this(0, 0, pMessage, pFont);
    }

    public MultiLineTextWidget(int pX, int pY, Component pMessage, Font pFont) {
        super(pX, pY, 0, 0, pMessage, pFont);
        this.cache = Util.singleKeyCache(
            p_340776_ -> p_340776_.maxRows.isPresent()
                ? MultiLineLabel.create(pFont, p_340776_.maxWidth, p_340776_.maxRows.getAsInt(), p_340776_.message)
                : MultiLineLabel.create(pFont, p_340776_.message, p_340776_.maxWidth)
        );
        this.active = false;
    }

    public MultiLineTextWidget setColor(int p_270378_) {
        super.setColor(p_270378_);
        return this;
    }

    public MultiLineTextWidget setMaxWidth(int pMaxWidth) {
        this.maxWidth = OptionalInt.of(pMaxWidth);
        return this;
    }

    public MultiLineTextWidget setMaxRows(int pMaxRows) {
        this.maxRows = OptionalInt.of(pMaxRows);
        return this;
    }

    public MultiLineTextWidget setCentered(boolean pCentered) {
        this.centered = pCentered;
        return this;
    }

    public MultiLineTextWidget configureStyleHandling(boolean pAllowHoverComponents, @Nullable Consumer<Style> pComponentClickHandler) {
        this.allowHoverComponents = pAllowHoverComponents;
        this.componentClickHandler = pComponentClickHandler;
        return this;
    }

    @Override
    public int getWidth() {
        return this.cache.getValue(this.getFreshCacheKey()).getWidth();
    }

    @Override
    public int getHeight() {
        return this.cache.getValue(this.getFreshCacheKey()).getLineCount() * 9;
    }

    @Override
    public void renderWidget(GuiGraphics p_282535_, int p_261774_, int p_261640_, float p_261514_) {
        MultiLineLabel multilinelabel = this.cache.getValue(this.getFreshCacheKey());
        int i = this.getX();
        int j = this.getY();
        int k = 9;
        int l = this.getColor();
        if (this.centered) {
            multilinelabel.renderCentered(p_282535_, i + this.getWidth() / 2, j, k, l);
        } else {
            multilinelabel.renderLeftAligned(p_282535_, i, j, k, l);
        }

        if (this.allowHoverComponents) {
            Style style = this.getComponentStyleAt(p_261774_, p_261640_);
            if (this.isHovered()) {
                p_282535_.renderComponentHoverEffect(this.getFont(), style, p_261774_, p_261640_);
            }
        }
    }

    @Nullable
    private Style getComponentStyleAt(double pMouseX, double pMouseY) {
        MultiLineLabel multilinelabel = this.cache.getValue(this.getFreshCacheKey());
        int i = this.getX();
        int j = this.getY();
        int k = 9;
        return this.centered
            ? multilinelabel.getStyleAtCentered(i + this.getWidth() / 2, j, k, pMouseX, pMouseY)
            : multilinelabel.getStyleAtLeftAligned(i, j, k, pMouseX, pMouseY);
    }

    @Override
    public void onClick(double p_408715_, double p_407516_) {
        if (this.componentClickHandler != null) {
            Style style = this.getComponentStyleAt(p_408715_, p_407516_);
            if (style != null) {
                this.componentClickHandler.accept(style);
                return;
            }
        }

        super.onClick(p_408715_, p_407516_);
    }

    private MultiLineTextWidget.CacheKey getFreshCacheKey() {
        return new MultiLineTextWidget.CacheKey(this.getMessage(), this.maxWidth.orElse(Integer.MAX_VALUE), this.maxRows);
    }

    @OnlyIn(Dist.CLIENT)
    record CacheKey(Component message, int maxWidth, OptionalInt maxRows) {
    }
}