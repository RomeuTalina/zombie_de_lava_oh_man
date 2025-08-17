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
        Model p_410560_, WoodType p_409628_, int p_408801_, int p_410057_, int p_408779_, int p_406621_, float p_409611_, @Nullable ScreenRectangle p_409510_
    ) {
        this(
            p_410560_,
            p_409628_,
            p_408801_,
            p_410057_,
            p_408779_,
            p_406621_,
            p_409611_,
            p_409510_,
            PictureInPictureRenderState.getBounds(p_408801_, p_410057_, p_408779_, p_406621_, p_409510_)
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