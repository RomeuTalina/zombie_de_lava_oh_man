package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.pip.GuiBannerResultRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiBannerResultRenderer extends PictureInPictureRenderer<GuiBannerResultRenderState> {
    public GuiBannerResultRenderer(MultiBufferSource.BufferSource p_410401_) {
        super(p_410401_);
    }

    @Override
    public Class<GuiBannerResultRenderState> getRenderStateClass() {
        return GuiBannerResultRenderState.class;
    }

    protected void renderToTexture(GuiBannerResultRenderState p_407898_, PoseStack p_406360_) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        p_406360_.translate(0.0F, 0.25F, 0.0F);
        BannerRenderer.renderPatterns(
            p_406360_,
            this.bufferSource,
            15728880,
            OverlayTexture.NO_OVERLAY,
            p_407898_.flag(),
            ModelBakery.BANNER_BASE,
            true,
            p_407898_.baseColor(),
            p_407898_.resultBannerPatterns()
        );
    }

    @Override
    protected String getTextureLabel() {
        return "banner result";
    }
}