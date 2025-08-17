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
public record ColoredRectangleRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    int x0,
    int y0,
    int x1,
    int y1,
    int col1,
    int col2,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public ColoredRectangleRenderState(
        RenderPipeline pPipeline,
        TextureSetup pTextureSetup,
        Matrix3x2f pPose,
        int pX0,
        int pY0,
        int pX1,
        int pY1,
        int pCol1,
        int pCol2,
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
            pCol1,
            pCol2,
            pScissorArea,
            getBounds(pX0, pY0, pX1, pY1, pPose, pScissorArea)
        );
    }

    @Override
    public void buildVertices(VertexConsumer p_409842_, float p_408355_) {
        p_409842_.addVertexWith2DPose(this.pose(), this.x0(), this.y0(), p_408355_).setColor(this.col1());
        p_409842_.addVertexWith2DPose(this.pose(), this.x0(), this.y1(), p_408355_).setColor(this.col2());
        p_409842_.addVertexWith2DPose(this.pose(), this.x1(), this.y1(), p_408355_).setColor(this.col2());
        p_409842_.addVertexWith2DPose(this.pose(), this.x1(), this.y0(), p_408355_).setColor(this.col1());
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