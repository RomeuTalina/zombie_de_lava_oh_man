package net.minecraft.client.gui.render.state.pip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.Model;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GuiSignRenderState(
    Model signModel,
    WoodType woodType,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiSignRenderState(
        Model pSignModel, WoodType pWoodType, int pX0, int pY0, int pX1, int pY1, float pScale, @Nullable ScreenRectangle pScissorArea
    ) {
        this(
            pSignModel,
            pWoodType,
            pX0,
            pY0,
            pX1,
            pY1,
            pScale,
            pScissorArea,
            PictureInPictureRenderState.getBounds(pX0, pY0, pX1, pY1, pScissorArea)
        );
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

    @Override
    public float scale() {
        return this.scale;
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