package net.minecraft.client.gui.render.state.pip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public record GuiEntityRenderState(
    EntityRenderState renderState,
    Vector3f translation,
    Quaternionf rotation,
    @Nullable Quaternionf overrideCameraAngle,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiEntityRenderState(
        EntityRenderState p_410239_,
        Vector3f p_408714_,
        Quaternionf p_408702_,
        @Nullable Quaternionf p_405966_,
        int p_406305_,
        int p_407621_,
        int p_405991_,
        int p_406970_,
        float p_409071_,
        @Nullable ScreenRectangle p_406124_
    ) {
        this(
            p_410239_,
            p_408714_,
            p_408702_,
            p_405966_,
            p_406305_,
            p_407621_,
            p_405991_,
            p_406970_,
            p_409071_,
            p_406124_,
            PictureInPictureRenderState.getBounds(p_406305_, p_407621_, p_405991_, p_406970_, p_406124_)
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