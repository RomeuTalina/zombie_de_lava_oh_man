package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;

@OnlyIn(Dist.CLIENT)
public record BlitRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    int x0,
    int y0,
    int x1,
    int y1,
    float u0,
    float u1,
    float v0,
    float v1,
    int color,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public BlitRenderState(
        RenderPipeline pPipeline,
        TextureSetup pTextureSetup,
        Matrix3x2f pPose,
        int pX0,
        int pY0,
        int pX1,
        int pY1,
        float pU0,
        float pU1,
        float pV0,
        float pV1,
        int pColor,
        @Nullable ScreenRectangle pScissorArea
    ) {
        this(
            pPipeline,
            pTextureSetup,
            pPose,
            pX0,
            pY0,
            pX1,
            pY1,
            pU0,
            pU1,
            pV0,
            pV1,
            pColor,
            pScissorArea,
            getBounds(pX0, pY0, pX1, pY1, pPose, pScissorArea)
        );
    }

    @Override
    public void buildVertices(VertexConsumer p_407042_, float p_406900_) {
        p_407042_.addVertexWith2DPose(this.pose(), this.x0(), this.y0(), p_406900_)
            .setUv(this.u0(), this.v0())
            .setColor(this.color());
        p_407042_.addVertexWith2DPose(this.pose(), this.x0(), this.y1(), p_406900_)
            .setUv(this.u0(), this.v1())
            .setColor(this.color());
        p_407042_.addVertexWith2DPose(this.pose(), this.x1(), this.y1(), p_406900_)
            .setUv(this.u1(), this.v1())
            .setColor(this.color());
        p_407042_.addVertexWith2DPose(this.pose(), this.x1(), this.y0(), p_406900_)
            .setUv(this.u1(), this.v0())
            .setColor(this.color());
    }

    @Nullable
    private static ScreenRectangle getBounds(
        int pX0, int pY0, int pX1, int pY1, Matrix3x2f pPose, @Nullable ScreenRectangle pScissorArea
    ) {
        ScreenRectangle screenrectangle = new ScreenRectangle(pX0, pY0, pX1 - pX0, pY1 - pY0).transformMaxBounds(pPose);
        return pScissorArea != null ? pScissorArea.intersection(screenrectangle) : screenrectangle;
    }

    @Override
    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    @Override
    public TextureSetup textureSetup() {
        return this.textureSetup;
    }

    @Nullable
    @Override
    public ScreenRectangle scissorArea() {
        return this.scissorArea;
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        return this.bounds;
    }
}