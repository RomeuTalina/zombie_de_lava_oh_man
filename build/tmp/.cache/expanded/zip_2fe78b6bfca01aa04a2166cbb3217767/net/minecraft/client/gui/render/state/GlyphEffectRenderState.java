package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public record GlyphEffectRenderState(Matrix3x2f pose, BakedGlyph whiteGlyph, BakedGlyph.Effect effect, @Nullable ScreenRectangle scissorArea)
    implements GuiElementRenderState {
    @Override
    public void buildVertices(VertexConsumer p_406993_, float p_408478_) {
        Matrix4f matrix4f = new Matrix4f().mul(this.pose).translate(0.0F, 0.0F, p_408478_);
        this.whiteGlyph.renderEffect(this.effect, matrix4f, p_406993_, 15728880, true);
    }

    @Override
    public RenderPipeline pipeline() {
        return this.whiteGlyph.guiPipeline();
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.singleTextureWithLightmap(Objects.requireNonNull(this.whiteGlyph.textureView()));
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        return null;
    }

    @Nullable
    @Override
    public ScreenRectangle scissorArea() {
        return this.scissorArea;
    }
}