package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.IntConsumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class RenderSystem {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final int MINIMUM_ATLAS_TEXTURE_SIZE = 1024;
    public static final int PROJECTION_MATRIX_UBO_SIZE = new Std140SizeCalculator().putMat4f().get();
    @Nullable
    private static Thread renderThread;
    @Nullable
    private static GpuDevice DEVICE;
    private static double lastDrawTime = Double.MIN_VALUE;
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequential = new RenderSystem.AutoStorageIndexBuffer(1, 1, IntConsumer::accept);
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialQuad = new RenderSystem.AutoStorageIndexBuffer(4, 6, (p_389087_, p_389088_) -> {
        p_389087_.accept(p_389088_);
        p_389087_.accept(p_389088_ + 1);
        p_389087_.accept(p_389088_ + 2);
        p_389087_.accept(p_389088_ + 2);
        p_389087_.accept(p_389088_ + 3);
        p_389087_.accept(p_389088_);
    });
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialLines = new RenderSystem.AutoStorageIndexBuffer(4, 6, (p_389089_, p_389090_) -> {
        p_389089_.accept(p_389090_);
        p_389089_.accept(p_389090_ + 1);
        p_389089_.accept(p_389090_ + 2);
        p_389089_.accept(p_389090_ + 3);
        p_389089_.accept(p_389090_ + 2);
        p_389089_.accept(p_389090_ + 1);
    });
    private static ProjectionType projectionType = ProjectionType.PERSPECTIVE;
    private static ProjectionType savedProjectionType = ProjectionType.PERSPECTIVE;
    private static final Matrix4fStack modelViewStack = new Matrix4fStack(16);
    private static Matrix4f textureMatrix = new Matrix4f();
    public static final int TEXTURE_COUNT = 12;
    private static final GpuTextureView[] shaderTextures = new GpuTextureView[12];
    @Nullable
    private static GpuBufferSlice shaderFog = null;
    @Nullable
    private static GpuBufferSlice shaderLightDirections;
    @Nullable
    private static GpuBufferSlice projectionMatrixBuffer;
    @Nullable
    private static GpuBufferSlice savedProjectionMatrixBuffer;
    private static final Vector3f modelOffset = new Vector3f();
    private static float shaderLineWidth = 1.0F;
    private static String apiDescription = "Unknown";
    private static final AtomicLong pollEventsWaitStart = new AtomicLong();
    private static final AtomicBoolean pollingEvents = new AtomicBoolean(false);
    @Nullable
    private static GpuBuffer QUAD_VERTEX_BUFFER;
    private static final ArrayListDeque<RenderSystem.GpuAsyncTask> PENDING_FENCES = new ArrayListDeque<>();
    @Nullable
    public static GpuTextureView outputColorTextureOverride;
    @Nullable
    public static GpuTextureView outputDepthTextureOverride;
    @Nullable
    private static GpuBuffer globalSettingsUniform;
    @Nullable
    private static DynamicUniforms dynamicUniforms;
    private static ScissorState scissorStateForRenderTypeDraws = new ScissorState();

    public static void initRenderThread() {
        if (renderThread != null) {
            throw new IllegalStateException("Could not initialize render thread");
        } else {
            renderThread = Thread.currentThread();
        }
    }

    public static boolean isOnRenderThread() {
        return Thread.currentThread() == renderThread;
    }

    public static void assertOnRenderThread() {
        if (!isOnRenderThread()) {
            throw constructThreadException();
        }
    }

    private static IllegalStateException constructThreadException() {
        return new IllegalStateException("Rendersystem called from wrong thread");
    }

    private static void pollEvents() {
        pollEventsWaitStart.set(Util.getMillis());
        pollingEvents.set(true);
        GLFW.glfwPollEvents();
        pollingEvents.set(false);
    }

    public static boolean isFrozenAtPollEvents() {
        return pollingEvents.get() && Util.getMillis() - pollEventsWaitStart.get() > 200L;
    }

    public static void flipFrame(long pWindow, @Nullable TracyFrameCapture pTracyFrameCapture) {
        pollEvents();
        Tesselator.getInstance().clear();
        GLFW.glfwSwapBuffers(pWindow);
        if (pTracyFrameCapture != null) {
            pTracyFrameCapture.endFrame();
        }

        dynamicUniforms.reset();
        Minecraft.getInstance().levelRenderer.endFrame();
        pollEvents();
    }

    public static void limitDisplayFPS(int pFrameRateLimit) {
        double d0 = lastDrawTime + 1.0 / pFrameRateLimit;

        double d1;
        for (d1 = GLFW.glfwGetTime(); d1 < d0; d1 = GLFW.glfwGetTime()) {
            GLFW.glfwWaitEventsTimeout(d0 - d1);
        }

        lastDrawTime = d1;
    }

    public static void setShaderFog(GpuBufferSlice pShaderFog) {
        shaderFog = pShaderFog;
    }

    @Nullable
    public static GpuBufferSlice getShaderFog() {
        return shaderFog;
    }

    public static void setShaderLights(GpuBufferSlice pShaderLights) {
        shaderLightDirections = pShaderLights;
    }

    @Nullable
    public static GpuBufferSlice getShaderLights() {
        return shaderLightDirections;
    }

    public static void lineWidth(float pShaderLineWidth) {
        assertOnRenderThread();
        shaderLineWidth = pShaderLineWidth;
    }

    public static float getShaderLineWidth() {
        assertOnRenderThread();
        return shaderLineWidth;
    }

    public static void enableScissorForRenderTypeDraws(int pX, int pY, int pWidth, int pHeight) {
        scissorStateForRenderTypeDraws.enable(pX, pY, pWidth, pHeight);
    }

    public static void disableScissorForRenderTypeDraws() {
        scissorStateForRenderTypeDraws.disable();
    }

    public static ScissorState getScissorStateForRenderTypeDraws() {
        return scissorStateForRenderTypeDraws;
    }

    public static String getBackendDescription() {
        return String.format(Locale.ROOT, "LWJGL version %s", GLX._getLWJGLVersion());
    }

    public static String getApiDescription() {
        return apiDescription;
    }

    public static TimeSource.NanoTimeSource initBackendSystem() {
        return GLX._initGlfw()::getAsLong;
    }

    public static void initRenderer(
        long pWindow, int pGlDebugVerbosity, boolean pSynchronous, BiFunction<ResourceLocation, ShaderType, String> pDefaultShaderSource, boolean pRenderDebugLabels
    ) {
        DEVICE = new GlDevice(pWindow, pGlDebugVerbosity, pSynchronous, pDefaultShaderSource, pRenderDebugLabels);
        apiDescription = getDevice().getImplementationInformation();
        dynamicUniforms = new DynamicUniforms();

        try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION.getVertexSize() * 4)) {
            BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);
            bufferbuilder.addVertex(1.0F, 0.0F, 0.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, 0.0F);
            bufferbuilder.addVertex(0.0F, 1.0F, 0.0F);

            try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
                QUAD_VERTEX_BUFFER = getDevice().createBuffer(() -> "Quad", 32, meshdata.vertexBuffer());
            }
        }
    }

    public static void setErrorCallback(GLFWErrorCallbackI pCallback) {
        GLX._setGlfwErrorCallback(pCallback);
    }

    public static void setupDefaultState() {
        modelViewStack.clear();
        textureMatrix.identity();
    }

    public static void setupOverlayColor(@Nullable GpuTextureView pTexture) {
        assertOnRenderThread();
        setShaderTexture(1, pTexture);
    }

    public static void teardownOverlayColor() {
        assertOnRenderThread();
        setShaderTexture(1, null);
    }

    public static void setShaderTexture(int pTextureId, @Nullable GpuTextureView pTexture) {
        assertOnRenderThread();
        if (pTextureId >= 0 && pTextureId < shaderTextures.length) {
            shaderTextures[pTextureId] = pTexture;
        }
    }

    @Nullable
    public static GpuTextureView getShaderTexture(int pId) {
        assertOnRenderThread();
        return pId >= 0 && pId < shaderTextures.length ? shaderTextures[pId] : null;
    }

    public static void setProjectionMatrix(GpuBufferSlice pProjectionMatrixBuffer, ProjectionType pProjectionType) {
        assertOnRenderThread();
        projectionMatrixBuffer = pProjectionMatrixBuffer;
        projectionType = pProjectionType;
    }

    public static void setTextureMatrix(Matrix4f pTextureMatrix) {
        assertOnRenderThread();
        textureMatrix = new Matrix4f(pTextureMatrix);
    }

    public static void resetTextureMatrix() {
        assertOnRenderThread();
        textureMatrix.identity();
    }

    public static void backupProjectionMatrix() {
        assertOnRenderThread();
        savedProjectionMatrixBuffer = projectionMatrixBuffer;
        savedProjectionType = projectionType;
    }

    public static void restoreProjectionMatrix() {
        assertOnRenderThread();
        projectionMatrixBuffer = savedProjectionMatrixBuffer;
        projectionType = savedProjectionType;
    }

    @Nullable
    public static GpuBufferSlice getProjectionMatrixBuffer() {
        assertOnRenderThread();
        return projectionMatrixBuffer;
    }

    public static Matrix4f getModelViewMatrix() {
        assertOnRenderThread();
        return modelViewStack;
    }

    public static Matrix4fStack getModelViewStack() {
        assertOnRenderThread();
        return modelViewStack;
    }

    public static Matrix4f getTextureMatrix() {
        assertOnRenderThread();
        return textureMatrix;
    }

    public static RenderSystem.AutoStorageIndexBuffer getSequentialBuffer(VertexFormat.Mode pFormatMode) {
        assertOnRenderThread();

        return switch (pFormatMode) {
            case QUADS -> sharedSequentialQuad;
            case LINES -> sharedSequentialLines;
            default -> sharedSequential;
        };
    }

    public static void setGlobalSettingsUniform(GpuBuffer pGlobalSettingsUniform) {
        globalSettingsUniform = pGlobalSettingsUniform;
    }

    @Nullable
    public static GpuBuffer getGlobalSettingsUniform() {
        return globalSettingsUniform;
    }

    public static ProjectionType getProjectionType() {
        assertOnRenderThread();
        return projectionType;
    }

    public static GpuBuffer getQuadVertexBuffer() {
        if (QUAD_VERTEX_BUFFER == null) {
            throw new IllegalStateException("Can't getQuadVertexBuffer() before renderer was initialized");
        } else {
            return QUAD_VERTEX_BUFFER;
        }
    }

    public static void setModelOffset(float pX, float pY, float pZ) {
        assertOnRenderThread();
        modelOffset.set(pX, pY, pZ);
    }

    public static void resetModelOffset() {
        assertOnRenderThread();
        modelOffset.set(0.0F, 0.0F, 0.0F);
    }

    public static Vector3f getModelOffset() {
        assertOnRenderThread();
        return modelOffset;
    }

    public static void queueFencedTask(Runnable pTask) {
        PENDING_FENCES.addLast(new RenderSystem.GpuAsyncTask(pTask, getDevice().createCommandEncoder().createFence()));
    }

    public static void executePendingTasks() {
        for (RenderSystem.GpuAsyncTask rendersystem$gpuasynctask = PENDING_FENCES.peekFirst();
            rendersystem$gpuasynctask != null;
            rendersystem$gpuasynctask = PENDING_FENCES.peekFirst()
        ) {
            if (!rendersystem$gpuasynctask.fence.awaitCompletion(0L)) {
                return;
            }

            try {
                rendersystem$gpuasynctask.callback.run();
            } finally {
                rendersystem$gpuasynctask.fence.close();
            }

            PENDING_FENCES.removeFirst();
        }
    }

    public static GpuDevice getDevice() {
        if (DEVICE == null) {
            throw new IllegalStateException("Can't getDevice() before it was initialized");
        } else {
            return DEVICE;
        }
    }

    @Nullable
    public static GpuDevice tryGetDevice() {
        return DEVICE;
    }

    public static DynamicUniforms getDynamicUniforms() {
        if (dynamicUniforms == null) {
            throw new IllegalStateException("Can't getDynamicUniforms() before device was initialized");
        } else {
            return dynamicUniforms;
        }
    }

    public static void bindDefaultUniforms(RenderPass pRenderPass) {
        GpuBufferSlice gpubufferslice = getProjectionMatrixBuffer();
        if (gpubufferslice != null) {
            pRenderPass.setUniform("Projection", gpubufferslice);
        }

        GpuBufferSlice gpubufferslice1 = getShaderFog();
        if (gpubufferslice1 != null) {
            pRenderPass.setUniform("Fog", gpubufferslice1);
        }

        GpuBuffer gpubuffer = getGlobalSettingsUniform();
        if (gpubuffer != null) {
            pRenderPass.setUniform("Globals", gpubuffer);
        }

        GpuBufferSlice gpubufferslice2 = getShaderLights();
        if (gpubufferslice2 != null) {
            pRenderPass.setUniform("Lighting", gpubufferslice2);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class AutoStorageIndexBuffer {
        private final int vertexStride;
        private final int indexStride;
        private final RenderSystem.AutoStorageIndexBuffer.IndexGenerator generator;
        @Nullable
        private GpuBuffer buffer;
        private VertexFormat.IndexType type = VertexFormat.IndexType.SHORT;
        private int indexCount;

        AutoStorageIndexBuffer(int pVertexStride, int pIndexStride, RenderSystem.AutoStorageIndexBuffer.IndexGenerator pGenerator) {
            this.vertexStride = pVertexStride;
            this.indexStride = pIndexStride;
            this.generator = pGenerator;
        }

        public boolean hasStorage(int pIndex) {
            return pIndex <= this.indexCount;
        }

        public GpuBuffer getBuffer(int pIndex) {
            this.ensureStorage(pIndex);
            return this.buffer;
        }

        private void ensureStorage(int pNeededIndexCount) {
            if (!this.hasStorage(pNeededIndexCount)) {
                pNeededIndexCount = Mth.roundToward(pNeededIndexCount * 2, this.indexStride);
                RenderSystem.LOGGER.debug("Growing IndexBuffer: Old limit {}, new limit {}.", this.indexCount, pNeededIndexCount);
                int i = pNeededIndexCount / this.indexStride;
                int j = i * this.vertexStride;
                VertexFormat.IndexType vertexformat$indextype = VertexFormat.IndexType.least(j);
                int k = Mth.roundToward(pNeededIndexCount * vertexformat$indextype.bytes, 4);
                ByteBuffer bytebuffer = MemoryUtil.memAlloc(k);

                try {
                    this.type = vertexformat$indextype;
                    it.unimi.dsi.fastutil.ints.IntConsumer intconsumer = this.intConsumer(bytebuffer);

                    for (int l = 0; l < pNeededIndexCount; l += this.indexStride) {
                        this.generator.accept(intconsumer, l * this.vertexStride / this.indexStride);
                    }

                    bytebuffer.flip();
                    if (this.buffer != null) {
                        this.buffer.close();
                    }

                    this.buffer = RenderSystem.getDevice().createBuffer(() -> "Auto Storage index buffer", 64, bytebuffer);
                } finally {
                    MemoryUtil.memFree(bytebuffer);
                }

                this.indexCount = pNeededIndexCount;
            }
        }

        private it.unimi.dsi.fastutil.ints.IntConsumer intConsumer(ByteBuffer pBuffer) {
            switch (this.type) {
                case SHORT:
                    return p_157482_ -> pBuffer.putShort((short)p_157482_);
                case INT:
                default:
                    return pBuffer::putInt;
            }
        }

        public VertexFormat.IndexType type() {
            return this.type;
        }

        @OnlyIn(Dist.CLIENT)
        interface IndexGenerator {
            void accept(it.unimi.dsi.fastutil.ints.IntConsumer pConsumer, int pIndex);
        }
    }

    @OnlyIn(Dist.CLIENT)
    record GpuAsyncTask(Runnable callback, GpuFence fence) {
    }
}