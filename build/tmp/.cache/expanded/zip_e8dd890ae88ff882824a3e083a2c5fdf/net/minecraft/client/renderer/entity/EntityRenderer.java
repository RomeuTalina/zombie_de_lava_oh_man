package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HitboxRenderState;
import net.minecraft.client.renderer.entity.state.HitboxesRenderState;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.NewMinecartBehavior;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState> {
    protected static final float NAMETAG_SCALE = 0.025F;
    public static final int LEASH_RENDER_STEPS = 24;
    public static final float LEASH_WIDTH = 0.05F;
    protected final EntityRenderDispatcher entityRenderDispatcher;
    private final Font font;
    protected float shadowRadius;
    protected float shadowStrength = 1.0F;
    private final S reusedState = this.createRenderState();

    protected EntityRenderer(EntityRendererProvider.Context pContext) {
        this.entityRenderDispatcher = pContext.getEntityRenderDispatcher();
        this.font = pContext.getFont();
    }

    public final int getPackedLightCoords(T pEntity, float pPartialTicks) {
        BlockPos blockpos = BlockPos.containing(pEntity.getLightProbePosition(pPartialTicks));
        return LightTexture.pack(this.getBlockLightLevel(pEntity, blockpos), this.getSkyLightLevel(pEntity, blockpos));
    }

    protected int getSkyLightLevel(T pEntity, BlockPos pPos) {
        return pEntity.level().getBrightness(LightLayer.SKY, pPos);
    }

    protected int getBlockLightLevel(T pEntity, BlockPos pPos) {
        return pEntity.isOnFire() ? 15 : pEntity.level().getBrightness(LightLayer.BLOCK, pPos);
    }

    public boolean shouldRender(T pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        if (!pLivingEntity.shouldRender(pCamX, pCamY, pCamZ)) {
            return false;
        } else if (!this.affectedByCulling(pLivingEntity)) {
            return true;
        } else {
            AABB aabb = this.getBoundingBoxForCulling(pLivingEntity).inflate(0.5);
            if (aabb.hasNaN() || aabb.getSize() == 0.0) {
                aabb = new AABB(
                    pLivingEntity.getX() - 2.0,
                    pLivingEntity.getY() - 2.0,
                    pLivingEntity.getZ() - 2.0,
                    pLivingEntity.getX() + 2.0,
                    pLivingEntity.getY() + 2.0,
                    pLivingEntity.getZ() + 2.0
                );
            }

            if (pCamera.isVisible(aabb)) {
                return true;
            } else {
                if (pLivingEntity instanceof Leashable leashable) {
                    Entity entity = leashable.getLeashHolder();
                    if (entity != null) {
                        AABB aabb1 = this.entityRenderDispatcher.getRenderer(entity).getBoundingBoxForCulling(entity);
                        return pCamera.isVisible(aabb1) || pCamera.isVisible(aabb.minmax(aabb1));
                    }
                }

                return false;
            }
        }
    }

    protected AABB getBoundingBoxForCulling(T pMinecraft) {
        return pMinecraft.getBoundingBox();
    }

    protected boolean affectedByCulling(T pDisplay) {
        return true;
    }

    public Vec3 getRenderOffset(S pRenderState) {
        return pRenderState.passengerOffset != null ? pRenderState.passengerOffset : Vec3.ZERO;
    }

    public void render(S pRenderState, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight) {
        if (pRenderState.leashStates != null) {
            for (EntityRenderState.LeashState entityrenderstate$leashstate : pRenderState.leashStates) {
                renderLeash(pPoseStack, pBufferSource, entityrenderstate$leashstate);
            }
        }

        var event = net.minecraftforge.client.event.ForgeEventFactoryClient.fireRenderNameTagEvent(pRenderState, pRenderState.nameTag, this, pPoseStack, pBufferSource, pPackedLight);
        if (!event.getResult().isDenied() && (event.getResult().isAllowed() || pRenderState.nameTag != null)) {
           this.renderNameTag(pRenderState, event.getContent(), pPoseStack, pBufferSource, pPackedLight);
        }
    }

    private static void renderLeash(PoseStack pPoseStack, MultiBufferSource pBuffer, EntityRenderState.LeashState pLeashState) {
        float f = (float)(pLeashState.end.x - pLeashState.start.x);
        float f1 = (float)(pLeashState.end.y - pLeashState.start.y);
        float f2 = (float)(pLeashState.end.z - pLeashState.start.z);
        float f3 = Mth.invSqrt(f * f + f2 * f2) * 0.05F / 2.0F;
        float f4 = f2 * f3;
        float f5 = f * f3;
        pPoseStack.pushPose();
        pPoseStack.translate(pLeashState.offset);
        VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.leash());
        Matrix4f matrix4f = pPoseStack.last().pose();

        for (int i = 0; i <= 24; i++) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, 0.05F, 0.05F, f4, f5, i, false, pLeashState);
        }

        for (int j = 24; j >= 0; j--) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, 0.05F, 0.0F, f4, f5, j, true, pLeashState);
        }

        pPoseStack.popPose();
    }

    private static void addVertexPair(
        VertexConsumer pConsumer,
        Matrix4f pPose,
        float pStartX,
        float pStartY,
        float pStartZ,
        float pYOffset,
        float pDy,
        float pDx,
        float pDz,
        int pIndex,
        boolean pReverse,
        EntityRenderState.LeashState pLeashState
    ) {
        float f = pIndex / 24.0F;
        int i = (int)Mth.lerp(f, pLeashState.startBlockLight, pLeashState.endBlockLight);
        int j = (int)Mth.lerp(f, pLeashState.startSkyLight, pLeashState.endSkyLight);
        int k = LightTexture.pack(i, j);
        float f1 = pIndex % 2 == (pReverse ? 1 : 0) ? 0.7F : 1.0F;
        float f2 = 0.5F * f1;
        float f3 = 0.4F * f1;
        float f4 = 0.3F * f1;
        float f5 = pStartX * f;
        float f6;
        if (pLeashState.slack) {
            f6 = pStartY > 0.0F ? pStartY * f * f : pStartY - pStartY * (1.0F - f) * (1.0F - f);
        } else {
            f6 = pStartY * f;
        }

        float f7 = pStartZ * f;
        pConsumer.addVertex(pPose, f5 - pDx, f6 + pDy, f7 + pDz).setColor(f2, f3, f4, 1.0F).setLight(k);
        pConsumer.addVertex(pPose, f5 + pDx, f6 + pYOffset - pDy, f7 - pDz).setColor(f2, f3, f4, 1.0F).setLight(k);
    }

    protected boolean shouldShowName(T pEntity, double pDistanceToCameraSq) {
        return pEntity.shouldShowName() || pEntity.hasCustomName() && pEntity == this.entityRenderDispatcher.crosshairPickEntity;
    }

    public Font getFont() {
        return this.font;
    }

    protected void renderNameTag(S pRenderState, Component pDisplayName, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight) {
        Vec3 vec3 = pRenderState.nameTagAttachment;
        if (vec3 != null) {
            boolean flag = !pRenderState.isDiscrete;
            int i = "deadmau5".equals(pDisplayName.getString()) ? -10 : 0;
            pPoseStack.pushPose();
            pPoseStack.translate(vec3.x, vec3.y + 0.5, vec3.z);
            pPoseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            pPoseStack.scale(0.025F, -0.025F, 0.025F);
            Matrix4f matrix4f = pPoseStack.last().pose();
            Font font = this.getFont();
            float f = -font.width(pDisplayName) / 2.0F;
            int j = (int)(Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
            font.drawInBatch(
                pDisplayName, f, i, -2130706433, false, matrix4f, pBufferSource, flag ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, j, pPackedLight
            );
            if (flag) {
                font.drawInBatch(pDisplayName, f, i, -1, false, matrix4f, pBufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.lightCoordsWithEmission(pPackedLight, 2));
            }

            pPoseStack.popPose();
        }
    }

    @Nullable
    protected Component getNameTag(T pEntity) {
        return pEntity.getDisplayName();
    }

    protected float getShadowRadius(S pRenderState) {
        return this.shadowRadius;
    }

    protected float getShadowStrength(S pRenderState) {
        return this.shadowStrength;
    }

    public abstract S createRenderState();

    public final S createRenderState(T pEntity, float pPartialTick) {
        S s = this.reusedState;
        this.extractRenderState(pEntity, s, pPartialTick);
        return s;
    }

    public void extractRenderState(T pEntity, S pReusedState, float pPartialTick) {
        pReusedState.entityType = pEntity.getType();
        pReusedState.x = Mth.lerp(pPartialTick, pEntity.xOld, pEntity.getX());
        pReusedState.y = Mth.lerp(pPartialTick, pEntity.yOld, pEntity.getY());
        pReusedState.z = Mth.lerp(pPartialTick, pEntity.zOld, pEntity.getZ());
        pReusedState.isInvisible = pEntity.isInvisible();
        pReusedState.ageInTicks = pEntity.tickCount + pPartialTick;
        pReusedState.boundingBoxWidth = pEntity.getBbWidth();
        pReusedState.boundingBoxHeight = pEntity.getBbHeight();
        pReusedState.eyeHeight = pEntity.getEyeHeight();
        if (pEntity.isPassenger()
            && pEntity.getVehicle() instanceof AbstractMinecart abstractminecart
            && abstractminecart.getBehavior() instanceof NewMinecartBehavior newminecartbehavior
            && newminecartbehavior.cartHasPosRotLerp()) {
            double d2 = Mth.lerp(pPartialTick, abstractminecart.xOld, abstractminecart.getX());
            double d0 = Mth.lerp(pPartialTick, abstractminecart.yOld, abstractminecart.getY());
            double d1 = Mth.lerp(pPartialTick, abstractminecart.zOld, abstractminecart.getZ());
            pReusedState.passengerOffset = newminecartbehavior.getCartLerpPosition(pPartialTick).subtract(new Vec3(d2, d0, d1));
        } else {
            pReusedState.passengerOffset = null;
        }

        pReusedState.distanceToCameraSq = this.entityRenderDispatcher.distanceToSqr(pEntity);
        boolean flag1 = net.minecraftforge.client.ForgeHooksClient.isNameplateInRenderDistance(pEntity, pReusedState.distanceToCameraSq) && this.shouldShowName(pEntity, pReusedState.distanceToCameraSq);
        if (flag1) {
            pReusedState.nameTag = this.getNameTag(pEntity);
            pReusedState.nameTagAttachment = pEntity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, pEntity.getYRot(pPartialTick));
        } else {
            pReusedState.nameTag = null;
        }

        label77: {
            pReusedState.isDiscrete = pEntity.isDiscrete();
            if (pEntity instanceof Leashable leashable) {
                Entity $$11 = leashable.getLeashHolder();
                if ($$11 instanceof Entity) {
                    float f = pEntity.getPreciseBodyRotation(pPartialTick) * (float) (Math.PI / 180.0);
                    Vec3 vec31 = leashable.getLeashOffset(pPartialTick);
                    BlockPos blockpos = BlockPos.containing(pEntity.getEyePosition(pPartialTick));
                    BlockPos blockpos1 = BlockPos.containing($$11.getEyePosition(pPartialTick));
                    int i = this.getBlockLightLevel(pEntity, blockpos);
                    int j = this.entityRenderDispatcher.getRenderer($$11).getBlockLightLevel($$11, blockpos1);
                    int k = pEntity.level().getBrightness(LightLayer.SKY, blockpos);
                    int l = pEntity.level().getBrightness(LightLayer.SKY, blockpos1);
                    boolean flag = $$11.supportQuadLeashAsHolder() && leashable.supportQuadLeash();
                    int i1 = flag ? 4 : 1;
                    if (pReusedState.leashStates == null || pReusedState.leashStates.size() != i1) {
                        pReusedState.leashStates = new ArrayList<>(i1);

                        for (int j1 = 0; j1 < i1; j1++) {
                            pReusedState.leashStates.add(new EntityRenderState.LeashState());
                        }
                    }

                    if (flag) {
                        float f1 = $$11.getPreciseBodyRotation(pPartialTick) * (float) (Math.PI / 180.0);
                        Vec3 vec3 = $$11.getPosition(pPartialTick);
                        Vec3[] avec3 = leashable.getQuadLeashOffsets();
                        Vec3[] avec31 = $$11.getQuadLeashHolderOffsets();
                        int k1 = 0;

                        while (true) {
                            if (k1 >= i1) {
                                break label77;
                            }

                            EntityRenderState.LeashState entityrenderstate$leashstate = pReusedState.leashStates.get(k1);
                            entityrenderstate$leashstate.offset = avec3[k1].yRot(-f);
                            entityrenderstate$leashstate.start = pEntity.getPosition(pPartialTick).add(entityrenderstate$leashstate.offset);
                            entityrenderstate$leashstate.end = vec3.add(avec31[k1].yRot(-f1));
                            entityrenderstate$leashstate.startBlockLight = i;
                            entityrenderstate$leashstate.endBlockLight = j;
                            entityrenderstate$leashstate.startSkyLight = k;
                            entityrenderstate$leashstate.endSkyLight = l;
                            entityrenderstate$leashstate.slack = false;
                            k1++;
                        }
                    } else {
                        Vec3 vec32 = vec31.yRot(-f);
                        EntityRenderState.LeashState entityrenderstate$leashstate1 = pReusedState.leashStates.getFirst();
                        entityrenderstate$leashstate1.offset = vec32;
                        entityrenderstate$leashstate1.start = pEntity.getPosition(pPartialTick).add(vec32);
                        entityrenderstate$leashstate1.end = $$11.getRopeHoldPosition(pPartialTick);
                        entityrenderstate$leashstate1.startBlockLight = i;
                        entityrenderstate$leashstate1.endBlockLight = j;
                        entityrenderstate$leashstate1.startSkyLight = k;
                        entityrenderstate$leashstate1.endSkyLight = l;
                        break label77;
                    }
                }
            }

            pReusedState.leashStates = null;
        }

        pReusedState.displayFireAnimation = pEntity.displayFireAnimation();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes() && !pReusedState.isInvisible && !minecraft.showOnlyReducedInfo()) {
            this.extractHitboxes(pEntity, pReusedState, pPartialTick);
        } else {
            pReusedState.hitboxesRenderState = null;
            pReusedState.serverHitboxesRenderState = null;
        }
    }

    private void extractHitboxes(T pEntity, S pReusedState, float pPartialTick) {
        pReusedState.hitboxesRenderState = this.extractHitboxes(pEntity, pPartialTick, false);
        pReusedState.serverHitboxesRenderState = null;
    }

    private HitboxesRenderState extractHitboxes(T pEntity, float pPartialTick, boolean pGreen) {
        Builder<HitboxRenderState> builder = new Builder<>();
        AABB aabb = pEntity.getBoundingBox();
        HitboxRenderState hitboxrenderstate;
        if (pGreen) {
            hitboxrenderstate = new HitboxRenderState(
                aabb.minX - pEntity.getX(),
                aabb.minY - pEntity.getY(),
                aabb.minZ - pEntity.getZ(),
                aabb.maxX - pEntity.getX(),
                aabb.maxY - pEntity.getY(),
                aabb.maxZ - pEntity.getZ(),
                0.0F,
                1.0F,
                0.0F
            );
        } else {
            hitboxrenderstate = new HitboxRenderState(
                aabb.minX - pEntity.getX(),
                aabb.minY - pEntity.getY(),
                aabb.minZ - pEntity.getZ(),
                aabb.maxX - pEntity.getX(),
                aabb.maxY - pEntity.getY(),
                aabb.maxZ - pEntity.getZ(),
                1.0F,
                1.0F,
                1.0F
            );
        }

        builder.add(hitboxrenderstate);
        Entity entity = pEntity.getVehicle();
        if (entity != null) {
            float f = Math.min(entity.getBbWidth(), pEntity.getBbWidth()) / 2.0F;
            float f1 = 0.0625F;
            Vec3 vec3 = entity.getPassengerRidingPosition(pEntity).subtract(pEntity.position());
            HitboxRenderState hitboxrenderstate1 = new HitboxRenderState(
                vec3.x - f, vec3.y, vec3.z - f, vec3.x + f, vec3.y + 0.0625, vec3.z + f, 1.0F, 1.0F, 0.0F
            );
            builder.add(hitboxrenderstate1);
        }

        this.extractAdditionalHitboxes(pEntity, builder, pPartialTick);
        Vec3 vec31 = pEntity.getViewVector(pPartialTick);
        return new HitboxesRenderState(vec31.x, vec31.y, vec31.z, builder.build());
    }

    protected void extractAdditionalHitboxes(T pEntity, Builder<HitboxRenderState> pHitboxes, float pPartialTick) {
        net.minecraft.client.renderer.entity.EnderDragonRenderer.extractAdditionalHitboxexGeneric(pEntity, pHitboxes, pPartialTick); // Forge: Extract for our generic multi-part entity system
    }

    @Nullable
    private static Entity getServerSideEntity(Entity pEntity) {
        IntegratedServer integratedserver = Minecraft.getInstance().getSingleplayerServer();
        if (integratedserver != null) {
            ServerLevel serverlevel = integratedserver.getLevel(pEntity.level().dimension());
            if (serverlevel != null) {
                return serverlevel.getEntity(pEntity.getId());
            }
        }

        return null;
    }
}
