package net.minecraft.client.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.ColoredRectangleRenderState;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.client.gui.render.state.pip.GuiBannerResultRenderState;
import net.minecraft.client.gui.render.state.pip.GuiBookModelRenderState;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.gui.render.state.pip.GuiProfilerChartRenderState;
import net.minecraft.client.gui.render.state.pip.GuiSignRenderState;
import net.minecraft.client.gui.render.state.pip.GuiSkinRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ResultField;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector2ic;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class GuiGraphics implements net.minecraftforge.client.extensions.IForgeGuiGraphics {
    private static final int EXTRA_SPACE_AFTER_FIRST_TOOLTIP_LINE = 2;
    private final Minecraft minecraft;
    private final Matrix3x2fStack pose;
    private final GuiGraphics.ScissorStack scissorStack = new GuiGraphics.ScissorStack();
    private final GuiSpriteManager sprites;
    private final GuiRenderState guiRenderState;
    @Nullable
    private Runnable deferredTooltip;

    private GuiGraphics(Minecraft pMinecraft, Matrix3x2fStack pPose, GuiRenderState pGuiRenderState) {
        this.minecraft = pMinecraft;
        this.pose = pPose;
        this.sprites = pMinecraft.getGuiSprites();
        this.guiRenderState = pGuiRenderState;
    }

    public GuiGraphics(Minecraft pMinecraft, GuiRenderState pGuiRenderState) {
        this(pMinecraft, new Matrix3x2fStack(16), pGuiRenderState);
    }

    public int guiWidth() {
        return this.minecraft.getWindow().getGuiScaledWidth();
    }

    public int guiHeight() {
        return this.minecraft.getWindow().getGuiScaledHeight();
    }

    public void nextStratum() {
        this.guiRenderState.nextStratum();
    }

    public void blurBeforeThisStratum() {
        this.guiRenderState.blurBeforeThisStratum();
    }

    public Matrix3x2fStack pose() {
        return this.pose;
    }

    public void hLine(int pMinX, int pMaxX, int pY, int pColor) {
        if (pMaxX < pMinX) {
            int i = pMinX;
            pMinX = pMaxX;
            pMaxX = i;
        }

        this.fill(pMinX, pY, pMaxX + 1, pY + 1, pColor);
    }

    public void vLine(int pX, int pMinY, int pMaxY, int pColor) {
        if (pMaxY < pMinY) {
            int i = pMinY;
            pMinY = pMaxY;
            pMaxY = i;
        }

        this.fill(pX, pMinY + 1, pX + 1, pMaxY, pColor);
    }

    public void enableScissor(int pMinX, int pMinY, int pMaxX, int pMaxY) {
        ScreenRectangle screenrectangle = new ScreenRectangle(pMinX, pMinY, pMaxX - pMinX, pMaxY - pMinY).transformAxisAligned(this.pose);
        this.scissorStack.push(screenrectangle);
    }

    public void disableScissor() {
        this.scissorStack.pop();
    }

    public boolean containsPointInScissor(int pX, int pY) {
        return this.scissorStack.containsPoint(pX, pY);
    }

    public void fill(int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor) {
        this.fill(RenderPipelines.GUI, pMinX, pMinY, pMaxX, pMaxY, pColor);
    }

    public void fill(RenderPipeline pPipeline, int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor) {
        if (pMinX < pMaxX) {
            int i = pMinX;
            pMinX = pMaxX;
            pMaxX = i;
        }

        if (pMinY < pMaxY) {
            int j = pMinY;
            pMinY = pMaxY;
            pMaxY = j;
        }

        this.submitColoredRectangle(pPipeline, TextureSetup.noTexture(), pMinX, pMinY, pMaxX, pMaxY, pColor, null);
    }

    public void fillGradient(int pMinX, int pMinY, int pMaxX, int pMaxY, int pColorFrom, int pColorTo) {
        this.submitColoredRectangle(RenderPipelines.GUI, TextureSetup.noTexture(), pMinX, pMinY, pMaxX, pMaxY, pColorFrom, pColorTo);
    }

    public void fill(RenderPipeline pPipeline, TextureSetup pTextureSetup, int pMinX, int pMinY, int pMaxX, int pMaxY) {
        this.submitColoredRectangle(pPipeline, pTextureSetup, pMinX, pMinY, pMaxX, pMaxY, -1, null);
    }

    private void submitColoredRectangle(
        RenderPipeline pPipeline,
        TextureSetup pTextureSetup,
        int pMinX,
        int pMinY,
        int pMaxX,
        int pMaxY,
        int pColorFrom,
        @Nullable Integer pColorTo
    ) {
        this.guiRenderState
            .submitGuiElement(
                new ColoredRectangleRenderState(
                    pPipeline,
                    pTextureSetup,
                    new Matrix3x2f(this.pose),
                    pMinX,
                    pMinY,
                    pMaxX,
                    pMaxY,
                    pColorFrom,
                    pColorTo != null ? pColorTo : pColorFrom,
                    this.scissorStack.peek()
                )
            );
    }

    public void textHighlight(int pMinX, int pMinY, int pMaxX, int pMaxY) {
        this.fill(RenderPipelines.GUI_INVERT, pMinX, pMinY, pMaxX, pMaxY, -1);
        this.fill(RenderPipelines.GUI_TEXT_HIGHLIGHT, pMinX, pMinY, pMaxX, pMaxY, -16776961);
    }

    public void drawCenteredString(Font pFont, String pText, int pX, int pY, int pColor) {
        this.drawString(pFont, pText, pX - pFont.width(pText) / 2, pY, pColor);
    }

    public void drawCenteredString(Font pFont, Component pText, int pX, int pY, int pColor) {
        FormattedCharSequence formattedcharsequence = pText.getVisualOrderText();
        this.drawString(pFont, formattedcharsequence, pX - pFont.width(formattedcharsequence) / 2, pY, pColor);
    }

    public void drawCenteredString(Font pFont, FormattedCharSequence pText, int pX, int pY, int pColor) {
        this.drawString(pFont, pText, pX - pFont.width(pText) / 2, pY, pColor);
    }

    public void drawString(Font pFont, @Nullable String pText, int pX, int pY, int pColor) {
        this.drawString(pFont, pText, pX, pY, pColor, true);
    }

    public void drawString(Font pFont, @Nullable String pText, int pX, int pY, int pColor, boolean pDrawShadow) {
        if (pText != null) {
            this.drawString(pFont, Language.getInstance().getVisualOrder(FormattedText.of(pText)), pX, pY, pColor, pDrawShadow);
        }
    }

    public void drawString(Font pFont, FormattedCharSequence pText, int pX, int pY, int pColor) {
        this.drawString(pFont, pText, pX, pY, pColor, true);
    }

    public void drawString(Font pFont, FormattedCharSequence pText, int pX, int pY, int pColor, boolean pDrawShadow) {
        if (ARGB.alpha(pColor) != 0) {
            this.guiRenderState
                .submitText(
                    new GuiTextRenderState(
                        pFont, pText, new Matrix3x2f(this.pose), pX, pY, pColor, 0, pDrawShadow, this.scissorStack.peek()
                    )
                );
        }
    }

    public void drawString(Font pFont, Component pText, int pX, int pY, int pColor) {
        this.drawString(pFont, pText, pX, pY, pColor, true);
    }

    public void drawString(Font pFont, Component pText, int pX, int pY, int pColor, boolean pDrawShadow) {
        this.drawString(pFont, pText.getVisualOrderText(), pX, pY, pColor, pDrawShadow);
    }

    public void drawWordWrap(Font pFont, FormattedText pText, int pX, int pY, int pLineWidth, int pColor) {
        this.drawWordWrap(pFont, pText, pX, pY, pLineWidth, pColor, true);
    }

    public void drawWordWrap(Font pFont, FormattedText pText, int pX, int pY, int pLineWidth, int pColor, boolean pDropShadow) {
        for (FormattedCharSequence formattedcharsequence : pFont.split(pText, pLineWidth)) {
            this.drawString(pFont, formattedcharsequence, pX, pY, pColor, pDropShadow);
            pY += 9;
        }
    }

    public void drawStringWithBackdrop(Font pFont, Component pText, int pX, int pY, int pWidth, int pColor) {
        int i = this.minecraft.options.getBackgroundColor(0.0F);
        if (i != 0) {
            int j = 2;
            this.fill(pX - 2, pY - 2, pX + pWidth + 2, pY + 9 + 2, ARGB.multiply(i, pColor));
        }

        this.drawString(pFont, pText, pX, pY, pColor, true);
    }

    public void renderOutline(int pX, int pY, int pWidth, int pHeight, int pColor) {
        this.fill(pX, pY, pX + pWidth, pY + 1, pColor);
        this.fill(pX, pY + pHeight - 1, pX + pWidth, pY + pHeight, pColor);
        this.fill(pX, pY + 1, pX + 1, pY + pHeight - 1, pColor);
        this.fill(pX + pWidth - 1, pY + 1, pX + pWidth, pY + pHeight - 1, pColor);
    }

    public void blitSprite(RenderPipeline pPipeline, ResourceLocation pSprite, int pX, int pY, int pWidth, int pHeight) {
        this.blitSprite(pPipeline, pSprite, pX, pY, pWidth, pHeight, -1);
    }

    public void blitSprite(RenderPipeline pPipeline, ResourceLocation pSprite, int pX, int pY, int pWidth, int pHeight, float pFade) {
        this.blitSprite(pPipeline, pSprite, pX, pY, pWidth, pHeight, ARGB.color(pFade, -1));
    }

    public void blitSprite(RenderPipeline pPipeline, ResourceLocation pSprite, int pX, int pY, int pWidth, int pHeight, int pColor) {
        TextureAtlasSprite textureatlassprite = this.sprites.getSprite(pSprite);
        GuiSpriteScaling guispritescaling = this.sprites.getSpriteScaling(textureatlassprite);
        if (guispritescaling instanceof GuiSpriteScaling.Stretch) {
            this.blitSprite(pPipeline, textureatlassprite, pX, pY, pWidth, pHeight, pColor);
        } else if (guispritescaling instanceof GuiSpriteScaling.Tile guispritescaling$tile) {
            this.blitTiledSprite(
                pPipeline,
                textureatlassprite,
                pX,
                pY,
                pWidth,
                pHeight,
                0,
                0,
                guispritescaling$tile.width(),
                guispritescaling$tile.height(),
                guispritescaling$tile.width(),
                guispritescaling$tile.height(),
                pColor
            );
        } else if (guispritescaling instanceof GuiSpriteScaling.NineSlice guispritescaling$nineslice) {
            this.blitNineSlicedSprite(pPipeline, textureatlassprite, guispritescaling$nineslice, pX, pY, pWidth, pHeight, pColor);
        }
    }

    public void blitSprite(
        RenderPipeline pPipeline,
        ResourceLocation pSprite,
        int pTextureWidth,
        int pTextureHeight,
        int pU,
        int pV,
        int pX,
        int pY,
        int pWidth,
        int pHeight
    ) {
        this.blitSprite(pPipeline, pSprite, pTextureWidth, pTextureHeight, pU, pV, pX, pY, pWidth, pHeight, -1);
    }

    public void blitSprite(
        RenderPipeline pPipeline,
        ResourceLocation pSprite,
        int pTextureWidth,
        int pTextureHeight,
        int pU,
        int pV,
        int pX,
        int pY,
        int pWidth,
        int pHeight,
        int pColor
    ) {
        TextureAtlasSprite textureatlassprite = this.sprites.getSprite(pSprite);
        GuiSpriteScaling guispritescaling = this.sprites.getSpriteScaling(textureatlassprite);
        if (guispritescaling instanceof GuiSpriteScaling.Stretch) {
            this.blitSprite(pPipeline, textureatlassprite, pTextureWidth, pTextureHeight, pU, pV, pX, pY, pWidth, pHeight, pColor);
        } else {
            this.enableScissor(pX, pY, pX + pWidth, pY + pHeight);
            this.blitSprite(pPipeline, pSprite, pX - pU, pY - pV, pTextureWidth, pTextureHeight, pColor);
            this.disableScissor();
        }
    }

    public void blitSprite(RenderPipeline pPipeline, TextureAtlasSprite pSprite, int pX, int pWidth, int pY, int pHeight) {
        this.blitSprite(pPipeline, pSprite, pX, pWidth, pY, pHeight, -1);
    }

    public void blitSprite(RenderPipeline pPipeline, TextureAtlasSprite pSprite, int pX, int pY, int pWidth, int pHeight, int pColor) {
        if (pWidth != 0 && pHeight != 0) {
            this.innerBlit(
                pPipeline,
                pSprite.atlasLocation(),
                pX,
                pX + pWidth,
                pY,
                pY + pHeight,
                pSprite.getU0(),
                pSprite.getU1(),
                pSprite.getV0(),
                pSprite.getV1(),
                pColor
            );
        }
    }

    private void blitSprite(
        RenderPipeline pPipeline,
        TextureAtlasSprite pSprite,
        int pTextureWidth,
        int pTextureHeight,
        int pU,
        int pV,
        int pX,
        int pY,
        int pWidth,
        int pHeight,
        int pColor
    ) {
        if (pWidth != 0 && pHeight != 0) {
            this.innerBlit(
                pPipeline,
                pSprite.atlasLocation(),
                pX,
                pX + pWidth,
                pY,
                pY + pHeight,
                pSprite.getU((float)pU / pTextureWidth),
                pSprite.getU((float)(pU + pWidth) / pTextureWidth),
                pSprite.getV((float)pV / pTextureHeight),
                pSprite.getV((float)(pV + pHeight) / pTextureHeight),
                pColor
            );
        }
    }

    private void blitNineSlicedSprite(
        RenderPipeline pPipeline,
        TextureAtlasSprite pSprite,
        GuiSpriteScaling.NineSlice pNineSlice,
        int pX,
        int pY,
        int pWidth,
        int pHeight,
        int pColor
    ) {
        GuiSpriteScaling.NineSlice.Border guispritescaling$nineslice$border = pNineSlice.border();
        int i = Math.min(guispritescaling$nineslice$border.left(), pWidth / 2);
        int j = Math.min(guispritescaling$nineslice$border.right(), pWidth / 2);
        int k = Math.min(guispritescaling$nineslice$border.top(), pHeight / 2);
        int l = Math.min(guispritescaling$nineslice$border.bottom(), pHeight / 2);
        if (pWidth == pNineSlice.width() && pHeight == pNineSlice.height()) {
            this.blitSprite(pPipeline, pSprite, pNineSlice.width(), pNineSlice.height(), 0, 0, pX, pY, pWidth, pHeight, pColor);
        } else if (pHeight == pNineSlice.height()) {
            this.blitSprite(pPipeline, pSprite, pNineSlice.width(), pNineSlice.height(), 0, 0, pX, pY, i, pHeight, pColor);
            this.blitNineSliceInnerSegment(
                pPipeline,
                pNineSlice,
                pSprite,
                pX + i,
                pY,
                pWidth - j - i,
                pHeight,
                i,
                0,
                pNineSlice.width() - j - i,
                pNineSlice.height(),
                pNineSlice.width(),
                pNineSlice.height(),
                pColor
            );
            this.blitSprite(
                pPipeline,
                pSprite,
                pNineSlice.width(),
                pNineSlice.height(),
                pNineSlice.width() - j,
                0,
                pX + pWidth - j,
                pY,
                j,
                pHeight,
                pColor
            );
        } else if (pWidth == pNineSlice.width()) {
            this.blitSprite(pPipeline, pSprite, pNineSlice.width(), pNineSlice.height(), 0, 0, pX, pY, pWidth, k, pColor);
            this.blitNineSliceInnerSegment(
                pPipeline,
                pNineSlice,
                pSprite,
                pX,
                pY + k,
                pWidth,
                pHeight - l - k,
                0,
                k,
                pNineSlice.width(),
                pNineSlice.height() - l - k,
                pNineSlice.width(),
                pNineSlice.height(),
                pColor
            );
            this.blitSprite(
                pPipeline,
                pSprite,
                pNineSlice.width(),
                pNineSlice.height(),
                0,
                pNineSlice.height() - l,
                pX,
                pY + pHeight - l,
                pWidth,
                l,
                pColor
            );
        } else {
            this.blitSprite(pPipeline, pSprite, pNineSlice.width(), pNineSlice.height(), 0, 0, pX, pY, i, k, pColor);
            this.blitNineSliceInnerSegment(
                pPipeline,
                pNineSlice,
                pSprite,
                pX + i,
                pY,
                pWidth - j - i,
                k,
                i,
                0,
                pNineSlice.width() - j - i,
                k,
                pNineSlice.width(),
                pNineSlice.height(),
                pColor
            );
            this.blitSprite(
                pPipeline,
                pSprite,
                pNineSlice.width(),
                pNineSlice.height(),
                pNineSlice.width() - j,
                0,
                pX + pWidth - j,
                pY,
                j,
                k,
                pColor
            );
            this.blitSprite(
                pPipeline,
                pSprite,
                pNineSlice.width(),
                pNineSlice.height(),
                0,
                pNineSlice.height() - l,
                pX,
                pY + pHeight - l,
                i,
                l,
                pColor
            );
            this.blitNineSliceInnerSegment(
                pPipeline,
                pNineSlice,
                pSprite,
                pX + i,
                pY + pHeight - l,
                pWidth - j - i,
                l,
                i,
                pNineSlice.height() - l,
                pNineSlice.width() - j - i,
                l,
                pNineSlice.width(),
                pNineSlice.height(),
                pColor
            );
            this.blitSprite(
                pPipeline,
                pSprite,
                pNineSlice.width(),
                pNineSlice.height(),
                pNineSlice.width() - j,
                pNineSlice.height() - l,
                pX + pWidth - j,
                pY + pHeight - l,
                j,
                l,
                pColor
            );
            this.blitNineSliceInnerSegment(
                pPipeline,
                pNineSlice,
                pSprite,
                pX,
                pY + k,
                i,
                pHeight - l - k,
                0,
                k,
                i,
                pNineSlice.height() - l - k,
                pNineSlice.width(),
                pNineSlice.height(),
                pColor
            );
            this.blitNineSliceInnerSegment(
                pPipeline,
                pNineSlice,
                pSprite,
                pX + i,
                pY + k,
                pWidth - j - i,
                pHeight - l - k,
                i,
                k,
                pNineSlice.width() - j - i,
                pNineSlice.height() - l - k,
                pNineSlice.width(),
                pNineSlice.height(),
                pColor
            );
            this.blitNineSliceInnerSegment(
                pPipeline,
                pNineSlice,
                pSprite,
                pX + pWidth - j,
                pY + k,
                j,
                pHeight - l - k,
                pNineSlice.width() - j,
                k,
                j,
                pNineSlice.height() - l - k,
                pNineSlice.width(),
                pNineSlice.height(),
                pColor
            );
        }
    }

    private void blitNineSliceInnerSegment(
        RenderPipeline pPipeline,
        GuiSpriteScaling.NineSlice pNineSlice,
        TextureAtlasSprite pSprite,
        int pBorderMinX,
        int pBorderMinY,
        int pBorderMaxX,
        int pBorderMaxY,
        int pU,
        int pV,
        int pSpriteWidth,
        int pSpriteHeight,
        int pTextureWidth,
        int pTextureHeight,
        int pColor
    ) {
        if (pBorderMaxX > 0 && pBorderMaxY > 0) {
            if (pNineSlice.stretchInner()) {
                this.innerBlit(
                    pPipeline,
                    pSprite.atlasLocation(),
                    pBorderMinX,
                    pBorderMinX + pBorderMaxX,
                    pBorderMinY,
                    pBorderMinY + pBorderMaxY,
                    pSprite.getU((float)pU / pTextureWidth),
                    pSprite.getU((float)(pU + pSpriteWidth) / pTextureWidth),
                    pSprite.getV((float)pV / pTextureHeight),
                    pSprite.getV((float)(pV + pSpriteHeight) / pTextureHeight),
                    pColor
                );
            } else {
                this.blitTiledSprite(
                    pPipeline,
                    pSprite,
                    pBorderMinX,
                    pBorderMinY,
                    pBorderMaxX,
                    pBorderMaxY,
                    pU,
                    pV,
                    pSpriteWidth,
                    pSpriteHeight,
                    pTextureWidth,
                    pTextureHeight,
                    pColor
                );
            }
        }
    }

    private void blitTiledSprite(
        RenderPipeline pPipeline,
        TextureAtlasSprite pSprite,
        int pX,
        int pY,
        int pWidth,
        int pHeight,
        int pU,
        int pV,
        int pSpriteWidth,
        int pSpriteHeight,
        int pTextureWidth,
        int pTextureHeight,
        int pColor
    ) {
        if (pWidth > 0 && pHeight > 0) {
            if (pSpriteWidth > 0 && pSpriteHeight > 0) {
                for (int i = 0; i < pWidth; i += pSpriteWidth) {
                    int j = Math.min(pSpriteWidth, pWidth - i);

                    for (int k = 0; k < pHeight; k += pSpriteHeight) {
                        int l = Math.min(pSpriteHeight, pHeight - k);
                        this.blitSprite(pPipeline, pSprite, pTextureWidth, pTextureHeight, pU, pV, pX + i, pY + k, j, l, pColor);
                    }
                }
            } else {
                throw new IllegalArgumentException("Tiled sprite texture size must be positive, got " + pSpriteWidth + "x" + pSpriteHeight);
            }
        }
    }

    public void blit(
        RenderPipeline pPipeline,
        ResourceLocation pAtlas,
        int pX,
        int pY,
        float pU,
        float pV,
        int pWidth,
        int pHeight,
        int pTextureWidth,
        int pTextureHeight,
        int pColor
    ) {
        this.blit(
            pPipeline, pAtlas, pX, pY, pU, pV, pWidth, pHeight, pWidth, pHeight, pTextureWidth, pTextureHeight, pColor
        );
    }

    public void blit(
        RenderPipeline pPipeline,
        ResourceLocation pAtlas,
        int pX,
        int pY,
        float pU,
        float pV,
        int pWidth,
        int pHeight,
        int pTextureWidth,
        int pTextureHeight
    ) {
        this.blit(pPipeline, pAtlas, pX, pY, pU, pV, pWidth, pHeight, pWidth, pHeight, pTextureWidth, pTextureHeight);
    }

    public void blit(
        RenderPipeline pPipeline,
        ResourceLocation pAtlas,
        int pX,
        int pY,
        float pU,
        float pV,
        int pWidth,
        int pHeight,
        int pUWidth,
        int pVHeight,
        int pTextureWidth,
        int pTextureHeight
    ) {
        this.blit(pPipeline, pAtlas, pX, pY, pU, pV, pWidth, pHeight, pUWidth, pVHeight, pTextureWidth, pTextureHeight, -1);
    }

    public void blit(
        RenderPipeline pPipeline,
        ResourceLocation pAtlas,
        int pX,
        int pY,
        float pU,
        float pV,
        int pWidth,
        int pHeight,
        int pUWidth,
        int pVHeight,
        int pTextureWidth,
        int pTextureHeight,
        int pColor
    ) {
        this.innerBlit(
            pPipeline,
            pAtlas,
            pX,
            pX + pWidth,
            pY,
            pY + pHeight,
            (pU + 0.0F) / pTextureWidth,
            (pU + pUWidth) / pTextureWidth,
            (pV + 0.0F) / pTextureHeight,
            (pV + pVHeight) / pTextureHeight,
            pColor
        );
    }

    public void blit(
        ResourceLocation pAtlas,
        int pX0,
        int pY0,
        int pX1,
        int pY1,
        float pU0,
        float pU1,
        float pV0,
        float pV1
    ) {
        this.innerBlit(RenderPipelines.GUI_TEXTURED, pAtlas, pX0, pX1, pY0, pY1, pU0, pU1, pV0, pV1, -1);
    }

    private void innerBlit(
        RenderPipeline pPipeline,
        ResourceLocation pAtlas,
        int pX0,
        int pX1,
        int pY0,
        int pY1,
        float pU0,
        float pU1,
        float pV0,
        float pV1,
        int pColor
    ) {
        GpuTextureView gputextureview = this.minecraft.getTextureManager().getTexture(pAtlas).getTextureView();
        this.submitBlit(pPipeline, gputextureview, pX0, pY0, pX1, pY1, pU0, pU1, pV0, pV1, pColor);
    }

    private void submitBlit(
        RenderPipeline pPipeline,
        GpuTextureView pAtlasTexture,
        int pX0,
        int pY0,
        int pX1,
        int pY1,
        float pU0,
        float pU1,
        float pV0,
        float pV1,
        int pColor
    ) {
        this.guiRenderState
            .submitGuiElement(
                new BlitRenderState(
                    pPipeline,
                    TextureSetup.singleTexture(pAtlasTexture),
                    new Matrix3x2f(this.pose),
                    pX0,
                    pY0,
                    pX1,
                    pY1,
                    pU0,
                    pU1,
                    pV0,
                    pV1,
                    pColor,
                    this.scissorStack.peek()
                )
            );
    }

    public void renderItem(ItemStack pStack, int pX, int pY) {
        this.renderItem(this.minecraft.player, this.minecraft.level, pStack, pX, pY, 0);
    }

    public void renderItem(ItemStack pStack, int pX, int pY, int pSeed) {
        this.renderItem(this.minecraft.player, this.minecraft.level, pStack, pX, pY, pSeed);
    }

    public void renderFakeItem(ItemStack pStack, int pX, int pY) {
        this.renderFakeItem(pStack, pX, pY, 0);
    }

    public void renderFakeItem(ItemStack pStack, int pX, int pY, int pSeed) {
        this.renderItem(null, this.minecraft.level, pStack, pX, pY, pSeed);
    }

    public void renderItem(LivingEntity pEntity, ItemStack pStack, int pX, int pY, int pSeed) {
        this.renderItem(pEntity, pEntity.level(), pStack, pX, pY, pSeed);
    }

    private void renderItem(@Nullable LivingEntity pEntity, @Nullable Level pLevel, ItemStack pStack, int pX, int pY, int pSeed) {
        if (!pStack.isEmpty()) {
            TrackingItemStackRenderState trackingitemstackrenderstate = new TrackingItemStackRenderState();
            this.minecraft.getItemModelResolver().updateForTopItem(trackingitemstackrenderstate, pStack, ItemDisplayContext.GUI, pLevel, pEntity, pSeed);

            try {
                this.guiRenderState
                    .submitItem(
                        new GuiItemRenderState(
                            pStack.getItem().getName().toString(),
                            new Matrix3x2f(this.pose),
                            trackingitemstackrenderstate,
                            pX,
                            pY,
                            this.scissorStack.peek()
                        )
                    );
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering item");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Item being rendered");
                crashreportcategory.setDetail("Item Type", () -> String.valueOf(pStack.getItem()));
                crashreportcategory.setDetail("Registry Name", () -> String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(pStack.getItem())));
                crashreportcategory.setDetail("Item Components", () -> String.valueOf(pStack.getComponents()));
                crashreportcategory.setDetail("Item Foil", () -> String.valueOf(pStack.hasFoil()));
                throw new ReportedException(crashreport);
            }
        }
    }

    public void renderItemDecorations(Font pFont, ItemStack pStack, int pX, int pY) {
        this.renderItemDecorations(pFont, pStack, pX, pY, null);
    }

    public void renderItemDecorations(Font pFont, ItemStack pStack, int pX, int pY, @Nullable String pText) {
        if (!pStack.isEmpty()) {
            this.pose.pushMatrix();
            this.renderItemBar(pStack, pX, pY);
            this.renderItemCooldown(pStack, pX, pY);
            this.renderItemCount(pFont, pStack, pX, pY, pText);
            this.pose.popMatrix();
            net.minecraftforge.client.ItemDecoratorHandler.of(pStack).render(this, pFont, pStack, pX, pY);
        }
    }

    public void setTooltipForNextFrame(Component pText, int pX, int pY) {
        this.setTooltipForNextFrame(List.of(pText.getVisualOrderText()), pX, pY);
    }

    public void setTooltipForNextFrame(List<FormattedCharSequence> pLines, int pX, int pY) {
        this.setTooltipForNextFrame(this.minecraft.font, pLines, DefaultTooltipPositioner.INSTANCE, pX, pY, false);
    }

    private ItemStack tooltipStack = ItemStack.EMPTY;

    public void setTooltipForNextFrame(Font pFont, ItemStack pStack, int pX, int pY) {
        this.tooltipStack = pStack;
        this.setTooltipForNextFrame(
            pFont, Screen.getTooltipFromItem(this.minecraft, pStack), pStack.getTooltipImage(), pX, pY, pStack.get(DataComponents.TOOLTIP_STYLE)
        );
        this.tooltipStack = ItemStack.EMPTY;
    }

    public void setTooltipForNextFrame(Font pFont, List<Component> pLines, Optional<TooltipComponent> pTooltipImage, int pX, int pY) {
        this.setTooltipForNextFrame(pFont, pLines, pTooltipImage, pX, pY, null);
    }

    public void setTooltipForNextFrame(Font font, List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, ItemStack stack, int mouseX, int mouseY) {
       this.tooltipStack = stack;
       this.setTooltipForNextFrame(font, textComponents, tooltipComponent, mouseX, mouseY, stack.get(DataComponents.TOOLTIP_STYLE));
       this.tooltipStack = ItemStack.EMPTY;
    }

    public void setTooltipForNextFrame(
        Font pFont, List<Component> pLines, Optional<TooltipComponent> pTooltipImage, int pX, int pY, @Nullable ResourceLocation pBackground
    ) {
        List<ClientTooltipComponent> list = net.minecraftforge.client.ForgeHooksClient.gatherTooltipComponents(this.tooltipStack, pLines, pTooltipImage, pX, guiWidth(), guiHeight(), pFont);
        this.setTooltipForNextFrameInternal(pFont, list, pX, pY, DefaultTooltipPositioner.INSTANCE, pBackground, false);
    }

    public void setTooltipForNextFrame(Font pFont, Component pText, int pX, int pY) {
        this.setTooltipForNextFrame(pFont, pText, pX, pY, null);
    }

    public void setTooltipForNextFrame(Font pFont, Component pText, int pX, int pY, @Nullable ResourceLocation pBackground) {
        this.setTooltipForNextFrame(pFont, List.of(pText.getVisualOrderText()), pX, pY, pBackground);
    }

    public void setComponentTooltipForNextFrame(Font pFont, List<Component> pLines, int pX, int pY) {
        List<ClientTooltipComponent> components = net.minecraftforge.client.ForgeHooksClient.gatherTooltipComponents(this.tooltipStack, pLines, pX, guiWidth(), guiHeight(), pFont);
        this.setTooltipForNextFrameInternal(pFont, components, pX, pY, DefaultTooltipPositioner.INSTANCE, null, false);
    }

    public void renderComponentTooltip(Font font, List<? extends net.minecraft.network.chat.FormattedText> tooltips, int mouseX, int mouseY, ItemStack stack) {
        this.tooltipStack = stack;
        List<ClientTooltipComponent> components = net.minecraftforge.client.ForgeHooksClient.gatherTooltipComponents(stack, tooltips, mouseX, guiWidth(), guiHeight(), font);
        this.setTooltipForNextFrameInternal(font, components, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, false);
        this.tooltipStack = ItemStack.EMPTY;
    }

    public void renderComponentTooltipFromElements(Font font, List<com.mojang.datafixers.util.Either<FormattedText, TooltipComponent>> elements, int mouseX, int mouseY, ItemStack stack) {
        this.tooltipStack = stack;
        List<ClientTooltipComponent> components = net.minecraftforge.client.ForgeHooksClient.gatherTooltipComponentsFromElements(stack, elements, mouseX, guiWidth(), guiHeight(), font);
        this.setTooltipForNextFrameInternal(font, components, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, false);
        this.tooltipStack = ItemStack.EMPTY;
    }

    public void setComponentTooltipForNextFrame(Font pFont, List<Component> pLines, int pX, int pY, @Nullable ResourceLocation pBackground) {
        this.setTooltipForNextFrameInternal(
            pFont,
            pLines.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).toList(),
            pX,
            pY,
            DefaultTooltipPositioner.INSTANCE,
            pBackground,
            false
        );
    }

    public void setTooltipForNextFrame(Font pFont, List<? extends FormattedCharSequence> pLines, int pX, int pY) {
        this.setTooltipForNextFrame(pFont, pLines, pX, pY, null);
    }

    public void setTooltipForNextFrame(Font pFont, List<? extends FormattedCharSequence> pLines, int pX, int pY, @Nullable ResourceLocation pBackground) {
        this.setTooltipForNextFrameInternal(
            pFont,
            pLines.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()),
            pX,
            pY,
            DefaultTooltipPositioner.INSTANCE,
            pBackground,
            false
        );
    }

    public void setTooltipForNextFrame(
        Font pFont, List<FormattedCharSequence> pLines, ClientTooltipPositioner pPositioner, int pX, int pY, boolean pFocused
    ) {
        this.setTooltipForNextFrameInternal(
            pFont, pLines.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()), pX, pY, pPositioner, null, pFocused
        );
    }

    private void setTooltipForNextFrameInternal(
        Font pFont,
        List<ClientTooltipComponent> pComponents,
        int pX,
        int pY,
        ClientTooltipPositioner pPositioner,
        @Nullable ResourceLocation pBackground,
        boolean pFocused
    ) {
        if (!pComponents.isEmpty()) {
            if (this.deferredTooltip == null || pFocused) {
                this.deferredTooltip = () -> this.renderTooltip(pFont, pComponents, pX, pY, pPositioner, pBackground);
            }
        }
    }

    public void renderTooltip(
        Font pFont,
        List<ClientTooltipComponent> pComponents,
        int pX,
        int pY,
        ClientTooltipPositioner pPositioner,
        @Nullable ResourceLocation pBackground
    ) {
        var preEvent = net.minecraftforge.client.ForgeHooksClient.onRenderTooltipPre(this.tooltipStack, this, pX, pY, guiWidth(), guiHeight(), pComponents, pFont, pPositioner);
        if (preEvent == null) return;
        int i = 0;
        int j = pComponents.size() == 1 ? -2 : 0;

        for (ClientTooltipComponent clienttooltipcomponent : pComponents) {
            int k = clienttooltipcomponent.getWidth(preEvent.getFont());
            if (k > i) {
                i = k;
            }

            j += clienttooltipcomponent.getHeight(pFont);
        }

        int l1 = i;
        int i2 = j;
        Vector2ic vector2ic = pPositioner.positionTooltip(this.guiWidth(), this.guiHeight(), preEvent.getX(), preEvent.getY(), i, j);
        int l = vector2ic.x();
        int i1 = vector2ic.y();
        this.pose.pushMatrix();
        var background_event = net.minecraftforge.client.event.ForgeEventFactoryClient.onRenderTooltipBackground(this.tooltipStack, this, l, i1, preEvent.getFont(), pComponents, pBackground);
        TooltipRenderUtil.renderTooltipBackground(this, l, i1, i, j, background_event.getBackground());
        int j1 = i1;

        for (int k1 = 0; k1 < pComponents.size(); k1++) {
            ClientTooltipComponent clienttooltipcomponent1 = pComponents.get(k1);
            clienttooltipcomponent1.renderText(this, preEvent.getFont(), l, j1);
            j1 += clienttooltipcomponent1.getHeight(pFont) + (k1 == 0 ? 2 : 0);
        }

        j1 = i1;

        for (int j2 = 0; j2 < pComponents.size(); j2++) {
            ClientTooltipComponent clienttooltipcomponent2 = pComponents.get(j2);
            clienttooltipcomponent2.renderImage(preEvent.getFont(), l, j1, l1, i2, this);
            j1 += clienttooltipcomponent2.getHeight(pFont) + (j2 == 0 ? 2 : 0);
        }

        this.pose.popMatrix();
    }

    public void renderDeferredTooltip() {
        if (this.deferredTooltip != null) {
            this.nextStratum();
            this.deferredTooltip.run();
            this.deferredTooltip = null;
        }
    }

    private void renderItemBar(ItemStack pStack, int pX, int pY) {
        if (pStack.isBarVisible()) {
            int i = pX + 2;
            int j = pY + 13;
            this.fill(RenderPipelines.GUI, i, j, i + 13, j + 2, -16777216);
            this.fill(RenderPipelines.GUI, i, j, i + pStack.getBarWidth(), j + 1, ARGB.opaque(pStack.getBarColor()));
        }
    }

    private void renderItemCount(Font pFont, ItemStack pStack, int pX, int pY, @Nullable String pText) {
        if (pStack.getCount() != 1 || pText != null) {
            String s = pText == null ? String.valueOf(pStack.getCount()) : pText;
            this.drawString(pFont, s, pX + 19 - 2 - pFont.width(s), pY + 6 + 3, -1, true);
        }
    }

    private void renderItemCooldown(ItemStack pStack, int pX, int pY) {
        LocalPlayer localplayer = this.minecraft.player;
        float f = localplayer == null ? 0.0F : localplayer.getCooldowns().getCooldownPercent(pStack, this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        if (f > 0.0F) {
            int i = pY + Mth.floor(16.0F * (1.0F - f));
            int j = i + Mth.ceil(16.0F * f);
            this.fill(RenderPipelines.GUI, pX, i, pX + 16, j, Integer.MAX_VALUE);
        }
    }

    public void renderComponentHoverEffect(Font pFont, @Nullable Style pStyle, int pMouseX, int pMouseY) {
        if (pStyle != null && pStyle.getHoverEvent() != null) {
            switch (pStyle.getHoverEvent()) {
                case HoverEvent.ShowItem(ItemStack itemstack):
                    this.setTooltipForNextFrame(pFont, itemstack, pMouseX, pMouseY);
                    break;
                case HoverEvent.ShowEntity(HoverEvent.EntityTooltipInfo hoverevent$entitytooltipinfo1):
                    HoverEvent.EntityTooltipInfo hoverevent$entitytooltipinfo = hoverevent$entitytooltipinfo1;
                    if (this.minecraft.options.advancedItemTooltips) {
                        this.setComponentTooltipForNextFrame(pFont, hoverevent$entitytooltipinfo.getTooltipLines(), pMouseX, pMouseY);
                    }
                    break;
                case HoverEvent.ShowText(Component component):
                    this.setTooltipForNextFrame(pFont, pFont.split(component, Math.max(this.guiWidth() / 2, 200)), pMouseX, pMouseY);
                    break;
                default:
            }
        }
    }

    public void submitMapRenderState(MapRenderState pRenderState) {
        Minecraft minecraft = Minecraft.getInstance();
        TextureManager texturemanager = minecraft.getTextureManager();
        GpuTextureView gputextureview = texturemanager.getTexture(pRenderState.texture).getTextureView();
        this.submitBlit(RenderPipelines.GUI_TEXTURED, gputextureview, 0, 0, 128, 128, 0.0F, 1.0F, 0.0F, 1.0F, -1);

        for (MapRenderState.MapDecorationRenderState maprenderstate$mapdecorationrenderstate : pRenderState.decorations) {
            if (maprenderstate$mapdecorationrenderstate.renderOnFrame) {
                this.pose.pushMatrix();
                this.pose
                    .translate(
                        maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F, maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F
                    );
                this.pose.rotate((float) (Math.PI / 180.0) * maprenderstate$mapdecorationrenderstate.rot * 360.0F / 16.0F);
                this.pose.scale(4.0F, 4.0F);
                this.pose.translate(-0.125F, 0.125F);
                TextureAtlasSprite textureatlassprite = maprenderstate$mapdecorationrenderstate.atlasSprite;
                if (textureatlassprite != null) {
                    GpuTextureView gputextureview1 = texturemanager.getTexture(textureatlassprite.atlasLocation()).getTextureView();
                    this.submitBlit(
                        RenderPipelines.GUI_TEXTURED,
                        gputextureview1,
                        -1,
                        -1,
                        1,
                        1,
                        textureatlassprite.getU0(),
                        textureatlassprite.getU1(),
                        textureatlassprite.getV1(),
                        textureatlassprite.getV0(),
                        -1
                    );
                }

                this.pose.popMatrix();
                if (maprenderstate$mapdecorationrenderstate.name != null) {
                    Font font = minecraft.font;
                    float f = font.width(maprenderstate$mapdecorationrenderstate.name);
                    float f1 = Mth.clamp(25.0F / f, 0.0F, 6.0F / 9.0F);
                    this.pose.pushMatrix();
                    this.pose
                        .translate(
                            maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F - f * f1 / 2.0F,
                            maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F + 4.0F
                        );
                    this.pose.scale(f1, f1);
                    this.guiRenderState
                        .submitText(
                            new GuiTextRenderState(
                                font,
                                maprenderstate$mapdecorationrenderstate.name.getVisualOrderText(),
                                new Matrix3x2f(this.pose),
                                0,
                                0,
                                -1,
                                Integer.MIN_VALUE,
                                false,
                                this.scissorStack.peek()
                            )
                        );
                    this.pose.popMatrix();
                }
            }
        }
    }

    public void submitEntityRenderState(
        EntityRenderState pRenderState,
        float pScale,
        Vector3f pTranslation,
        Quaternionf pRotation,
        @Nullable Quaternionf pOverrideCameraAngle,
        int pX0,
        int pY0,
        int pX1,
        int pY1
    ) {
        this.guiRenderState
            .submitPicturesInPictureState(
                new GuiEntityRenderState(
                    pRenderState, pTranslation, pRotation, pOverrideCameraAngle, pX0, pY0, pX1, pY1, pScale, this.scissorStack.peek()
                )
            );
    }

    public void submitSkinRenderState(
        PlayerModel pPlayerModel,
        ResourceLocation pTexture,
        float pScale,
        float pRotationX,
        float pRotationY,
        float pPivotY,
        int pX0,
        int pY0,
        int pX1,
        int pY1
    ) {
        this.guiRenderState
            .submitPicturesInPictureState(
                new GuiSkinRenderState(
                    pPlayerModel, pTexture, pRotationX, pRotationY, pPivotY, pX0, pY0, pX1, pY1, pScale, this.scissorStack.peek()
                )
            );
    }

    public void submitBookModelRenderState(
        BookModel pBookModel,
        ResourceLocation pTexture,
        float pScale,
        float pOpen,
        float pFlip,
        int pX0,
        int pY0,
        int pX1,
        int pY1
    ) {
        this.guiRenderState
            .submitPicturesInPictureState(
                new GuiBookModelRenderState(
                    pBookModel, pTexture, pOpen, pFlip, pX0, pY0, pX1, pY1, pScale, this.scissorStack.peek()
                )
            );
    }

    public void submitBannerPatternRenderState(ModelPart pFlag, DyeColor pBaseColor, BannerPatternLayers pResultBannerPatterns, int pX0, int pY0, int pX1, int pY1) {
        this.guiRenderState
            .submitPicturesInPictureState(new GuiBannerResultRenderState(pFlag, pBaseColor, pResultBannerPatterns, pX0, pY0, pX1, pY1, this.scissorStack.peek()));
    }

    public void submitSignRenderState(Model pSignModel, float pScale, WoodType pWoodType, int pX0, int pY0, int pX1, int pY1) {
        this.guiRenderState
            .submitPicturesInPictureState(new GuiSignRenderState(pSignModel, pWoodType, pX0, pY0, pX1, pY1, pScale, this.scissorStack.peek()));
    }

    public void submitProfilerChartRenderState(List<ResultField> pChartData, int pX0, int pY0, int pX1, int pY1) {
        this.guiRenderState.submitPicturesInPictureState(new GuiProfilerChartRenderState(pChartData, pX0, pY0, pX1, pY1, this.scissorStack.peek()));
    }

    public GuiRenderState getRenderState() {
        return this.guiRenderState;
    }

    public GuiGraphics.ScissorStack getScissorStack() {
        return this.scissorStack;
    }

    /**
     * A utility class for managing a stack of screen rectangles for scissoring.
     */
    @OnlyIn(Dist.CLIENT)
    public static class ScissorStack {
        private final Deque<ScreenRectangle> stack = new ArrayDeque<>();

        public ScreenRectangle push(ScreenRectangle pScissor) {
            ScreenRectangle screenrectangle = this.stack.peekLast();
            if (screenrectangle != null) {
                ScreenRectangle screenrectangle1 = Objects.requireNonNullElse(pScissor.intersection(screenrectangle), ScreenRectangle.empty());
                this.stack.addLast(screenrectangle1);
                return screenrectangle1;
            } else {
                this.stack.addLast(pScissor);
                return pScissor;
            }
        }

        @Nullable
        public ScreenRectangle pop() {
            if (this.stack.isEmpty()) {
                throw new IllegalStateException("Scissor stack underflow");
            } else {
                this.stack.removeLast();
                return this.stack.peekLast();
            }
        }

        @Nullable
        public ScreenRectangle peek() {
            return this.stack.peekLast();
        }

        public boolean containsPoint(int pX, int pY) {
            return this.stack.isEmpty() ? true : this.stack.peek().containsPoint(pX, pY);
        }
    }
}
