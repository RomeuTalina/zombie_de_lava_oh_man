package net.minecraft.client.gui.render;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.OversizedItemRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GlyphEffectRenderState;
import net.minecraft.client.gui.render.state.GlyphRenderState;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.client.gui.render.state.pip.OversizedItemRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GuiRenderer implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float MAX_GUI_Z = 10000.0F;
    private static final float MIN_GUI_Z = 0.0F;
    private static final float GUI_Z_NEAR = 1000.0F;
    public static final int GUI_3D_Z_FAR = 1000;
    public static final int GUI_3D_Z_NEAR = -1000;
    public static final int DEFAULT_ITEM_SIZE = 16;
    private static final int MINIMUM_ITEM_ATLAS_SIZE = 512;
    private static final int MAXIMUM_ITEM_ATLAS_SIZE = RenderSystem.getDevice().getMaxTextureSize();
    public static final int CLEAR_COLOR = 0;
    private static final Comparator<ScreenRectangle> SCISSOR_COMPARATOR = Comparator.nullsFirst(
        Comparator.comparing(ScreenRectangle::top)
            .thenComparing(ScreenRectangle::bottom)
            .thenComparing(ScreenRectangle::left)
            .thenComparing(ScreenRectangle::right)
    );
    private static final Comparator<TextureSetup> TEXTURE_COMPARATOR = Comparator.nullsFirst(Comparator.comparing(TextureSetup::getSortKey));
    private static final Comparator<GuiElementRenderState> ELEMENT_SORT_COMPARATOR = Comparator.comparing(GuiElementRenderState::scissorArea, SCISSOR_COMPARATOR)
        .thenComparing(GuiElementRenderState::pipeline, Comparator.comparing(RenderPipeline::getSortKey))
        .thenComparing(GuiElementRenderState::textureSetup, TEXTURE_COMPARATOR);
    private final Map<Object, GuiRenderer.AtlasPosition> atlasPositions = new Object2ObjectOpenHashMap<>();
    private final Map<Object, OversizedItemRenderer> oversizedItemRenderers = new Object2ObjectOpenHashMap<>();
    final GuiRenderState renderState;
    private final List<GuiRenderer.Draw> draws = new ArrayList<>();
    private final List<GuiRenderer.MeshToDraw> meshesToDraw = new ArrayList<>();
    private final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(786432);
    private final Map<VertexFormat, MappableRingBuffer> vertexBuffers = new Object2ObjectOpenHashMap<>();
    private int firstDrawIndexAfterBlur = Integer.MAX_VALUE;
    private final CachedOrthoProjectionMatrixBuffer guiProjectionMatrixBuffer = new CachedOrthoProjectionMatrixBuffer("gui", 1000.0F, 11000.0F, true);
    private final CachedOrthoProjectionMatrixBuffer itemsProjectionMatrixBuffer = new CachedOrthoProjectionMatrixBuffer("items", -1000.0F, 1000.0F, true);
    private final MultiBufferSource.BufferSource bufferSource;
    private final Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> pictureInPictureRenderers;
    @Nullable
    private GpuTexture itemsAtlas;
    @Nullable
    private GpuTextureView itemsAtlasView;
    @Nullable
    private GpuTexture itemsAtlasDepth;
    @Nullable
    private GpuTextureView itemsAtlasDepthView;
    private int itemAtlasX;
    private int itemAtlasY;
    private int cachedGuiScale;
    private int frameNumber;
    @Nullable
    private ScreenRectangle previousScissorArea = null;
    @Nullable
    private RenderPipeline previousPipeline = null;
    @Nullable
    private TextureSetup previousTextureSetup = null;
    @Nullable
    private BufferBuilder bufferBuilder = null;

    public GuiRenderer(GuiRenderState pRenderState, MultiBufferSource.BufferSource pBufferSource, List<PictureInPictureRenderer<?>> pPictureInPictureRenderers) {
        this.renderState = pRenderState;
        this.bufferSource = pBufferSource;
        Builder<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> builder = ImmutableMap.builder();

        for (PictureInPictureRenderer<?> pictureinpicturerenderer : pPictureInPictureRenderers) {
            builder.put((Class<? extends PictureInPictureRenderState>)pictureinpicturerenderer.getRenderStateClass(), pictureinpicturerenderer);
        }

        net.minecraftforge.client.ForgeHooksClient.onRegisterPictureInPictureRenderers(pPictureInPictureRenderers, pBufferSource, builder);
        this.pictureInPictureRenderers = builder.buildOrThrow();
    }

    public void incrementFrameNumber() {
        this.frameNumber++;
    }

    public void render(GpuBufferSlice pBufferSlice) {
        this.prepare();
        this.draw(pBufferSlice);

        for (MappableRingBuffer mappableringbuffer : this.vertexBuffers.values()) {
            mappableringbuffer.rotate();
        }

        this.draws.clear();
        this.meshesToDraw.clear();
        this.renderState.reset();
        this.firstDrawIndexAfterBlur = Integer.MAX_VALUE;
        this.clearUnusedOversizedItemRenderers();
    }

    private void clearUnusedOversizedItemRenderers() {
        Iterator<Entry<Object, OversizedItemRenderer>> iterator = this.oversizedItemRenderers.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<Object, OversizedItemRenderer> entry = iterator.next();
            OversizedItemRenderer oversizeditemrenderer = entry.getValue();
            if (!oversizeditemrenderer.usedOnThisFrame()) {
                oversizeditemrenderer.close();
                iterator.remove();
            } else {
                oversizeditemrenderer.resetUsedOnThisFrame();
            }
        }
    }

    private void prepare() {
        this.bufferSource.endBatch();
        this.preparePictureInPicture();
        this.prepareItemElements();
        this.prepareText();
        this.renderState.sortElements(ELEMENT_SORT_COMPARATOR);
        this.addElementsToMeshes(GuiRenderState.TraverseRange.BEFORE_BLUR);
        this.firstDrawIndexAfterBlur = this.meshesToDraw.size();
        this.addElementsToMeshes(GuiRenderState.TraverseRange.AFTER_BLUR);
        this.recordDraws();
    }

    private void addElementsToMeshes(GuiRenderState.TraverseRange pTraverseRange) {
        this.previousScissorArea = null;
        this.previousPipeline = null;
        this.previousTextureSetup = null;
        this.bufferBuilder = null;
        this.renderState.forEachElement(this::addElementToMesh, pTraverseRange);
        if (this.bufferBuilder != null) {
            this.recordMesh(this.bufferBuilder, this.previousPipeline, this.previousTextureSetup, this.previousScissorArea);
        }
    }

    private void draw(GpuBufferSlice pBufferSlice) {
        if (!this.draws.isEmpty()) {
            Minecraft minecraft = Minecraft.getInstance();
            Window window = minecraft.getWindow();
            RenderSystem.setProjectionMatrix(
                this.guiProjectionMatrixBuffer.getBuffer((float)window.getWidth() / window.getGuiScale(), (float)window.getHeight() / window.getGuiScale()),
                ProjectionType.ORTHOGRAPHIC
            );
            RenderTarget rendertarget = minecraft.getMainRenderTarget();
            int i = 0;

            for (GuiRenderer.Draw guirenderer$draw : this.draws) {
                if (guirenderer$draw.indexCount > i) {
                    i = guirenderer$draw.indexCount;
                }
            }

            RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            GpuBuffer gpubuffer = rendersystem$autostorageindexbuffer.getBuffer(i);
            VertexFormat.IndexType vertexformat$indextype = rendersystem$autostorageindexbuffer.type();
            GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
                .writeTransform(new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f(), 0.0F);
            if (this.firstDrawIndexAfterBlur > 0) {
                this.executeDrawRange(
                    () -> "GUI before blur",
                    rendertarget,
                    pBufferSlice,
                    gpubufferslice,
                    gpubuffer,
                    vertexformat$indextype,
                    0,
                    Math.min(this.firstDrawIndexAfterBlur, this.draws.size())
                );
            }

            if (this.draws.size() > this.firstDrawIndexAfterBlur) {
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(rendertarget.getDepthTexture(), 1.0);
                minecraft.gameRenderer.processBlurEffect();
                this.executeDrawRange(
                    () -> "GUI after blur", rendertarget, pBufferSlice, gpubufferslice, gpubuffer, vertexformat$indextype, this.firstDrawIndexAfterBlur, this.draws.size()
                );
            }
        }
    }

    private void executeDrawRange(
        Supplier<String> pDebugGroup,
        RenderTarget pRenderTarget,
        GpuBufferSlice pFog,
        GpuBufferSlice pDynamicTransforms,
        GpuBuffer pBuffer,
        VertexFormat.IndexType pIndexType,
        int pStart,
        int pEnd
    ) {
        try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                    pDebugGroup, pRenderTarget.getColorTextureView(), OptionalInt.empty(), pRenderTarget.useDepth ? pRenderTarget.getDepthTextureView() : null, OptionalDouble.empty()
                )) {
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.setUniform("Fog", pFog);
            renderpass.setUniform("DynamicTransforms", pDynamicTransforms);

            for (int i = pStart; i < pEnd; i++) {
                GuiRenderer.Draw guirenderer$draw = this.draws.get(i);
                this.executeDraw(guirenderer$draw, renderpass, pBuffer, pIndexType);
            }
        }
    }

    private void addElementToMesh(GuiElementRenderState pRenderState, int pLayer) {
        RenderPipeline renderpipeline = pRenderState.pipeline();
        TextureSetup texturesetup = pRenderState.textureSetup();
        ScreenRectangle screenrectangle = pRenderState.scissorArea();
        if (renderpipeline != this.previousPipeline || this.scissorChanged(screenrectangle, this.previousScissorArea) || !texturesetup.equals(this.previousTextureSetup)) {
            if (this.bufferBuilder != null) {
                this.recordMesh(this.bufferBuilder, this.previousPipeline, this.previousTextureSetup, this.previousScissorArea);
            }

            this.bufferBuilder = this.getBufferBuilder(renderpipeline);
            this.previousPipeline = renderpipeline;
            this.previousTextureSetup = texturesetup;
            this.previousScissorArea = screenrectangle;
        }

        pRenderState.buildVertices(this.bufferBuilder, 0.0F + pLayer);
    }

    private void prepareText() {
        this.renderState.forEachText(p_409515_ -> {
            final Matrix3x2f matrix3x2f = p_409515_.pose;
            final ScreenRectangle screenrectangle = p_409515_.scissor;
            p_409515_.ensurePrepared().visit(new Font.GlyphVisitor() {
                @Override
                public void acceptGlyph(BakedGlyph.GlyphInstance p_410146_) {
                    if (p_410146_.glyph().textureView() != null) {
                        GuiRenderer.this.renderState.submitGlyphToCurrentLayer(new GlyphRenderState(matrix3x2f, p_410146_, screenrectangle));
                    }
                }

                @Override
                public void acceptEffect(BakedGlyph p_406242_, BakedGlyph.Effect p_406324_) {
                    if (p_406242_.textureView() != null) {
                        GuiRenderer.this.renderState.submitGlyphToCurrentLayer(new GlyphEffectRenderState(matrix3x2f, p_406242_, p_406324_, screenrectangle));
                    }
                }
            });
        });
    }

    private void prepareItemElements() {
        if (!this.renderState.getItemModelIdentities().isEmpty()) {
            int i = this.getGuiScaleInvalidatingItemAtlasIfChanged();
            int j = 16 * i;
            int k = this.calculateAtlasSizeInPixels(j);
            if (this.itemsAtlas == null) {
                this.createAtlasTextures(k);
            }

            RenderSystem.outputColorTextureOverride = this.itemsAtlasView;
            RenderSystem.outputDepthTextureOverride = this.itemsAtlasDepthView;
            RenderSystem.setProjectionMatrix(this.itemsProjectionMatrixBuffer.getBuffer(k, k), ProjectionType.ORTHOGRAPHIC);
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
            PoseStack posestack = new PoseStack();
            MutableBoolean mutableboolean = new MutableBoolean(false);
            MutableBoolean mutableboolean1 = new MutableBoolean(false);
            this.renderState
                .forEachItem(
                    p_408308_ -> {
                        if (p_408308_.oversizedItemBounds() != null) {
                            mutableboolean1.setTrue();
                        } else {
                            TrackingItemStackRenderState trackingitemstackrenderstate = p_408308_.itemStackRenderState();
                            GuiRenderer.AtlasPosition guirenderer$atlasposition = this.atlasPositions.get(trackingitemstackrenderstate.getModelIdentity());
                            if (guirenderer$atlasposition == null
                                || trackingitemstackrenderstate.isAnimated() && guirenderer$atlasposition.lastAnimatedOnFrame != this.frameNumber) {
                                if (this.itemAtlasX + j > k) {
                                    this.itemAtlasX = 0;
                                    this.itemAtlasY += j;
                                }

                                boolean flag = trackingitemstackrenderstate.isAnimated() && guirenderer$atlasposition != null;
                                if (!flag && this.itemAtlasY + j > k) {
                                    if (mutableboolean.isFalse()) {
                                        LOGGER.warn("Trying to render too many items in GUI at the same time. Skipping some of them.");
                                        mutableboolean.setTrue();
                                    }
                                } else {
                                    int l = flag ? guirenderer$atlasposition.x : this.itemAtlasX;
                                    int i1 = flag ? guirenderer$atlasposition.y : this.itemAtlasY;
                                    if (flag) {
                                        RenderSystem.getDevice()
                                            .createCommandEncoder()
                                            .clearColorAndDepthTextures(this.itemsAtlas, 0, this.itemsAtlasDepth, 1.0, l, k - i1 - j, j, j);
                                    }

                                    this.renderItemToAtlas(trackingitemstackrenderstate, posestack, l, i1, j);
                                    float f = (float)l / k;
                                    float f1 = (float)(k - i1) / k;
                                    this.submitBlitFromItemAtlas(p_408308_, f, f1, j, k);
                                    if (flag) {
                                        guirenderer$atlasposition.lastAnimatedOnFrame = this.frameNumber;
                                    } else {
                                        this.atlasPositions
                                            .put(
                                                p_408308_.itemStackRenderState().getModelIdentity(),
                                                new GuiRenderer.AtlasPosition(this.itemAtlasX, this.itemAtlasY, f, f1, this.frameNumber)
                                            );
                                        this.itemAtlasX += j;
                                    }
                                }
                            } else {
                                this.submitBlitFromItemAtlas(p_408308_, guirenderer$atlasposition.u, guirenderer$atlasposition.v, j, k);
                            }
                        }
                    }
                );
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            if (mutableboolean1.getValue()) {
                this.renderState
                    .forEachItem(
                        p_408200_ -> {
                            if (p_408200_.oversizedItemBounds() != null) {
                                TrackingItemStackRenderState trackingitemstackrenderstate = p_408200_.itemStackRenderState();
                                OversizedItemRenderer oversizeditemrenderer = this.oversizedItemRenderers
                                    .computeIfAbsent(trackingitemstackrenderstate.getModelIdentity(), p_409833_ -> new OversizedItemRenderer(this.bufferSource));
                                ScreenRectangle screenrectangle = p_408200_.oversizedItemBounds();
                                OversizedItemRenderState oversizeditemrenderstate = new OversizedItemRenderState(
                                    p_408200_,
                                    screenrectangle.left(),
                                    screenrectangle.top(),
                                    screenrectangle.right(),
                                    screenrectangle.bottom()
                                );
                                oversizeditemrenderer.prepare(oversizeditemrenderstate, this.renderState, i);
                            }
                        }
                    );
            }
        }
    }

    private void preparePictureInPicture() {
        int i = Minecraft.getInstance().getWindow().getGuiScale();
        this.renderState.forEachPictureInPicture(p_410704_ -> this.preparePictureInPictureState(p_410704_, i));
    }

    private <T extends PictureInPictureRenderState> void preparePictureInPictureState(T pState, int pGuiScale) {
        PictureInPictureRenderer<T> pictureinpicturerenderer = (PictureInPictureRenderer<T>)this.pictureInPictureRenderers.get(pState.getClass());
        if (pictureinpicturerenderer != null) {
            pictureinpicturerenderer.prepare(pState, this.renderState, pGuiScale);
        }
    }

    private void renderItemToAtlas(TrackingItemStackRenderState pRenderState, PoseStack pPoseStack, int pX, int pY, int pSize) {
        pPoseStack.pushPose();
        pPoseStack.translate(pX + pSize / 2.0F, pY + pSize / 2.0F, 0.0F);
        pPoseStack.scale(pSize, -pSize, pSize);
        boolean flag = !pRenderState.usesBlockLight();
        if (flag) {
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        } else {
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        }

        RenderSystem.enableScissorForRenderTypeDraws(pX, this.itemsAtlas.getHeight(0) - pY - pSize, pSize, pSize);
        pRenderState.render(pPoseStack, this.bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
        this.bufferSource.endBatch();
        RenderSystem.disableScissorForRenderTypeDraws();
        pPoseStack.popPose();
    }

    private void submitBlitFromItemAtlas(GuiItemRenderState pRenderState, float pX, float pY, int pItemSize, int pAtlasSize) {
        float f = pX + (float)pItemSize / pAtlasSize;
        float f1 = pY + (float)(-pItemSize) / pAtlasSize;
        this.renderState
            .submitBlitToCurrentLayer(
                new BlitRenderState(
                    RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                    TextureSetup.singleTexture(this.itemsAtlasView),
                    pRenderState.pose(),
                    pRenderState.x(),
                    pRenderState.y(),
                    pRenderState.x() + 16,
                    pRenderState.y() + 16,
                    pX,
                    f,
                    pY,
                    f1,
                    -1,
                    pRenderState.scissorArea(),
                    null
                )
            );
    }

    private void createAtlasTextures(int pAtlasSize) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.itemsAtlas = gpudevice.createTexture("UI items atlas", 12, TextureFormat.RGBA8, pAtlasSize, pAtlasSize, 1, 1);
        this.itemsAtlas.setTextureFilter(FilterMode.NEAREST, false);
        this.itemsAtlasView = gpudevice.createTextureView(this.itemsAtlas);
        this.itemsAtlasDepth = gpudevice.createTexture("UI items atlas depth", 8, TextureFormat.DEPTH32, pAtlasSize, pAtlasSize, 1, 1);
        this.itemsAtlasDepthView = gpudevice.createTextureView(this.itemsAtlasDepth);
        gpudevice.createCommandEncoder().clearColorAndDepthTextures(this.itemsAtlas, 0, this.itemsAtlasDepth, 1.0);
    }

    private int calculateAtlasSizeInPixels(int pItemWidth) {
        Set<Object> set = this.renderState.getItemModelIdentities();
        int i;
        if (this.atlasPositions.isEmpty()) {
            i = set.size();
        } else {
            i = this.atlasPositions.size();

            for (Object object : set) {
                if (!this.atlasPositions.containsKey(object)) {
                    i++;
                }
            }
        }

        if (this.itemsAtlas != null) {
            int j = this.itemsAtlas.getWidth(0) / pItemWidth;
            int l = j * j;
            if (i < l) {
                return this.itemsAtlas.getWidth(0);
            }

            this.invalidateItemAtlas();
        }

        int k = set.size();
        int i1 = Mth.smallestSquareSide(k + k / 2);
        return Math.clamp((long)Mth.smallestEncompassingPowerOfTwo(i1 * pItemWidth), 512, MAXIMUM_ITEM_ATLAS_SIZE);
    }

    private int getGuiScaleInvalidatingItemAtlasIfChanged() {
        int i = Minecraft.getInstance().getWindow().getGuiScale();
        if (i != this.cachedGuiScale) {
            this.invalidateItemAtlas();

            for (OversizedItemRenderer oversizeditemrenderer : this.oversizedItemRenderers.values()) {
                oversizeditemrenderer.invalidateTexture();
            }

            this.cachedGuiScale = i;
        }

        return i;
    }

    private void invalidateItemAtlas() {
        this.itemAtlasX = 0;
        this.itemAtlasY = 0;
        this.atlasPositions.clear();
        if (this.itemsAtlas != null) {
            this.itemsAtlas.close();
            this.itemsAtlas = null;
        }

        if (this.itemsAtlasView != null) {
            this.itemsAtlasView.close();
            this.itemsAtlasView = null;
        }

        if (this.itemsAtlasDepth != null) {
            this.itemsAtlasDepth.close();
            this.itemsAtlasDepth = null;
        }

        if (this.itemsAtlasDepthView != null) {
            this.itemsAtlasDepthView.close();
            this.itemsAtlasDepthView = null;
        }
    }

    private void recordMesh(BufferBuilder pBufferBuilder, RenderPipeline pPipeline, TextureSetup pTextureSetup, @Nullable ScreenRectangle pScissorArea) {
        MeshData meshdata = pBufferBuilder.buildOrThrow();
        this.meshesToDraw.add(new GuiRenderer.MeshToDraw(meshdata, pPipeline, pTextureSetup, pScissorArea));
    }

    private void recordDraws() {
        this.ensureVertexBufferSizes();
        CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
        Object2IntMap<VertexFormat> object2intmap = new Object2IntOpenHashMap<>();

        for (GuiRenderer.MeshToDraw guirenderer$meshtodraw : this.meshesToDraw) {
            MeshData meshdata = guirenderer$meshtodraw.mesh;
            MeshData.DrawState meshdata$drawstate = meshdata.drawState();
            VertexFormat vertexformat = meshdata$drawstate.format();
            MappableRingBuffer mappableringbuffer = this.vertexBuffers.get(vertexformat);
            if (!object2intmap.containsKey(vertexformat)) {
                object2intmap.put(vertexformat, 0);
            }

            ByteBuffer bytebuffer = meshdata.vertexBuffer();
            int i = bytebuffer.remaining();
            int j = object2intmap.getInt(vertexformat);

            try (GpuBuffer.MappedView gpubuffer$mappedview = commandencoder.mapBuffer(mappableringbuffer.currentBuffer().slice(j, i), false, true)) {
                MemoryUtil.memCopy(bytebuffer, gpubuffer$mappedview.data());
            }

            object2intmap.put(vertexformat, j + i);
            this.draws
                .add(
                    new GuiRenderer.Draw(
                        mappableringbuffer.currentBuffer(),
                        j / vertexformat.getVertexSize(),
                        meshdata$drawstate.mode(),
                        meshdata$drawstate.indexCount(),
                        guirenderer$meshtodraw.pipeline,
                        guirenderer$meshtodraw.textureSetup,
                        guirenderer$meshtodraw.scissorArea
                    )
                );
            guirenderer$meshtodraw.close();
        }
    }

    private void ensureVertexBufferSizes() {
        Object2IntMap<VertexFormat> object2intmap = this.calculatedRequiredVertexBufferSizes();

        for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<VertexFormat> entry : object2intmap.object2IntEntrySet()) {
            VertexFormat vertexformat = entry.getKey();
            int i = entry.getIntValue();
            MappableRingBuffer mappableringbuffer = this.vertexBuffers.get(vertexformat);
            if (mappableringbuffer == null || mappableringbuffer.size() < i) {
                if (mappableringbuffer != null) {
                    mappableringbuffer.close();
                }

                this.vertexBuffers.put(vertexformat, new MappableRingBuffer(() -> "GUI vertex buffer for " + vertexformat, 34, i));
            }
        }
    }

    private Object2IntMap<VertexFormat> calculatedRequiredVertexBufferSizes() {
        Object2IntMap<VertexFormat> object2intmap = new Object2IntOpenHashMap<>();

        for (GuiRenderer.MeshToDraw guirenderer$meshtodraw : this.meshesToDraw) {
            MeshData.DrawState meshdata$drawstate = guirenderer$meshtodraw.mesh.drawState();
            VertexFormat vertexformat = meshdata$drawstate.format();
            if (!object2intmap.containsKey(vertexformat)) {
                object2intmap.put(vertexformat, 0);
            }

            object2intmap.put(vertexformat, object2intmap.getInt(vertexformat) + meshdata$drawstate.vertexCount() * vertexformat.getVertexSize());
        }

        return object2intmap;
    }

    private void executeDraw(GuiRenderer.Draw pDraw, RenderPass pRenderPass, GpuBuffer pBuffer, VertexFormat.IndexType pIndexType) {
        RenderPipeline renderpipeline = pDraw.pipeline();
        pRenderPass.setPipeline(renderpipeline);
        pRenderPass.setVertexBuffer(0, pDraw.vertexBuffer);
        ScreenRectangle screenrectangle = pDraw.scissorArea();
        if (screenrectangle != null) {
            this.enableScissor(screenrectangle, pRenderPass);
        } else {
            pRenderPass.disableScissor();
        }

        if (pDraw.textureSetup.texure0() != null) {
            pRenderPass.bindSampler("Sampler0", pDraw.textureSetup.texure0());
        }

        if (pDraw.textureSetup.texure1() != null) {
            pRenderPass.bindSampler("Sampler1", pDraw.textureSetup.texure1());
        }

        if (pDraw.textureSetup.texure2() != null) {
            pRenderPass.bindSampler("Sampler2", pDraw.textureSetup.texure2());
        }

        pRenderPass.setIndexBuffer(pBuffer, pIndexType);
        pRenderPass.drawIndexed(pDraw.baseVertex, 0, pDraw.indexCount, 1);
    }

    private BufferBuilder getBufferBuilder(RenderPipeline pPipeline) {
        return new BufferBuilder(this.byteBufferBuilder, pPipeline.getVertexFormatMode(), pPipeline.getVertexFormat());
    }

    private boolean scissorChanged(@Nullable ScreenRectangle pScissorArea, @Nullable ScreenRectangle pOldScissorArea) {
        if (pScissorArea == pOldScissorArea) {
            return false;
        } else {
            return pScissorArea != null ? !pScissorArea.equals(pOldScissorArea) : true;
        }
    }

    private void enableScissor(ScreenRectangle pScissorArea, RenderPass pRenderPass) {
        Window window = Minecraft.getInstance().getWindow();
        int i = window.getHeight();
        int j = window.getGuiScale();
        double d0 = pScissorArea.left() * j;
        double d1 = i - pScissorArea.bottom() * j;
        double d2 = pScissorArea.width() * j;
        double d3 = pScissorArea.height() * j;
        pRenderPass.enableScissor((int)d0, (int)d1, Math.max(0, (int)d2), Math.max(0, (int)d3));
    }

    @Override
    public void close() {
        this.byteBufferBuilder.close();
        if (this.itemsAtlas != null) {
            this.itemsAtlas.close();
        }

        if (this.itemsAtlasView != null) {
            this.itemsAtlasView.close();
        }

        if (this.itemsAtlasDepth != null) {
            this.itemsAtlasDepth.close();
        }

        if (this.itemsAtlasDepthView != null) {
            this.itemsAtlasDepthView.close();
        }

        this.pictureInPictureRenderers.values().forEach(PictureInPictureRenderer::close);
        this.guiProjectionMatrixBuffer.close();
        this.itemsProjectionMatrixBuffer.close();

        for (MappableRingBuffer mappableringbuffer : this.vertexBuffers.values()) {
            mappableringbuffer.close();
        }

        this.oversizedItemRenderers.values().forEach(PictureInPictureRenderer::close);
    }

    @OnlyIn(Dist.CLIENT)
    static final class AtlasPosition {
        final int x;
        final int y;
        final float u;
        final float v;
        int lastAnimatedOnFrame;

        AtlasPosition(int pX, int pY, float pU, float pV, int pLastAnimatedOnFrame) {
            this.x = pX;
            this.y = pY;
            this.u = pU;
            this.v = pV;
            this.lastAnimatedOnFrame = pLastAnimatedOnFrame;
        }
    }

    @OnlyIn(Dist.CLIENT)
    record Draw(
        GpuBuffer vertexBuffer,
        int baseVertex,
        VertexFormat.Mode mode,
        int indexCount,
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        @Nullable ScreenRectangle scissorArea
    ) {
    }

    @OnlyIn(Dist.CLIENT)
    record MeshToDraw(MeshData mesh, RenderPipeline pipeline, TextureSetup textureSetup, @Nullable ScreenRectangle scissorArea) implements AutoCloseable {
        @Override
        public void close() {
            this.mesh.close();
        }
    }
}
