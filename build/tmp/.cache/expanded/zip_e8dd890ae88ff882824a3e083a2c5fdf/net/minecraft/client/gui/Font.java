package net.minecraft.client.gui;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringDecomposer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class Font implements net.minecraftforge.client.extensions.IForgeFont {
    private static final float EFFECT_DEPTH = 0.01F;
    private static final float OVER_EFFECT_DEPTH = 0.01F;
    private static final float UNDER_EFFECT_DEPTH = -0.01F;
    public static final float SHADOW_DEPTH = 0.03F;
    public static final int NO_SHADOW = 0;
    public final int lineHeight = 9;
    public final RandomSource random = RandomSource.create();
    private final Function<ResourceLocation, FontSet> fonts;
    final boolean filterFishyGlyphs;
    private final StringSplitter splitter;

    public Font(Function<ResourceLocation, FontSet> pFonts, boolean pFilterFishyGlyphs) {
        this.fonts = pFonts;
        this.filterFishyGlyphs = pFilterFishyGlyphs;
        this.splitter = new StringSplitter(
            (p_92722_, p_92723_) -> this.getFontSet(p_92723_.getFont()).getGlyphInfo(p_92722_, this.filterFishyGlyphs).getAdvance(p_92723_.isBold())
        );
    }

    FontSet getFontSet(ResourceLocation pFontLocation) {
        return this.fonts.apply(pFontLocation);
    }

    public String bidirectionalShaping(String pText) {
        try {
            Bidi bidi = new Bidi(new ArabicShaping(8).shape(pText), 127);
            bidi.setReorderingMode(0);
            return bidi.writeReordered(2);
        } catch (ArabicShapingException arabicshapingexception) {
            return pText;
        }
    }

    public void drawInBatch(
        String pText,
        float pX,
        float pY,
        int pColor,
        boolean pDrawShadow,
        Matrix4f pPose,
        MultiBufferSource pBufferSource,
        Font.DisplayMode pMode,
        int pBackgroundColor,
        int pPackedLightCoords
    ) {
        Font.PreparedText font$preparedtext = this.prepareText(pText, pX, pY, pColor, pDrawShadow, pBackgroundColor);
        font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(pBufferSource, pPose, pMode, pPackedLightCoords));
    }

    public void drawInBatch(
        Component pText,
        float pX,
        float pY,
        int pColor,
        boolean pDrawShadow,
        Matrix4f pPose,
        MultiBufferSource pBufferSource,
        Font.DisplayMode pMode,
        int pBackgroundColor,
        int pPackedLightCoords
    ) {
        Font.PreparedText font$preparedtext = this.prepareText(pText.getVisualOrderText(), pX, pY, pColor, pDrawShadow, pBackgroundColor);
        font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(pBufferSource, pPose, pMode, pPackedLightCoords));
    }

    public void drawInBatch(
        FormattedCharSequence pText,
        float pX,
        float pY,
        int pColor,
        boolean pDrawShadow,
        Matrix4f pPose,
        MultiBufferSource pBufferSource,
        Font.DisplayMode pMode,
        int pBackgroundColor,
        int pPackedLightCoords
    ) {
        Font.PreparedText font$preparedtext = this.prepareText(pText, pX, pY, pColor, pDrawShadow, pBackgroundColor);
        font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(pBufferSource, pPose, pMode, pPackedLightCoords));
    }

    public void drawInBatch8xOutline(
        FormattedCharSequence pText,
        float pX,
        float pY,
        int pColor,
        int pBackgroundColor,
        Matrix4f pPose,
        MultiBufferSource pBufferSource,
        int pPackedLightCoords
    ) {
        Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(0.0F, 0.0F, pBackgroundColor, false);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i != 0 || j != 0) {
                    float[] afloat = new float[]{pX};
                    int k = i;
                    int l = j;
                    pText.accept((p_168661_, p_168662_, p_168663_) -> {
                        boolean flag = p_168662_.isBold();
                        FontSet fontset = this.getFontSet(p_168662_.getFont());
                        GlyphInfo glyphinfo = fontset.getGlyphInfo(p_168663_, this.filterFishyGlyphs);
                        font$preparedtextbuilder.x = afloat[0] + k * glyphinfo.getShadowOffset();
                        font$preparedtextbuilder.y = pY + l * glyphinfo.getShadowOffset();
                        afloat[0] += glyphinfo.getAdvance(flag);
                        return font$preparedtextbuilder.accept(p_168661_, p_168662_.withColor(pBackgroundColor), p_168663_);
                    });
                }
            }
        }

        Font.GlyphVisitor font$glyphvisitor = Font.GlyphVisitor.forMultiBufferSource(pBufferSource, pPose, Font.DisplayMode.NORMAL, pPackedLightCoords);

        for (BakedGlyph.GlyphInstance bakedglyph$glyphinstance : font$preparedtextbuilder.glyphs) {
            font$glyphvisitor.acceptGlyph(bakedglyph$glyphinstance);
        }

        Font.PreparedTextBuilder font$preparedtextbuilder1 = new Font.PreparedTextBuilder(pX, pY, pColor, false);
        pText.accept(font$preparedtextbuilder1);
        font$preparedtextbuilder1.visit(Font.GlyphVisitor.forMultiBufferSource(pBufferSource, pPose, Font.DisplayMode.POLYGON_OFFSET, pPackedLightCoords));
    }

    public Font.PreparedText prepareText(String pText, float pX, float pY, int pColor, boolean pDropShadow, int pBackgroundColor) {
        if (this.isBidirectional()) {
            pText = this.bidirectionalShaping(pText);
        }

        Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(pX, pY, pColor, pBackgroundColor, pDropShadow);
        StringDecomposer.iterateFormatted(pText, Style.EMPTY, font$preparedtextbuilder);
        return font$preparedtextbuilder;
    }

    public Font.PreparedText prepareText(FormattedCharSequence pText, float pX, float pY, int pColor, boolean pDropShadow, int pBackgroundColor) {
        Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(pX, pY, pColor, pBackgroundColor, pDropShadow);
        pText.accept(font$preparedtextbuilder);
        return font$preparedtextbuilder;
    }

    public int width(String pText) {
        return Mth.ceil(this.splitter.stringWidth(pText));
    }

    public int width(FormattedText pText) {
        return Mth.ceil(this.splitter.stringWidth(pText));
    }

    public int width(FormattedCharSequence pText) {
        return Mth.ceil(this.splitter.stringWidth(pText));
    }

    public String plainSubstrByWidth(String pText, int pMaxWidth, boolean pTail) {
        return pTail ? this.splitter.plainTailByWidth(pText, pMaxWidth, Style.EMPTY) : this.splitter.plainHeadByWidth(pText, pMaxWidth, Style.EMPTY);
    }

    public String plainSubstrByWidth(String pText, int pMaxWidth) {
        return this.splitter.plainHeadByWidth(pText, pMaxWidth, Style.EMPTY);
    }

    public FormattedText substrByWidth(FormattedText pText, int pMaxWidth) {
        return this.splitter.headByWidth(pText, pMaxWidth, Style.EMPTY);
    }

    public int wordWrapHeight(String pText, int pMaxWidth) {
        return 9 * this.splitter.splitLines(pText, pMaxWidth, Style.EMPTY).size();
    }

    public int wordWrapHeight(FormattedText pText, int pMaxWidth) {
        return 9 * this.splitter.splitLines(pText, pMaxWidth, Style.EMPTY).size();
    }

    public List<FormattedCharSequence> split(FormattedText pText, int pMaxWidth) {
        return Language.getInstance().getVisualOrder(this.splitter.splitLines(pText, pMaxWidth, Style.EMPTY));
    }

    public List<FormattedText> splitIgnoringLanguage(FormattedText pText, int pMaxWidth) {
        return this.splitter.splitLines(pText, pMaxWidth, Style.EMPTY);
    }

    public boolean isBidirectional() {
        return Language.getInstance().isDefaultRightToLeft();
    }

    public StringSplitter getSplitter() {
        return this.splitter;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum DisplayMode {
        NORMAL,
        SEE_THROUGH,
        POLYGON_OFFSET;
    }

    @OnlyIn(Dist.CLIENT)
    public interface GlyphVisitor {
        static Font.GlyphVisitor forMultiBufferSource(final MultiBufferSource pBufferSource, final Matrix4f pPose, final Font.DisplayMode pDisplayMode, final int pPackedLight) {
            return new Font.GlyphVisitor() {
                @Override
                public void acceptGlyph(BakedGlyph.GlyphInstance p_407665_) {
                    BakedGlyph bakedglyph = p_407665_.glyph();
                    VertexConsumer vertexconsumer = pBufferSource.getBuffer(bakedglyph.renderType(pDisplayMode));
                    bakedglyph.renderChar(p_407665_, pPose, vertexconsumer, pPackedLight, false);
                }

                @Override
                public void acceptEffect(BakedGlyph p_408649_, BakedGlyph.Effect p_408930_) {
                    VertexConsumer vertexconsumer = pBufferSource.getBuffer(p_408649_.renderType(pDisplayMode));
                    p_408649_.renderEffect(p_408930_, pPose, vertexconsumer, pPackedLight, false);
                }
            };
        }

        void acceptGlyph(BakedGlyph.GlyphInstance pGlyph);

        void acceptEffect(BakedGlyph pGlyph, BakedGlyph.Effect pEffect);
    }

    @OnlyIn(Dist.CLIENT)
    public interface PreparedText {
        void visit(Font.GlyphVisitor pVisitor);

        @Nullable
        ScreenRectangle bounds();
    }

    @OnlyIn(Dist.CLIENT)
    class PreparedTextBuilder implements FormattedCharSink, Font.PreparedText {
        private final boolean drawShadow;
        private final int color;
        private final int backgroundColor;
        float x;
        float y;
        private float left = Float.MAX_VALUE;
        private float top = Float.MAX_VALUE;
        private float right = -Float.MAX_VALUE;
        private float bottom = -Float.MAX_VALUE;
        private float backgroundLeft = Float.MAX_VALUE;
        private float backgroundTop = Float.MAX_VALUE;
        private float backgroundRight = -Float.MAX_VALUE;
        private float backgroundBottom = -Float.MAX_VALUE;
        final List<BakedGlyph.GlyphInstance> glyphs = new ArrayList<>();
        @Nullable
        private List<BakedGlyph.Effect> effects;

        public PreparedTextBuilder(final float pX, final float pY, final int pColor, final boolean pDropShadow) {
            this(pX, pY, pColor, 0, pDropShadow);
        }

        public PreparedTextBuilder(final float pX, final float pY, final int pColor, final int pBackgroundColor, final boolean pDropShadow) {
            this.x = pX;
            this.y = pY;
            this.drawShadow = pDropShadow;
            this.color = pColor;
            this.backgroundColor = pBackgroundColor;
            this.markBackground(pX, pY, 0.0F);
        }

        private void markSize(float pLeft, float pTop, float pRight, float pBottom) {
            this.left = Math.min(this.left, pLeft);
            this.top = Math.min(this.top, pTop);
            this.right = Math.max(this.right, pRight);
            this.bottom = Math.max(this.bottom, pBottom);
        }

        private void markBackground(float pX, float pY, float pAdvance) {
            if (ARGB.alpha(this.backgroundColor) != 0) {
                this.backgroundLeft = Math.min(this.backgroundLeft, pX - 1.0F);
                this.backgroundTop = Math.min(this.backgroundTop, pY - 1.0F);
                this.backgroundRight = Math.max(this.backgroundRight, pX + pAdvance);
                this.backgroundBottom = Math.max(this.backgroundBottom, pY + 9.0F);
                this.markSize(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom);
            }
        }

        private void addGlyph(BakedGlyph.GlyphInstance pGlyph) {
            this.glyphs.add(pGlyph);
            this.markSize(pGlyph.left(), pGlyph.top(), pGlyph.right(), pGlyph.bottom());
        }

        private void addEffect(BakedGlyph.Effect pEffect) {
            if (this.effects == null) {
                this.effects = new ArrayList<>();
            }

            this.effects.add(pEffect);
            this.markSize(pEffect.left(), pEffect.top(), pEffect.right(), pEffect.bottom());
        }

        @Override
        public boolean accept(int p_408106_, Style p_408632_, int p_410483_) {
            FontSet fontset = Font.this.getFontSet(p_408632_.getFont());
            GlyphInfo glyphinfo = fontset.getGlyphInfo(p_410483_, Font.this.filterFishyGlyphs);
            BakedGlyph bakedglyph = p_408632_.isObfuscated() && p_410483_ != 32 ? fontset.getRandomGlyph(glyphinfo) : fontset.getGlyph(p_410483_);
            boolean flag = p_408632_.isBold();
            TextColor textcolor = p_408632_.getColor();
            int i = this.getTextColor(textcolor);
            int j = this.getShadowColor(p_408632_, i);
            float f = glyphinfo.getAdvance(flag);
            float f1 = p_408106_ == 0 ? this.x - 1.0F : this.x;
            float f2 = glyphinfo.getShadowOffset();
            if (!(bakedglyph instanceof EmptyGlyph)) {
                float f3 = flag ? glyphinfo.getBoldOffset() : 0.0F;
                this.addGlyph(new BakedGlyph.GlyphInstance(this.x, this.y, i, j, bakedglyph, p_408632_, f3, f2));
            }

            this.markBackground(this.x, this.y, f);
            if (p_408632_.isStrikethrough()) {
                this.addEffect(new BakedGlyph.Effect(f1, this.y + 4.5F - 1.0F, this.x + f, this.y + 4.5F, 0.01F, i, j, f2));
            }

            if (p_408632_.isUnderlined()) {
                this.addEffect(new BakedGlyph.Effect(f1, this.y + 9.0F - 1.0F, this.x + f, this.y + 9.0F, 0.01F, i, j, f2));
            }

            this.x += f;
            return true;
        }

        @Override
        public void visit(Font.GlyphVisitor p_407346_) {
            BakedGlyph bakedglyph = null;
            if (ARGB.alpha(this.backgroundColor) != 0) {
                BakedGlyph.Effect bakedglyph$effect = new BakedGlyph.Effect(
                    this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom, -0.01F, this.backgroundColor
                );
                bakedglyph = Font.this.getFontSet(Style.DEFAULT_FONT).whiteGlyph();
                p_407346_.acceptEffect(bakedglyph, bakedglyph$effect);
            }

            for (BakedGlyph.GlyphInstance bakedglyph$glyphinstance : this.glyphs) {
                p_407346_.acceptGlyph(bakedglyph$glyphinstance);
            }

            if (this.effects != null) {
                if (bakedglyph == null) {
                    bakedglyph = Font.this.getFontSet(Style.DEFAULT_FONT).whiteGlyph();
                }

                for (BakedGlyph.Effect bakedglyph$effect1 : this.effects) {
                    p_407346_.acceptEffect(bakedglyph, bakedglyph$effect1);
                }
            }
        }

        private int getTextColor(@Nullable TextColor pTextColor) {
            if (pTextColor != null) {
                int i = ARGB.alpha(this.color);
                int j = pTextColor.getValue();
                return ARGB.color(i, j);
            } else {
                return this.color;
            }
        }

        private int getShadowColor(Style pStyle, int pTextColor) {
            Integer integer = pStyle.getShadowColor();
            if (integer != null) {
                float f = ARGB.alphaFloat(pTextColor);
                float f1 = ARGB.alphaFloat(integer);
                return f != 1.0F ? ARGB.color(ARGB.as8BitChannel(f * f1), integer) : integer;
            } else {
                return this.drawShadow ? ARGB.scaleRGB(pTextColor, 0.25F) : 0;
            }
        }

        @Nullable
        @Override
        public ScreenRectangle bounds() {
            if (!(this.left >= this.right) && !(this.top >= this.bottom)) {
                int i = Mth.floor(this.left);
                int j = Mth.floor(this.top);
                int k = Mth.ceil(this.right);
                int l = Mth.ceil(this.bottom);
                return new ScreenRectangle(i, j, k - i, l - j);
            } else {
                return null;
            }
        }
    }
}
