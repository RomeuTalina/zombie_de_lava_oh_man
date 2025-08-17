package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class BakedGlyph {
    public static final float Z_FIGHTER = 0.001F;
    private final GlyphRenderTypes renderTypes;
    @Nullable
    private final GpuTextureView textureView;
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;
    private final float left;
    private final float right;
    private final float up;
    private final float down;

    public BakedGlyph(
        GlyphRenderTypes pRenderTypes,
        @Nullable GpuTextureView pTextureView,
        float pU0,
        float pU1,
        float pV0,
        float pV1,
        float pLeft,
        float pRight,
        float pUp,
        float pDown
    ) {
        this.renderTypes = pRenderTypes;
        this.textureView = pTextureView;
        this.u0 = pU0;
        this.u1 = pU1;
        this.v0 = pV0;
        this.v1 = pV1;
        this.left = pLeft;
        this.right = pRight;
        this.up = pUp;
        this.down = pDown;
    }

    public float left(BakedGlyph.GlyphInstance pGlyph) {
        return pGlyph.x
            + this.left
            + (pGlyph.style.isItalic() ? Math.min(this.shearTop(), this.shearBottom()) : 0.0F)
            - extraThickness(pGlyph.style.isBold());
    }

    public float top(BakedGlyph.GlyphInstance pGlyph) {
        return pGlyph.y + this.up - extraThickness(pGlyph.style.isBold());
    }

    public float right(BakedGlyph.GlyphInstance pGlyph) {
        return pGlyph.x
            + this.right
            + (pGlyph.hasShadow() ? pGlyph.shadowOffset : 0.0F)
            + (pGlyph.style.isItalic() ? Math.max(this.shearTop(), this.shearBottom()) : 0.0F)
            + extraThickness(pGlyph.style.isBold());
    }

    public float bottom(BakedGlyph.GlyphInstance pGlyph) {
        return pGlyph.y + this.down + (pGlyph.hasShadow() ? pGlyph.shadowOffset : 0.0F) + extraThickness(pGlyph.style.isBold());
    }

    public void renderChar(BakedGlyph.GlyphInstance pGlyph, Matrix4f pPose, VertexConsumer pBuffer, int pPackedLight, boolean pNoDepth) {
        Style style = pGlyph.style();
        boolean flag = style.isItalic();
        float f = pGlyph.x();
        float f1 = pGlyph.y();
        int i = pGlyph.color();
        boolean flag1 = style.isBold();
        float f3 = pNoDepth ? 0.0F : 0.001F;
        float f2;
        if (pGlyph.hasShadow()) {
            int j = pGlyph.shadowColor();
            this.render(flag, f + pGlyph.shadowOffset(), f1 + pGlyph.shadowOffset(), 0.0F, pPose, pBuffer, j, flag1, pPackedLight);
            if (flag1) {
                this.render(
                    flag, f + pGlyph.boldOffset() + pGlyph.shadowOffset(), f1 + pGlyph.shadowOffset(), f3, pPose, pBuffer, j, true, pPackedLight
                );
            }

            f2 = pNoDepth ? 0.0F : 0.03F;
        } else {
            f2 = 0.0F;
        }

        this.render(flag, f, f1, f2, pPose, pBuffer, i, flag1, pPackedLight);
        if (flag1) {
            this.render(flag, f + pGlyph.boldOffset(), f1, f2 + f3, pPose, pBuffer, i, true, pPackedLight);
        }
    }

    private void render(
        boolean pItalic,
        float pX,
        float pY,
        float pZ,
        Matrix4f pPose,
        VertexConsumer pBuffer,
        int pColor,
        boolean pBold,
        int pPackedLight
    ) {
        float f = pX + this.left;
        float f1 = pX + this.right;
        float f2 = pY + this.up;
        float f3 = pY + this.down;
        float f4 = pItalic ? this.shearTop() : 0.0F;
        float f5 = pItalic ? this.shearBottom() : 0.0F;
        float f6 = extraThickness(pBold);
        pBuffer.addVertex(pPose, f + f4 - f6, f2 - f6, pZ).setColor(pColor).setUv(this.u0, this.v0).setLight(pPackedLight);
        pBuffer.addVertex(pPose, f + f5 - f6, f3 + f6, pZ).setColor(pColor).setUv(this.u0, this.v1).setLight(pPackedLight);
        pBuffer.addVertex(pPose, f1 + f5 + f6, f3 + f6, pZ).setColor(pColor).setUv(this.u1, this.v1).setLight(pPackedLight);
        pBuffer.addVertex(pPose, f1 + f4 + f6, f2 - f6, pZ).setColor(pColor).setUv(this.u1, this.v0).setLight(pPackedLight);
    }

    private static float extraThickness(boolean pBold) {
        return pBold ? 0.1F : 0.0F;
    }

    private float shearBottom() {
        return 1.0F - 0.25F * this.down;
    }

    private float shearTop() {
        return 1.0F - 0.25F * this.up;
    }

    public void renderEffect(BakedGlyph.Effect pEffect, Matrix4f pPose, VertexConsumer pBuffer, int pPackedLight, boolean pNoDepth) {
        float f = pNoDepth ? 0.0F : pEffect.depth;
        if (pEffect.hasShadow()) {
            this.buildEffect(pEffect, pEffect.shadowOffset(), f, pEffect.shadowColor(), pBuffer, pPackedLight, pPose);
            f += pNoDepth ? 0.0F : 0.03F;
        }

        this.buildEffect(pEffect, 0.0F, f, pEffect.color, pBuffer, pPackedLight, pPose);
    }

    private void buildEffect(
        BakedGlyph.Effect pEffect, float pShadowOffset, float pDepthOffset, int pShadowColor, VertexConsumer pBuffer, int pPackedLight, Matrix4f pPose
    ) {
        pBuffer.addVertex(pPose, pEffect.x0 + pShadowOffset, pEffect.y1 + pShadowOffset, pDepthOffset)
            .setColor(pShadowColor)
            .setUv(this.u0, this.v0)
            .setLight(pPackedLight);
        pBuffer.addVertex(pPose, pEffect.x1 + pShadowOffset, pEffect.y1 + pShadowOffset, pDepthOffset)
            .setColor(pShadowColor)
            .setUv(this.u0, this.v1)
            .setLight(pPackedLight);
        pBuffer.addVertex(pPose, pEffect.x1 + pShadowOffset, pEffect.y0 + pShadowOffset, pDepthOffset)
            .setColor(pShadowColor)
            .setUv(this.u1, this.v1)
            .setLight(pPackedLight);
        pBuffer.addVertex(pPose, pEffect.x0 + pShadowOffset, pEffect.y0 + pShadowOffset, pDepthOffset)
            .setColor(pShadowColor)
            .setUv(this.u1, this.v0)
            .setLight(pPackedLight);
    }

    @Nullable
    public GpuTextureView textureView() {
        return this.textureView;
    }

    public RenderPipeline guiPipeline() {
        return this.renderTypes.guiPipeline();
    }

    public RenderType renderType(Font.DisplayMode pDisplayMode) {
        return this.renderTypes.select(pDisplayMode);
    }

    @OnlyIn(Dist.CLIENT)
    public record Effect(float x0, float y0, float x1, float y1, float depth, int color, int shadowColor, float shadowOffset) {
        public Effect(float pX0, float pY0, float pX1, float pY1, float pDepth, int pColor) {
            this(pX0, pY0, pX1, pY1, pDepth, pColor, 0, 0.0F);
        }

        public float left() {
            return this.x0;
        }

        public float top() {
            return this.y0;
        }

        public float right() {
            return this.x1 + (this.hasShadow() ? this.shadowOffset : 0.0F);
        }

        public float bottom() {
            return this.y1 + (this.hasShadow() ? this.shadowOffset : 0.0F);
        }

        boolean hasShadow() {
            return this.shadowColor() != 0;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record GlyphInstance(
        float x, float y, int color, int shadowColor, BakedGlyph glyph, Style style, float boldOffset, float shadowOffset
    ) {
        public float left() {
            return this.glyph.left(this);
        }

        public float top() {
            return this.glyph.top(this);
        }

        public float right() {
            return this.glyph.right(this);
        }

        public float bottom() {
            return this.glyph.bottom(this);
        }

        boolean hasShadow() {
            return this.shadowColor() != 0;
        }
    }
}