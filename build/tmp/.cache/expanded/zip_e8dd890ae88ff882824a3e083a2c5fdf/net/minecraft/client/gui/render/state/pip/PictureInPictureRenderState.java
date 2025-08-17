package net.minecraft.client.gui.render.state.pip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.ScreenArea;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;

@OnlyIn(Dist.CLIENT)
public interface PictureInPictureRenderState extends ScreenArea {
    Matrix3x2f IDENTITY_POSE = new Matrix3x2f();

    int x0();

    int x1();

    int y0();

    int y1();

    float scale();

    default Matrix3x2f pose() {
        return IDENTITY_POSE;
    }

    @Nullable
    ScreenRectangle scissorArea();

    @Nullable
    static ScreenRectangle getBounds(int pX0, int pY0, int pX1, int pY1, @Nullable ScreenRectangle pScissorArea) {
        ScreenRectangle screenrectangle = new ScreenRectangle(pX0, pY0, pX1 - pX0, pY1 - pY0);
        return pScissorArea != null ? pScissorArea.intersection(screenrectangle) : screenrectangle;
    }
}