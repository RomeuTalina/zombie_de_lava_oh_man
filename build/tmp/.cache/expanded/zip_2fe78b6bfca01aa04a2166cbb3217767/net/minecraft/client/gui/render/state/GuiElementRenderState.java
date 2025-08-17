package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface GuiElementRenderState extends ScreenArea {
    void buildVertices(VertexConsumer p_408479_, float p_406310_);

    RenderPipeline pipeline();

    TextureSetup textureSetup();

    @Nullable
    ScreenRectangle scissorArea();
}