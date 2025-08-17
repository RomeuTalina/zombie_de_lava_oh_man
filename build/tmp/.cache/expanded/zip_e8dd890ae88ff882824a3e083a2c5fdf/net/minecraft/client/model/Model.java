package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class Model {
    protected final ModelPart root;
    protected final Function<ResourceLocation, RenderType> renderType;
    private final List<ModelPart> allParts;

    public Model(ModelPart pRoot, Function<ResourceLocation, RenderType> pRenderType) {
        this.root = pRoot;
        this.renderType = pRenderType;
        this.allParts = pRoot.getAllParts();
    }

    public final RenderType renderType(ResourceLocation pLocation) {
        return this.renderType.apply(pLocation);
    }

    public final void renderToBuffer(PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, int pColor) {
        this.root().render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pColor);
    }

    public final void renderToBuffer(PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay) {
        this.renderToBuffer(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, -1);
    }

    public final ModelPart root() {
        return this.root;
    }

    public final List<ModelPart> allParts() {
        return this.allParts;
    }

    public final void resetPose() {
        for (ModelPart modelpart : this.allParts) {
            modelpart.resetPose();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Simple extends Model {
        public Simple(ModelPart pRoot, Function<ResourceLocation, RenderType> pRenderType) {
            super(pRoot, pRenderType);
        }
    }
}