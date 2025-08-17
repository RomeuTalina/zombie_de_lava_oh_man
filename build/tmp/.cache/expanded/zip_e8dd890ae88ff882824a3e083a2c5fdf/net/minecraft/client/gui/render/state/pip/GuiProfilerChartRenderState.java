package net.minecraft.client.gui.render.state.pip;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.profiling.ResultField;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GuiProfilerChartRenderState(
    List<ResultField> chartData,
    int x0,
    int y0,
    int x1,
    int y1,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiProfilerChartRenderState(
        List<ResultField> pChartData, int pX0, int pY0, int pX1, int pY1, @Nullable ScreenRectangle pScissorArea
    ) {
        this(
            pChartData,
            pX0,
            pY0,
            pX1,
            pY1,
            pScissorArea,
            PictureInPictureRenderState.getBounds(pX0, pY0, pX1, pY1, pScissorArea)
        );
    }

    @Override
    public float scale() {
        return 1.0F;
    }

    @Override
    public int x0() {
        return this.x0;
    }

    @Override
    public int y0() {
        return this.y0;
    }

    @Override
    public int x1() {
        return this.x1;
    }

    @Override
    public int y1() {
        return this.y1;
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