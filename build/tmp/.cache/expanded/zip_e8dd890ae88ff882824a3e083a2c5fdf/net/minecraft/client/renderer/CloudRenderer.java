package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class CloudRenderer extends SimplePreparableReloadListener<Optional<CloudRenderer.TextureData>> implements AutoCloseable {
    private static final int FLAG_INSIDE_FACE = 16;
    private static final int FLAG_USE_TOP_COLOR = 32;
    private static final int MAX_RADIUS_CHUNKS = 128;
    private static final float CELL_SIZE_IN_BLOCKS = 12.0F;
    private static final int UBO_SIZE = new Std140SizeCalculator().putVec4().putVec3().putVec3().get();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");
    private static final float BLOCKS_PER_SECOND = 0.6F;
    private static final long EMPTY_CELL = 0L;
    private static final int COLOR_OFFSET = 4;
    private static final int NORTH_OFFSET = 3;
    private static final int EAST_OFFSET = 2;
    private static final int SOUTH_OFFSET = 1;
    private static final int WEST_OFFSET = 0;
    private boolean needsRebuild = true;
    private int prevCellX = Integer.MIN_VALUE;
    private int prevCellZ = Integer.MIN_VALUE;
    private CloudRenderer.RelativeCameraPos prevRelativeCameraPos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
    @Nullable
    private CloudStatus prevType;
    @Nullable
    private CloudRenderer.TextureData texture;
    private int quadCount = 0;
    private final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
    private final MappableRingBuffer ubo = new MappableRingBuffer(() -> "Cloud UBO", 130, UBO_SIZE);
    @Nullable
    private MappableRingBuffer utb;

    protected Optional<CloudRenderer.TextureData> prepare(ResourceManager p_361257_, ProfilerFiller p_362196_) {
        try {
            Optional optional;
            try (
                InputStream inputstream = p_361257_.open(TEXTURE_LOCATION);
                NativeImage nativeimage = NativeImage.read(inputstream);
            ) {
                int i = nativeimage.getWidth();
                int j = nativeimage.getHeight();
                long[] along = new long[i * j];

                for (int k = 0; k < j; k++) {
                    for (int l = 0; l < i; l++) {
                        int i1 = nativeimage.getPixel(l, k);
                        if (isCellEmpty(i1)) {
                            along[l + k * i] = 0L;
                        } else {
                            boolean flag = isCellEmpty(nativeimage.getPixel(l, Math.floorMod(k - 1, j)));
                            boolean flag1 = isCellEmpty(nativeimage.getPixel(Math.floorMod(l + 1, j), k));
                            boolean flag2 = isCellEmpty(nativeimage.getPixel(l, Math.floorMod(k + 1, j)));
                            boolean flag3 = isCellEmpty(nativeimage.getPixel(Math.floorMod(l - 1, j), k));
                            along[l + k * i] = packCellData(i1, flag, flag1, flag2, flag3);
                        }
                    }
                }

                optional = Optional.of(new CloudRenderer.TextureData(along, i, j));
            }

            return optional;
        } catch (IOException ioexception) {
            LOGGER.error("Failed to load cloud texture", (Throwable)ioexception);
            return Optional.empty();
        }
    }

    private static int getSizeForCloudDistance(int pCloudDistance) {
        int i = 4;
        int j = (pCloudDistance + 1) * 2 * (pCloudDistance + 1) * 2 / 2;
        int k = j * 4 + 54;
        return k * 3;
    }

    protected void apply(Optional<CloudRenderer.TextureData> p_370042_, ResourceManager p_368869_, ProfilerFiller p_367795_) {
        this.texture = p_370042_.orElse(null);
        this.needsRebuild = true;
    }

    private static boolean isCellEmpty(int pColor) {
        return ARGB.alpha(pColor) < 10;
    }

    private static long packCellData(int pColor, boolean pNorthEmpty, boolean pEastEmpty, boolean pSouthEmpty, boolean pWestEmpty) {
        return (long)pColor << 4 | (pNorthEmpty ? 1 : 0) << 3 | (pEastEmpty ? 1 : 0) << 2 | (pSouthEmpty ? 1 : 0) << 1 | (pWestEmpty ? 1 : 0) << 0;
    }

    private static boolean isNorthEmpty(long pCellData) {
        return (pCellData >> 3 & 1L) != 0L;
    }

    private static boolean isEastEmpty(long pCellData) {
        return (pCellData >> 2 & 1L) != 0L;
    }

    private static boolean isSouthEmpty(long pCellData) {
        return (pCellData >> 1 & 1L) != 0L;
    }

    private static boolean isWestEmpty(long pCellData) {
        return (pCellData >> 0 & 1L) != 0L;
    }

    public void render(int pCloudColor, CloudStatus pCloudStatus, float pHeight, Vec3 pCameraPosition, float pTicks) {
        if (this.texture != null) {
            int i = Math.min(Minecraft.getInstance().options.cloudRange().get(), 128) * 16;
            int j = Mth.ceil(i / 12.0F);
            int k = getSizeForCloudDistance(j);
            if (this.utb == null || this.utb.currentBuffer().size() != k) {
                if (this.utb != null) {
                    this.utb.close();
                }

                this.utb = new MappableRingBuffer(() -> "Cloud UTB", 258, k);
            }

            float f = (float)(pHeight - pCameraPosition.y);
            float f1 = f + 4.0F;
            CloudRenderer.RelativeCameraPos cloudrenderer$relativecamerapos;
            if (f1 < 0.0F) {
                cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS;
            } else if (f > 0.0F) {
                cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.BELOW_CLOUDS;
            } else {
                cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
            }

            double d0 = pCameraPosition.x + pTicks * 0.030000001F;
            double d1 = pCameraPosition.z + 3.96F;
            double d2 = this.texture.width * 12.0;
            double d3 = this.texture.height * 12.0;
            d0 -= Mth.floor(d0 / d2) * d2;
            d1 -= Mth.floor(d1 / d3) * d3;
            int l = Mth.floor(d0 / 12.0);
            int i1 = Mth.floor(d1 / 12.0);
            float f2 = (float)(d0 - l * 12.0F);
            float f3 = (float)(d1 - i1 * 12.0F);
            boolean flag = pCloudStatus == CloudStatus.FANCY;
            RenderPipeline renderpipeline = flag ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;
            if (this.needsRebuild
                || l != this.prevCellX
                || i1 != this.prevCellZ
                || cloudrenderer$relativecamerapos != this.prevRelativeCameraPos
                || pCloudStatus != this.prevType) {
                this.needsRebuild = false;
                this.prevCellX = l;
                this.prevCellZ = i1;
                this.prevRelativeCameraPos = cloudrenderer$relativecamerapos;
                this.prevType = pCloudStatus;
                this.utb.rotate();

                try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .mapBuffer(this.utb.currentBuffer(), false, true)) {
                    this.buildMesh(cloudrenderer$relativecamerapos, gpubuffer$mappedview.data(), l, i1, flag, j);
                    this.quadCount = gpubuffer$mappedview.data().position() / 3;
                }
            }

            if (this.quadCount != 0) {
                try (GpuBuffer.MappedView gpubuffer$mappedview1 = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .mapBuffer(this.ubo.currentBuffer(), false, true)) {
                    Std140Builder.intoBuffer(gpubuffer$mappedview1.data())
                        .putVec4(ARGB.redFloat(pCloudColor), ARGB.greenFloat(pCloudColor), ARGB.blueFloat(pCloudColor), 1.0F)
                        .putVec3(-f2, f, -f3)
                        .putVec3(12.0F, 4.0F, 12.0F);
                }

                GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
                    .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f(), 0.0F);
                RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
                RenderTarget rendertarget1 = Minecraft.getInstance().levelRenderer.getCloudsTarget();
                RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                GpuBuffer gpubuffer = rendersystem$autostorageindexbuffer.getBuffer(6 * this.quadCount);
                GpuTextureView gputextureview;
                GpuTextureView gputextureview1;
                if (rendertarget1 != null) {
                    gputextureview = rendertarget1.getColorTextureView();
                    gputextureview1 = rendertarget1.getDepthTextureView();
                } else {
                    gputextureview = rendertarget.getColorTextureView();
                    gputextureview1 = rendertarget.getDepthTextureView();
                }

                try (RenderPass renderpass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "Clouds", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
                    renderpass.setPipeline(renderpipeline);
                    RenderSystem.bindDefaultUniforms(renderpass);
                    renderpass.setUniform("DynamicTransforms", gpubufferslice);
                    renderpass.setIndexBuffer(gpubuffer, rendersystem$autostorageindexbuffer.type());
                    renderpass.setVertexBuffer(0, RenderSystem.getQuadVertexBuffer());
                    renderpass.setUniform("CloudInfo", this.ubo.currentBuffer());
                    renderpass.setUniform("CloudFaces", this.utb.currentBuffer());
                    renderpass.setPipeline(renderpipeline);
                    renderpass.drawIndexed(0, 0, 6 * this.quadCount, 1);
                }
            }
        }
    }

    private void buildMesh(CloudRenderer.RelativeCameraPos pRelativeCameraPos, ByteBuffer pBuffer, int pCellX, int pCellZ, boolean pFancyClouds, int pSize) {
        if (this.texture != null) {
            long[] along = this.texture.cells;
            int i = this.texture.width;
            int j = this.texture.height;

            for (int k = 0; k <= 2 * pSize; k++) {
                for (int l = -k; l <= k; l++) {
                    int i1 = k - Math.abs(l);
                    if (i1 >= 0 && i1 <= pSize && l * l + i1 * i1 <= pSize * pSize) {
                        if (i1 != 0) {
                            this.tryBuildCell(pRelativeCameraPos, pBuffer, pCellX, pCellZ, pFancyClouds, l, i, -i1, j, along);
                        }

                        this.tryBuildCell(pRelativeCameraPos, pBuffer, pCellX, pCellZ, pFancyClouds, l, i, i1, j, along);
                    }
                }
            }
        }
    }

    private void tryBuildCell(
        CloudRenderer.RelativeCameraPos pRelativeCameraPos,
        ByteBuffer pBuffer,
        int pCellX,
        int pCellZ,
        boolean pFancyClouds,
        int pX,
        int pWidth,
        int pZ,
        int pHeight,
        long[] pCells
    ) {
        int i = Math.floorMod(pCellX + pX, pWidth);
        int j = Math.floorMod(pCellZ + pZ, pHeight);
        long k = pCells[i + j * pWidth];
        if (k != 0L) {
            if (pFancyClouds) {
                this.buildExtrudedCell(pRelativeCameraPos, pBuffer, pX, pZ, k);
            } else {
                this.buildFlatCell(pBuffer, pX, pZ);
            }
        }
    }

    private void buildFlatCell(ByteBuffer pBuffer, int pCellX, int pCellZ) {
        this.encodeFace(pBuffer, pCellX, pCellZ, Direction.DOWN, 32);
    }

    private void encodeFace(ByteBuffer pBuffer, int pCellX, int pCellZ, Direction pFace, int pOffset) {
        int i = pFace.get3DDataValue() | pOffset;
        i |= (pCellX & 1) << 7;
        i |= (pCellZ & 1) << 6;
        pBuffer.put((byte)(pCellX >> 1)).put((byte)(pCellZ >> 1)).put((byte)i);
    }

    private void buildExtrudedCell(CloudRenderer.RelativeCameraPos pRelativeCameraPos, ByteBuffer pBuffer, int pCellX, int pCellZ, long pCellData) {
        if (pRelativeCameraPos != CloudRenderer.RelativeCameraPos.BELOW_CLOUDS) {
            this.encodeFace(pBuffer, pCellX, pCellZ, Direction.UP, 0);
        }

        if (pRelativeCameraPos != CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS) {
            this.encodeFace(pBuffer, pCellX, pCellZ, Direction.DOWN, 0);
        }

        if (isNorthEmpty(pCellData) && pCellZ > 0) {
            this.encodeFace(pBuffer, pCellX, pCellZ, Direction.NORTH, 0);
        }

        if (isSouthEmpty(pCellData) && pCellZ < 0) {
            this.encodeFace(pBuffer, pCellX, pCellZ, Direction.SOUTH, 0);
        }

        if (isWestEmpty(pCellData) && pCellX > 0) {
            this.encodeFace(pBuffer, pCellX, pCellZ, Direction.WEST, 0);
        }

        if (isEastEmpty(pCellData) && pCellX < 0) {
            this.encodeFace(pBuffer, pCellX, pCellZ, Direction.EAST, 0);
        }

        boolean flag = Math.abs(pCellX) <= 1 && Math.abs(pCellZ) <= 1;
        if (flag) {
            for (Direction direction : Direction.values()) {
                this.encodeFace(pBuffer, pCellX, pCellZ, direction, 16);
            }
        }
    }

    public void markForRebuild() {
        this.needsRebuild = true;
    }

    public void endFrame() {
        this.ubo.rotate();
    }

    @Override
    public void close() {
        this.ubo.close();
        if (this.utb != null) {
            this.utb.close();
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum RelativeCameraPos {
        ABOVE_CLOUDS,
        INSIDE_CLOUDS,
        BELOW_CLOUDS;
    }

    @OnlyIn(Dist.CLIENT)
    public record TextureData(long[] cells, int width, int height) {
    }
}