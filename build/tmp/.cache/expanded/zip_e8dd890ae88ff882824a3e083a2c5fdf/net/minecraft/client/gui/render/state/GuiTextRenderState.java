package net.minecraft.client.gui.render.state;

import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;

@OnlyIn(Dist.CLIENT)
public final class GuiTextRenderState implements ScreenArea {
    public final Font font;
    public final FormattedCharSequence text;
    public final Matrix3x2f pose;
    public final int x;
    public final int y;
    public final int color;
    public final int backgroundColor;
    public final boolean dropShadow;
    @Nullable
    public final ScreenRectangle scissor;
    @Nullable
    private Font.PreparedText preparedText;
    @Nullable
    private ScreenRectangle bounds;

    public GuiTextRenderState(
        Font pFont,
        FormattedCharSequence pText,
        Matrix3x2f pPose,
        int pX,
        int pY,
        int pColor,
        int pBackgroundColor,
        boolean pDropShadow,
        @Nullable ScreenRectangle pScissor
    ) {
        this.font = pFont;
        this.text = pText;
        this.pose = pPose;
        this.x = pX;
        this.y = pY;
        this.color = pColor;
        this.backgroundColor = pBackgroundColor;
        this.dropShadow = pDropShadow;
        this.scissor = pScissor;
    }

    public Font.PreparedText ensurePrepared() {
        if (this.preparedText == null) {
            this.preparedText = this.font.prepareText(this.text, this.x, this.y, this.color, this.dropShadow, this.backgroundColor);
            ScreenRectangle screenrectangle = this.preparedText.bounds();
            if (screenrectangle != null) {
                screenrectangle = screenrectangle.transformMaxBounds(this.pose);
                this.bounds = this.scissor != null ? this.scissor.intersection(screenrectangle) : screenrectangle;
            }
        }

        return this.preparedText;
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        this.ensurePrepared();
        return this.bounds;
    }
}