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

    public Font(Function<ResourceLocation, FontSet> p_243253_, boolean p_243245_) {
        this.fonts = p_243253_;
        this.filterFishyGlyphs = p_243245_;
        this.splitter = new StringSplitter(
            (p_92722_, p_92723_) -> this.getFontSet(p_92723_.getFont()).getGlyphInfo(p_92722_, this.filterFishyGlyphs).getAdvance(p_92723_.isBold())
        );
    }

    FontSet getFontSet(ResourceLocation p_92864_) {
        return this.fonts.apply(p_92864_);
    }

    public String bidirectionalShaping(String p_92802_) {
        try {
            Bidi bidi = new Bidi(new ArabicShaping(8).shape(p_92802_), 127);
            bidi.setReorderingMode(0);
            return bidi.writeReordered(2);
        } catch (ArabicShapingException arabicshapingexception) {
            return p_92802_;
        }
    }

    public void drawInBatch(
        String p_272751_,
        float p_272661_,
        float p_273129_,
        int p_273272_,
        boolean p_273209_,
        Matrix4f p_272940_,
        MultiBufferSource p_273017_,
        Font.DisplayMode p_272608_,
        int p_273365_,
        int p_272755_
    ) {
        Font.PreparedText font$preparedtext = this.prepareText(p_272751_, p_272661_, p_273129_, p_273272_, p_273209_, p_273365_);
        font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(p_273017_, p_272940_, p_272608_, p_272755_));
    }

    public void drawInBatch(
        Component p_409939_,
        float p_273006_,
        float p_273254_,
        int p_273375_,
        boolean p_273674_,
        Matrix4f p_273525_,
        MultiBufferSource p_272624_,
        Font.DisplayMode p_273418_,
        int p_273330_,
        int p_272981_
    ) {
        Font.PreparedText font$preparedtext = this.prepareText(p_409939_.getVisualOrderText(), p_273006_, p_273254_, p_273375_, p_273674_, p_273330_);
        font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(p_272624_, p_273525_, p_273418_, p_272981_));
    }

    public void drawInBatch(
        FormattedCharSequence p_407439_,
        float p_272811_,
        float p_272610_,
        int p_273422_,
        boolean p_273016_,
        Matrix4f p_273443_,
        MultiBufferSource p_273387_,
        Font.DisplayMode p_273551_,
        int p_272706_,
        int p_273114_
    ) {
        Font.PreparedText font$preparedtext = this.prepareText(p_407439_, p_272811_, p_272610_, p_273422_, p_273016_, p_272706_);
        font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(p_273387_, p_273443_, p_273551_, p_273114_));
    }

    public void drawInBatch8xOutline(
        FormattedCharSequence p_168646_,
        float p_168647_,
        float p_168648_,
        int p_168649_,
        int p_168650_,
        Matrix4f p_254170_,
        MultiBufferSource p_168652_,
        int p_168653_
    ) {
        Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(0.0F, 0.0F, p_168650_, false);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i != 0 || j != 0) {
                    float[] afloat = new float[]{p_168647_};
                    int k = i;
                    int l = j;
                    p_168646_.accept((p_168661_, p_168662_, p_168663_) -> {
                        boolean flag = p_168662_.isBold();
                        FontSet fontset = this.getFontSet(p_168662_.getFont());
                        GlyphInfo glyphinfo = fontset.getGlyphInfo(p_168663_, this.filterFishyGlyphs);
                        font$preparedtextbuilder.x = afloat[0] + k * glyphinfo.getShadowOffset();
                        font$preparedtextbuilder.y = p_168648_ + l * glyphinfo.getShadowOffset();
                        afloat[0] += glyphinfo.getAdvance(flag);
                        return font$preparedtextbuilder.accept(p_168661_, p_168662_.withColor(p_168650_), p_168663_);
                    });
                }
            }
        }

        Font.GlyphVisitor font$glyphvisitor = Font.GlyphVisitor.forMultiBufferSource(p_168652_, p_254170_, Font.DisplayMode.NORMAL, p_168653_);

        for (BakedGlyph.GlyphInstance bakedglyph$glyphinstance : font$preparedtextbuilder.glyphs) {
            font$glyphvisitor.acceptGlyph(bakedglyph$glyphinstance);
        }

        Font.PreparedTextBuilder font$preparedtextbuilder1 = new Font.PreparedTextBuilder(p_168647_, p_168648_, p_168649_, false);
        p_168646_.accept(font$preparedtextbuilder1);
        font$preparedtextbuilder1.visit(Font.GlyphVisitor.forMultiBufferSource(p_168652_, p_254170_, Font.DisplayMode.POLYGON_OFFSET, p_168653_));
    }

    public Font.PreparedText prepareText(String p_409763_, float p_405856_, float p_406377_, int p_406829_, boolean p_408402_, int p_406561_) {
        if (this.isBidirectional()) {
            p_409763_ = this.bidirectionalShaping(p_409763_);
        }

        Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(p_405856_, p_406377_, p_406829_, p_406561_, p_408402_);
        StringDecomposer.iterateFormatted(p_409763_, Style.EMPTY, font$preparedtextbuilder);
        return font$preparedtextbuilder;
    }

    public Font.PreparedText prepareText(FormattedCharSequence p_406646_, float p_410379_, float p_409318_, int p_410317_, boolean p_406084_, int p_406668_) {
        Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(p_410379_, p_409318_, p_410317_, p_406668_, p_406084_);
        p_406646_.accept(font$preparedtextbuilder);
        return font$preparedtextbuilder;
    }

    public int width(String p_92896_) {
        return Mth.ceil(this.splitter.stringWidth(p_92896_));
    }

    public int width(FormattedText p_92853_) {
        return Mth.ceil(this.splitter.stringWidth(p_92853_));
    }

    public int width(FormattedCharSequence p_92725_) {
        return Mth.ceil(this.splitter.stringWidth(p_92725_));
    }

    public String plainSubstrByWidth(String p_92838_, int p_92839_, boolean p_92840_) {
        return p_92840_ ? this.splitter.plainTailByWidth(p_92838_, p_92839_, Style.EMPTY) : this.splitter.plainHeadByWidth(p_92838_, p_92839_, Style.EMPTY);
    }

    public String plainSubstrByWidth(String p_92835_, int p_92836_) {
        return this.splitter.plainHeadByWidth(p_92835_, p_92836_, Style.EMPTY);
    }

    public FormattedText substrByWidth(FormattedText p_92855_, int p_92856_) {
        return this.splitter.headByWidth(p_92855_, p_92856_, Style.EMPTY);
    }

    public int wordWrapHeight(String p_92921_, int p_92922_) {
        return 9 * this.splitter.splitLines(p_92921_, p_92922_, Style.EMPTY).size();
    }

    public int wordWrapHeight(FormattedText p_239134_, int p_239135_) {
        return 9 * this.splitter.splitLines(p_239134_, p_239135_, Style.EMPTY).size();
    }

    public List<FormattedCharSequence> split(FormattedText p_92924_, int p_92925_) {
        return Language.getInstance().getVisualOrder(this.splitter.splitLines(p_92924_, p_92925_, Style.EMPTY));
    }

    public List<FormattedText> splitIgnoringLanguage(FormattedText p_407108_, int p_408991_) {
        return this.splitter.splitLines(p_407108_, p_408991_, Style.EMPTY);
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
        static Font.GlyphVisitor forMultiBufferSource(final MultiBufferSource p_409617_, final Matrix4f p_408497_, final Font.DisplayMode p_409313_, final int p_408611_) {
            return new Font.GlyphVisitor() {
                @Override
                public void acceptGlyph(BakedGlyph.GlyphInstance p_407665_) {
                    BakedGlyph bakedglyph = p_407665_.glyph();
                    VertexConsumer vertexconsumer = p_409617_.getBuffer(bakedglyph.renderType(p_409313_));
                    bakedglyph.renderChar(p_407665_, p_408497_, vertexconsumer, p_408611_, false);
                }

                @Override
                public void acceptEffect(BakedGlyph p_408649_, BakedGlyph.Effect p_408930_) {
                    VertexConsumer vertexconsumer = p_409617_.getBuffer(p_408649_.renderType(p_409313_));
                    p_408649_.renderEffect(p_408930_, p_408497_, vertexconsumer, p_408611_, false);
                }
            };
        }

        void acceptGlyph(BakedGlyph.GlyphInstance p_409018_);

        void acceptEffect(BakedGlyph p_410481_, BakedGlyph.Effect p_406217_);
    }

    @OnlyIn(Dist.CLIENT)
    public interface PreparedText {
        void visit(Font.GlyphVisitor p_406444_);

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

        public PreparedTextBuilder(final float p_407746_, final float p_410668_, final int p_408037_, final boolean p_406887_) {
            this(p_407746_, p_410668_, p_408037_, 0, p_406887_);
        }

        public PreparedTextBuilder(final float p_408474_, final float p_405862_, final int p_406916_, final int p_407483_, final boolean p_410641_) {
            this.x = p_408474_;
            this.y = p_405862_;
            this.drawShadow = p_410641_;
            this.color = p_406916_;
            this.backgroundColor = p_407483_;
            this.markBackground(p_408474_, p_405862_, 0.0F);
        }

        private void markSize(float p_408328_, float p_410584_, float p_407096_, float p_407028_) {
            this.left = Math.min(this.left, p_408328_);
            this.top = Math.min(this.top, p_410584_);
            this.right = Math.max(this.right, p_407096_);
            this.bottom = Math.max(this.bottom, p_407028_);
        }

        private void markBackground(float p_407445_, float p_408838_, float p_406374_) {
            if (ARGB.alpha(this.backgroundColor) != 0) {
                this.backgroundLeft = Math.min(this.backgroundLeft, p_407445_ - 1.0F);
                this.backgroundTop = Math.min(this.backgroundTop, p_408838_ - 1.0F);
                this.backgroundRight = Math.max(this.backgroundRight, p_407445_ + p_406374_);
                this.backgroundBottom = Math.max(this.backgroundBottom, p_408838_ + 9.0F);
                this.markSize(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom);
            }
        }

        private void addGlyph(BakedGlyph.GlyphInstance p_406370_) {
            this.glyphs.add(p_406370_);
            this.markSize(p_406370_.left(), p_406370_.top(), p_406370_.right(), p_406370_.bottom());
        }

        private void addEffect(BakedGlyph.Effect p_409773_) {
            if (this.effects == null) {
                this.effects = new ArrayList<>();
            }

            this.effects.add(p_409773_);
            this.markSize(p_409773_.left(), p_409773_.top(), p_409773_.right(), p_409773_.bottom());
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

        private int getTextColor(@Nullable TextColor p_407859_) {
            if (p_407859_ != null) {
                int i = ARGB.alpha(this.color);
                int j = p_407859_.getValue();
                return ARGB.color(i, j);
            } else {
                return this.color;
            }
        }

        private int getShadowColor(Style p_408920_, int p_408082_) {
            Integer integer = p_408920_.getShadowColor();
            if (integer != null) {
                float f = ARGB.alphaFloat(p_408082_);
                float f1 = ARGB.alphaFloat(integer);
                return f != 1.0F ? ARGB.color(ARGB.as8BitChannel(f * f1), integer) : integer;
            } else {
                return this.drawShadow ? ARGB.scaleRGB(p_408082_, 0.25F) : 0;
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
