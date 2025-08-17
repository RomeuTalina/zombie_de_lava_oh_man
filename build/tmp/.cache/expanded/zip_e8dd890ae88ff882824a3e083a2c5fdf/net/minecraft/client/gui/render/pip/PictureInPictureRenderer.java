package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class PictureInPictureRenderer<T extends PictureInPictureRenderState> implements AutoCloseable {
    protected final MultiBufferSource.BufferSource bufferSource;
    @Nullable
    private GpuTexture texture;
    @Nullable
    private GpuTextureView textureView;
    @Nullable
    private GpuTexture depthTexture;
    @Nullable
    private GpuTextureView depthTextureView;
    private final CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer = new CachedOrthoProjectionMatrixBuffer(
        "PIP - " + this.getClass().getSimpleName(), -1000.0F, 1000.0F, true
    );

    protected PictureInPictureRenderer(MultiBufferSource.BufferSource pBufferSource) {
        this.bufferSource = pBufferSource;
    }

    public void prepare(T pRenderState, GuiRenderState pGuiRenderState, int pGuiScale) {
        int i = (pRenderState.x1() - pRenderState.x0()) * pGuiScale;
        int j = (pRenderState.y1() - pRenderState.y0()) * pGuiScale;
        boolean flag = this.texture == null || this.texture.getWidth(0) != i || this.texture.getHeight(0) != j;
        if (!flag && this.textureIsReadyToBlit(pRenderState)) {
            this.blitTexture(pRenderState, pGuiRenderState);
        } else {
            this.prepareTexturesAndProjection(flag, i, j);
            RenderSystem.outputColorTextureOverride = this.textureView;
            RenderSystem.outputDepthTextureOverride = this.depthTextureView;
            PoseStack posestack = new PoseStack();
            posestack.translate(i / 2.0F, this.getTranslateY(j, pGuiScale), 0.0F);
            float f = pGuiScale * pRenderState.scale();
            posestack.scale(f, f, -f);
            this.renderToTexture(pRenderState, posestack);
            this.bufferSource.endBatch();
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            this.blitTexture(pRenderState, pGuiRenderState);
        }
    }

    protected void blitTexture(T pRenderState, GuiRenderState pGuiRenderState) {
        pGuiRenderState.submitBlitToCurrentLayer(
            new BlitRenderState(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                TextureSetup.singleTexture(this.textureView),
                pRenderState.pose(),
                pRenderState.x0(),
                pRenderState.y0(),
                pRenderState.x1(),
                pRenderState.y1(),
                0.0F,
                1.0F,
                1.0F,
                0.0F,
                -1,
                pRenderState.scissorArea(),
                null
            )
        );
    }

    private void prepareTexturesAndProjection(boolean pResetTexture, int pWidth, int pHeight) {
        if (this.texture != null && pResetTexture) {
            this.texture.close();
            this.texture = null;
            this.textureView.close();
            this.textureView = null;
            this.depthTexture.close();
            this.depthTexture = null;
            this.depthTextureView.close();
            this.depthTextureView = null;
        }

        GpuDevice gpudevice = RenderSystem.getDevice();
        if (this.texture == null) {
            this.texture = gpudevice.createTexture(() -> "UI " + this.getTextureLabel() + " texture", 12, TextureFormat.RGBA8, pWidth, pHeight, 1, 1);
            this.texture.setTextureFilter(FilterMode.NEAREST, false);
            this.textureView = gpudevice.createTextureView(this.texture);
            this.depthTexture = gpudevice.createTexture(() -> "UI " + this.getTextureLabel() + " depth texture", 8, TextureFormat.DEPTH32, pWidth, pHeight, 1, 1);
            this.depthTextureView = gpudevice.createTextureView(this.depthTexture);
        }

        gpudevice.createCommandEncoder().clearColorAndDepthTextures(this.texture, 0, this.depthTexture, 1.0);
        RenderSystem.setProjectionMatrix(this.projectionMatrixBuffer.getBuffer(pWidth, pHeight), ProjectionType.ORTHOGRAPHIC);
    }

    protected boolean textureIsReadyToBlit(T pRenderState) {
        return false;
    }

    protected float getTranslateY(int pHeight, int pGuiScale) {
        return pHeight;
    }

    @Override
    public void close() {
        if (this.texture != null) {
            this.texture.close();
        }

        if (this.textureView != null) {
            this.textureView.close();
        }

        if (this.depthTexture != null) {
            this.depthTexture.close();
        }

        if (this.depthTextureView != null) {
            this.depthTextureView.close();
        }

        this.projectionMatrixBuffer.close();
    }

    public abstract Class<T> getRenderStateClass();

    protected abstract void renderToTexture(T pRenderState, PoseStack pPoseStack);

    protected abstract String getTextureLabel();
}