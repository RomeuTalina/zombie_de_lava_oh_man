package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HappyGhastModel extends EntityModel<HappyGhastRenderState> {
    public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.2375F);
    private static final float BODY_SQUEEZE = 0.9375F;
    private final ModelPart[] tentacles = new ModelPart[9];
    private final ModelPart body;

    public HappyGhastModel(ModelPart p_407681_) {
        super(p_407681_);
        this.body = p_407681_.getChild("body");

        for (int i = 0; i < this.tentacles.length; i++) {
            this.tentacles[i] = this.body.getChild(PartNames.tentacle(i));
        }
    }

    public static LayerDefinition createBodyLayer(boolean pBaby, CubeDeformation pCubeDeformation) {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, pCubeDeformation),
            PartPose.offset(0.0F, 16.0F, 0.0F)
        );
        if (pBaby) {
            partdefinition1.addOrReplaceChild(
                "inner_body",
                CubeListBuilder.create().texOffs(0, 32).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F, pCubeDeformation.extend(-0.5F)),
                PartPose.offset(0.0F, 8.0F, 0.0F)
            );
        }

        partdefinition1.addOrReplaceChild(
            PartNames.tentacle(0),
            CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, pCubeDeformation),
            PartPose.offset(-3.75F, 7.0F, -5.0F)
        );
        partdefinition1.addOrReplaceChild(
            PartNames.tentacle(1),
            CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, pCubeDeformation),
            PartPose.offset(1.25F, 7.0F, -5.0F)
        );
        partdefinition1.addOrReplaceChild(
            PartNames.tentacle(2),
            CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, pCubeDeformation),
            PartPose.offset(6.25F, 7.0F, -5.0F)
        );
        partdefinition1.addOrReplaceChild(
            PartNames.tentacle(3),
            CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, pCubeDeformation),
            PartPose.offset(-6.25F, 7.0F, 0.0F)
        );
        partdefinition1.addOrReplaceChild(
            PartNames.tentacle(4),
            CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, pCubeDeformation),
            PartPose.offset(-1.25F, 7.0F, 0.0F)
        );
        partdefinition1.addOrReplaceChild(
            PartNames.tentacle(5),
            CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, pCubeDeformation),
            PartPose.offset(3.75F, 7.0F, 0.0F)
        );
        partdefinition1.addOrReplaceChild(
            PartNames.tentacle(6),
            CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, pCubeDeformation),
            PartPose.offset(-3.75F, 7.0F, 5.0F)
        );
        partdefinition1.addOrReplaceChild(
            PartNames.tentacle(7),
            CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, pCubeDeformation),
            PartPose.offset(1.25F, 7.0F, 5.0F)
        );
        partdefinition1.addOrReplaceChild(
            PartNames.tentacle(8),
            CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, pCubeDeformation),
            PartPose.offset(6.25F, 7.0F, 5.0F)
        );
        return LayerDefinition.create(meshdefinition, 64, 64).apply(MeshTransformer.scaling(4.0F));
    }

    public void setupAnim(HappyGhastRenderState p_409844_) {
        super.setupAnim(p_409844_);
        if (!p_409844_.bodyItem.isEmpty()) {
            this.body.xScale = 0.9375F;
            this.body.yScale = 0.9375F;
            this.body.zScale = 0.9375F;
        }

        GhastModel.animateTentacles(p_409844_, this.tentacles);
    }
}