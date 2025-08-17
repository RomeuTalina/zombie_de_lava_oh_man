package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class GuiEntityRenderer extends PictureInPictureRenderer<GuiEntityRenderState> {
    private final EntityRenderDispatcher entityRenderDispatcher;

    public GuiEntityRenderer(MultiBufferSource.BufferSource pBufferSource, EntityRenderDispatcher pEntityRenderDispatcher) {
        super(pBufferSource);
        this.entityRenderDispatcher = pEntityRenderDispatcher;
    }

    @Override
    public Class<GuiEntityRenderState> getRenderStateClass() {
        return GuiEntityRenderState.class;
    }

    protected void renderToTexture(GuiEntityRenderState p_408559_, PoseStack p_410540_) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        Vector3f vector3f = p_408559_.translation();
        p_410540_.translate(vector3f.x, vector3f.y, vector3f.z);
        p_410540_.mulPose(p_408559_.rotation());
        Quaternionf quaternionf = p_408559_.overrideCameraAngle();
        if (quaternionf != null) {
            this.entityRenderDispatcher.overrideCameraOrientation(quaternionf.conjugate(new Quaternionf()).rotateY((float) Math.PI));
        }

        this.entityRenderDispatcher.setRenderShadow(false);
        this.entityRenderDispatcher.render(p_408559_.renderState(), 0.0, 0.0, 0.0, p_410540_, this.bufferSource, 15728880);
        this.entityRenderDispatcher.setRenderShadow(true);
    }

    @Override
    protected float getTranslateY(int p_409319_, int p_407944_) {
        return p_409319_ / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "entity";
    }
}