package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class WorldBorderRenderer {
    public static final ResourceLocation FORCEFIELD_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/forcefield.png");
    private boolean needsRebuild = true;
    private double lastMinX;
    private double lastMinZ;
    private double lastBorderMinX;
    private double lastBorderMaxX;
    private double lastBorderMinZ;
    private double lastBorderMaxZ;
    private final GpuBuffer worldBorderBuffer = RenderSystem.getDevice()
        .createBuffer(() -> "World border vertex buffer", 40, 16 * DefaultVertexFormat.POSITION_TEX.getVertexSize());
    private final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

    private void rebuildWorldBorderBuffer(WorldBorder pWorldBorder, double pRenderDistance, double pCamZ, double pCamX, float pFarPlaneDepth, float pVBottom, float pVTop) {
        try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_TEX.getVertexSize() * 4 * 4)) {
            double d0 = pWorldBorder.getMinX();
            double d1 = pWorldBorder.getMaxX();
            double d2 = pWorldBorder.getMinZ();
            double d3 = pWorldBorder.getMaxZ();
            double d4 = Math.max((double)Mth.floor(pCamZ - pRenderDistance), d2);
            double d5 = Math.min((double)Mth.ceil(pCamZ + pRenderDistance), d3);
            float f = (Mth.floor(d4) & 1) * 0.5F;
            float f1 = (float)(d5 - d4) / 2.0F;
            double d6 = Math.max((double)Mth.floor(pCamX - pRenderDistance), d0);
            double d7 = Math.min((double)Mth.ceil(pCamX + pRenderDistance), d1);
            float f2 = (Mth.floor(d6) & 1) * 0.5F;
            float f3 = (float)(d7 - d6) / 2.0F;
            BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferbuilder.addVertex(0.0F, -pFarPlaneDepth, (float)(d3 - d4)).setUv(f2, pVBottom);
            bufferbuilder.addVertex((float)(d7 - d6), -pFarPlaneDepth, (float)(d3 - d4)).setUv(f3 + f2, pVBottom);
            bufferbuilder.addVertex((float)(d7 - d6), pFarPlaneDepth, (float)(d3 - d4)).setUv(f3 + f2, pVTop);
            bufferbuilder.addVertex(0.0F, pFarPlaneDepth, (float)(d3 - d4)).setUv(f2, pVTop);
            bufferbuilder.addVertex(0.0F, -pFarPlaneDepth, 0.0F).setUv(f, pVBottom);
            bufferbuilder.addVertex(0.0F, -pFarPlaneDepth, (float)(d5 - d4)).setUv(f1 + f, pVBottom);
            bufferbuilder.addVertex(0.0F, pFarPlaneDepth, (float)(d5 - d4)).setUv(f1 + f, pVTop);
            bufferbuilder.addVertex(0.0F, pFarPlaneDepth, 0.0F).setUv(f, pVTop);
            bufferbuilder.addVertex((float)(d7 - d6), -pFarPlaneDepth, 0.0F).setUv(f2, pVBottom);
            bufferbuilder.addVertex(0.0F, -pFarPlaneDepth, 0.0F).setUv(f3 + f2, pVBottom);
            bufferbuilder.addVertex(0.0F, pFarPlaneDepth, 0.0F).setUv(f3 + f2, pVTop);
            bufferbuilder.addVertex((float)(d7 - d6), pFarPlaneDepth, 0.0F).setUv(f2, pVTop);
            bufferbuilder.addVertex((float)(d1 - d6), -pFarPlaneDepth, (float)(d5 - d4)).setUv(f, pVBottom);
            bufferbuilder.addVertex((float)(d1 - d6), -pFarPlaneDepth, 0.0F).setUv(f1 + f, pVBottom);
            bufferbuilder.addVertex((float)(d1 - d6), pFarPlaneDepth, 0.0F).setUv(f1 + f, pVTop);
            bufferbuilder.addVertex((float)(d1 - d6), pFarPlaneDepth, (float)(d5 - d4)).setUv(f, pVTop);

            try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.worldBorderBuffer.slice(), meshdata.vertexBuffer());
            }

            this.lastBorderMinX = d0;
            this.lastBorderMaxX = d1;
            this.lastBorderMinZ = d2;
            this.lastBorderMaxZ = d3;
            this.lastMinX = d6;
            this.lastMinZ = d4;
            this.needsRebuild = false;
        }
    }

    public void render(WorldBorder pWorldBorder, Vec3 pCameraPosition, double pRenderDistance, double pFarPlaneDepth) {
        double d0 = pWorldBorder.getMinX();
        double d1 = pWorldBorder.getMaxX();
        double d2 = pWorldBorder.getMinZ();
        double d3 = pWorldBorder.getMaxZ();
        if ((
                !(pCameraPosition.x < d1 - pRenderDistance)
                    || !(pCameraPosition.x > d0 + pRenderDistance)
                    || !(pCameraPosition.z < d3 - pRenderDistance)
                    || !(pCameraPosition.z > d2 + pRenderDistance)
            )
            && !(pCameraPosition.x < d0 - pRenderDistance)
            && !(pCameraPosition.x > d1 + pRenderDistance)
            && !(pCameraPosition.z < d2 - pRenderDistance)
            && !(pCameraPosition.z > d3 + pRenderDistance)) {
            double d4 = 1.0 - pWorldBorder.getDistanceToBorder(pCameraPosition.x, pCameraPosition.z) / pRenderDistance;
            d4 = Math.pow(d4, 4.0);
            d4 = Mth.clamp(d4, 0.0, 1.0);
            double d5 = pCameraPosition.x;
            double d6 = pCameraPosition.z;
            float f = (float)pFarPlaneDepth;
            int i = pWorldBorder.getStatus().getColor();
            float f1 = ARGB.red(i) / 255.0F;
            float f2 = ARGB.green(i) / 255.0F;
            float f3 = ARGB.blue(i) / 255.0F;
            float f4 = (float)(Util.getMillis() % 3000L) / 3000.0F;
            float f5 = (float)(-Mth.frac(pCameraPosition.y * 0.5));
            float f6 = f5 + f;
            if (this.shouldRebuildWorldBorderBuffer(pWorldBorder)) {
                this.rebuildWorldBorderBuffer(pWorldBorder, pRenderDistance, d6, d5, f, f6, f5);
            }

            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            AbstractTexture abstracttexture = texturemanager.getTexture(FORCEFIELD_LOCATION);
            abstracttexture.setUseMipmaps(false);
            RenderPipeline renderpipeline = RenderPipelines.WORLD_BORDER;
            RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
            RenderTarget rendertarget1 = Minecraft.getInstance().levelRenderer.getWeatherTarget();
            GpuTextureView gputextureview;
            GpuTextureView gputextureview1;
            if (rendertarget1 != null) {
                gputextureview = rendertarget1.getColorTextureView();
                gputextureview1 = rendertarget1.getDepthTextureView();
            } else {
                gputextureview = rendertarget.getColorTextureView();
                gputextureview1 = rendertarget.getDepthTextureView();
            }

            GpuBuffer gpubuffer = this.indices.getBuffer(6);
            GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
                .writeTransform(
                    RenderSystem.getModelViewMatrix(),
                    new Vector4f(f1, f2, f3, (float)d4),
                    new Vector3f((float)(this.lastMinX - d5), (float)(-pCameraPosition.y), (float)(this.lastMinZ - d6)),
                    new Matrix4f().translation(f4, f4, 0.0F),
                    0.0F
                );

            try (RenderPass renderpass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "World border", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
                renderpass.setPipeline(renderpipeline);
                RenderSystem.bindDefaultUniforms(renderpass);
                renderpass.setUniform("DynamicTransforms", gpubufferslice);
                renderpass.setIndexBuffer(gpubuffer, this.indices.type());
                renderpass.bindSampler("Sampler0", abstracttexture.getTextureView());
                renderpass.setVertexBuffer(0, this.worldBorderBuffer);
                ArrayList<RenderPass.Draw<WorldBorderRenderer>> arraylist = new ArrayList<>();

                for (WorldBorder.DistancePerDirection worldborder$distanceperdirection : pWorldBorder.closestBorder(d5, d6)) {
                    if (worldborder$distanceperdirection.distance() < pRenderDistance) {
                        int j = worldborder$distanceperdirection.direction().get2DDataValue();
                        arraylist.add(new RenderPass.Draw<>(0, this.worldBorderBuffer, gpubuffer, this.indices.type(), 6 * j, 6));
                    }
                }

                renderpass.drawMultipleIndexed(arraylist, null, null, Collections.emptyList(), this);
            }
        }
    }

    public void invalidate() {
        this.needsRebuild = true;
    }

    private boolean shouldRebuildWorldBorderBuffer(WorldBorder pWorldBorder) {
        return this.needsRebuild
            || pWorldBorder.getMinX() != this.lastBorderMinX
            || pWorldBorder.getMinZ() != this.lastBorderMinZ
            || pWorldBorder.getMaxX() != this.lastBorderMaxX
            || pWorldBorder.getMaxZ() != this.lastBorderMaxZ;
    }
}