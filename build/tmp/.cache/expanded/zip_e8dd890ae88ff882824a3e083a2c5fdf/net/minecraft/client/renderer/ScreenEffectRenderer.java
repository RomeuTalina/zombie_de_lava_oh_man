package net.minecraft.client.renderer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class ScreenEffectRenderer {
    private static final ResourceLocation UNDERWATER_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");
    private final Minecraft minecraft;
    private final MultiBufferSource bufferSource;
    public static final int ITEM_ACTIVATION_ANIMATION_LENGTH = 40;
    @Nullable
    private ItemStack itemActivationItem;
    private int itemActivationTicks;
    private float itemActivationOffX;
    private float itemActivationOffY;

    public ScreenEffectRenderer(Minecraft pMinecraft, MultiBufferSource pBufferSource) {
        this.minecraft = pMinecraft;
        this.bufferSource = pBufferSource;
    }

    public void tick() {
        if (this.itemActivationTicks > 0) {
            this.itemActivationTicks--;
            if (this.itemActivationTicks == 0) {
                this.itemActivationItem = null;
            }
        }
    }

    public void renderScreenEffect(boolean pSleeping, float pPartialTick) {
        PoseStack posestack = new PoseStack();
        Player player = this.minecraft.player;
        if (this.minecraft.options.getCameraType().isFirstPerson() && !pSleeping) {
            if (!player.noPhysics) {
                var overlay = getOverlayBlock(player);
                BlockState blockstate = overlay == null ? null : overlay.getLeft();
                if (blockstate != null) {
                    if (!net.minecraftforge.client.ForgeHooksClient.renderBlockOverlay(player, posestack, net.minecraftforge.client.event.RenderBlockScreenEffectEvent.OverlayType.BLOCK, overlay.getLeft(), overlay.getRight()))
                    renderTex(this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(blockstate), posestack, this.bufferSource);
                }
            }

            if (!this.minecraft.player.isSpectator()) {
                if (this.minecraft.player.isEyeInFluid(FluidTags.WATER)) {
                    if (!net.minecraftforge.client.ForgeHooksClient.renderWaterOverlay(player, posestack))
                    renderWater(this.minecraft, posestack, this.bufferSource);
                } else if (!player.getEyeInFluidType().isAir()) {
                    net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(player.getEyeInFluidType()).renderOverlay(this.minecraft, posestack, this.bufferSource);
                }

                if (this.minecraft.player.isOnFire()) {
                    if (!net.minecraftforge.client.ForgeHooksClient.renderFireOverlay(player, posestack))
                    renderFire(posestack, this.bufferSource);
                }
            }
        }

        if (!this.minecraft.options.hideGui) {
            this.renderItemActivationAnimation(posestack, pPartialTick);
        }
    }

    private void renderItemActivationAnimation(PoseStack pPoseStack, float pPartialTick) {
        if (this.itemActivationItem != null && this.itemActivationTicks > 0) {
            int i = 40 - this.itemActivationTicks;
            float f = (i + pPartialTick) / 40.0F;
            float f1 = f * f;
            float f2 = f * f1;
            float f3 = 10.25F * f2 * f1 - 24.95F * f1 * f1 + 25.5F * f2 - 13.8F * f1 + 4.0F * f;
            float f4 = f3 * (float) Math.PI;
            float f5 = (float)this.minecraft.getWindow().getWidth() / this.minecraft.getWindow().getHeight();
            float f6 = this.itemActivationOffX * 0.3F * f5;
            float f7 = this.itemActivationOffY * 0.3F;
            pPoseStack.pushPose();
            pPoseStack.translate(f6 * Mth.abs(Mth.sin(f4 * 2.0F)), f7 * Mth.abs(Mth.sin(f4 * 2.0F)), -10.0F + 9.0F * Mth.sin(f4));
            float f8 = 0.8F;
            pPoseStack.scale(0.8F, 0.8F, 0.8F);
            pPoseStack.mulPose(Axis.YP.rotationDegrees(900.0F * Mth.abs(Mth.sin(f4))));
            pPoseStack.mulPose(Axis.XP.rotationDegrees(6.0F * Mth.cos(f * 8.0F)));
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(6.0F * Mth.cos(f * 8.0F)));
            this.minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
            this.minecraft
                .getItemRenderer()
                .renderStatic(this.itemActivationItem, ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY, pPoseStack, this.bufferSource, this.minecraft.level, 0);
            pPoseStack.popPose();
        }
    }

    public void resetItemActivation() {
        this.itemActivationItem = null;
    }

    public void displayItemActivation(ItemStack pItem, RandomSource pRandom) {
        this.itemActivationItem = pItem;
        this.itemActivationTicks = 40;
        this.itemActivationOffX = pRandom.nextFloat() * 2.0F - 1.0F;
        this.itemActivationOffY = pRandom.nextFloat() * 2.0F - 1.0F;
    }

    @Nullable
    private static BlockState getViewBlockingState(Player pPlayer) {
        var ret = getOverlayBlock(pPlayer);
        return ret == null ? null : ret.getLeft();
    }

    @Nullable
    private static org.apache.commons.lang3.tuple.Pair<BlockState, BlockPos> getOverlayBlock(Player pPlayer) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < 8; i++) {
            double d0 = pPlayer.getX() + ((i >> 0) % 2 - 0.5F) * pPlayer.getBbWidth() * 0.8F;
            double d1 = pPlayer.getEyeY() + ((i >> 1) % 2 - 0.5F) * 0.1F * pPlayer.getScale();
            double d2 = pPlayer.getZ() + ((i >> 2) % 2 - 0.5F) * pPlayer.getBbWidth() * 0.8F;
            blockpos$mutableblockpos.set(d0, d1, d2);
            BlockState blockstate = pPlayer.level().getBlockState(blockpos$mutableblockpos);
            if (blockstate.getRenderShape() != RenderShape.INVISIBLE && blockstate.isViewBlocking(pPlayer.level(), blockpos$mutableblockpos)) {
                return org.apache.commons.lang3.tuple.Pair.of(blockstate, blockpos$mutableblockpos.immutable());
            }
        }

        return null;
    }

    private static void renderTex(TextureAtlasSprite pTexture, PoseStack pPoseStack, MultiBufferSource pBufferSource) {
        float f = 0.1F;
        int i = ARGB.colorFromFloat(1.0F, 0.1F, 0.1F, 0.1F);
        float f1 = -1.0F;
        float f2 = 1.0F;
        float f3 = -1.0F;
        float f4 = 1.0F;
        float f5 = -0.5F;
        float f6 = pTexture.getU0();
        float f7 = pTexture.getU1();
        float f8 = pTexture.getV0();
        float f9 = pTexture.getV1();
        Matrix4f matrix4f = pPoseStack.last().pose();
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.blockScreenEffect(pTexture.atlasLocation()));
        vertexconsumer.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(f7, f9).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(f6, f9).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(f6, f8).setColor(i);
        vertexconsumer.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(f7, f8).setColor(i);
    }

    private static void renderWater(Minecraft pMinecraft, PoseStack pPoseStack, MultiBufferSource pBufferSource) {
        renderFluid(pMinecraft, pPoseStack, pBufferSource, UNDERWATER_LOCATION);
    }

    public static void renderFluid(Minecraft pMinecraft, PoseStack pPoseStack, MultiBufferSource pBufferSource, ResourceLocation texture) {
        BlockPos blockpos = BlockPos.containing(pMinecraft.player.getX(), pMinecraft.player.getEyeY(), pMinecraft.player.getZ());
        float f = LightTexture.getBrightness(pMinecraft.player.level().dimensionType(), pMinecraft.player.level().getMaxLocalRawBrightness(blockpos));
        int i = ARGB.colorFromFloat(0.1F, f, f, f);
        float f1 = 4.0F;
        float f2 = -1.0F;
        float f3 = 1.0F;
        float f4 = -1.0F;
        float f5 = 1.0F;
        float f6 = -0.5F;
        float f7 = -pMinecraft.player.getYRot() / 64.0F;
        float f8 = pMinecraft.player.getXRot() / 64.0F;
        Matrix4f matrix4f = pPoseStack.last().pose();
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.blockScreenEffect(texture));
        vertexconsumer.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(4.0F + f7, 4.0F + f8).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(0.0F + f7, 4.0F + f8).setColor(i);
        vertexconsumer.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(0.0F + f7, 0.0F + f8).setColor(i);
        vertexconsumer.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(4.0F + f7, 0.0F + f8).setColor(i);
    }

    private static void renderFire(PoseStack pPoseStack, MultiBufferSource pBufferSource) {
        TextureAtlasSprite textureatlassprite = ModelBakery.FIRE_1.sprite();
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.fireScreenEffect(textureatlassprite.atlasLocation()));
        float f = textureatlassprite.getU0();
        float f1 = textureatlassprite.getU1();
        float f2 = (f + f1) / 2.0F;
        float f3 = textureatlassprite.getV0();
        float f4 = textureatlassprite.getV1();
        float f5 = (f3 + f4) / 2.0F;
        float f6 = textureatlassprite.uvShrinkRatio();
        float f7 = Mth.lerp(f6, f, f2);
        float f8 = Mth.lerp(f6, f1, f2);
        float f9 = Mth.lerp(f6, f3, f5);
        float f10 = Mth.lerp(f6, f4, f5);
        float f11 = 1.0F;

        for (int i = 0; i < 2; i++) {
            pPoseStack.pushPose();
            float f12 = -0.5F;
            float f13 = 0.5F;
            float f14 = -0.5F;
            float f15 = 0.5F;
            float f16 = -0.5F;
            pPoseStack.translate(-(i * 2 - 1) * 0.24F, -0.3F, 0.0F);
            pPoseStack.mulPose(Axis.YP.rotationDegrees((i * 2 - 1) * 10.0F));
            Matrix4f matrix4f = pPoseStack.last().pose();
            vertexconsumer.addVertex(matrix4f, -0.5F, -0.5F, -0.5F).setUv(f8, f10).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            vertexconsumer.addVertex(matrix4f, 0.5F, -0.5F, -0.5F).setUv(f7, f10).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            vertexconsumer.addVertex(matrix4f, 0.5F, 0.5F, -0.5F).setUv(f7, f9).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            vertexconsumer.addVertex(matrix4f, -0.5F, 0.5F, -0.5F).setUv(f8, f9).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            pPoseStack.popPose();
        }
    }
}
