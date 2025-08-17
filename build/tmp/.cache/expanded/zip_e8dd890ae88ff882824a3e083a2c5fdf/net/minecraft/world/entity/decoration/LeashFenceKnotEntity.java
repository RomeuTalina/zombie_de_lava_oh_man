package net.minecraft.world.entity.decoration;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LeashFenceKnotEntity extends BlockAttachedEntity {
    public static final double OFFSET_Y = 0.375;

    public LeashFenceKnotEntity(EntityType<? extends LeashFenceKnotEntity> p_31828_, Level p_31829_) {
        super(p_31828_, p_31829_);
    }

    public LeashFenceKnotEntity(Level pLevel, BlockPos pPos) {
        super(EntityType.LEASH_KNOT, pLevel, pPos);
        this.setPos(pPos.getX(), pPos.getY(), pPos.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_343909_) {
    }

    @Override
    protected void recalculateBoundingBox() {
        this.setPosRaw(this.pos.getX() + 0.5, this.pos.getY() + 0.375, this.pos.getZ() + 0.5);
        double d0 = this.getType().getWidth() / 2.0;
        double d1 = this.getType().getHeight();
        this.setBoundingBox(new AABB(this.getX() - d0, this.getY(), this.getZ() - d0, this.getX() + d0, this.getY() + d1, this.getZ() + d0));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return pDistance < 1024.0;
    }

    @Override
    public void dropItem(ServerLevel p_367811_, @Nullable Entity p_31837_) {
        this.playSound(SoundEvents.LEAD_UNTIED, 1.0F, 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_407899_) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_406481_) {
    }

    @Override
    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            if (pPlayer.getItemInHand(pHand).is(Items.SHEARS)) {
                InteractionResult interactionresult = super.interact(pPlayer, pHand);
                if (interactionresult instanceof InteractionResult.Success interactionresult$success && interactionresult$success.wasItemInteraction()) {
                    return interactionresult;
                }
            }

            boolean flag = false;

            for (Leashable leashable : Leashable.leashableLeashedTo(pPlayer)) {
                if (leashable.canHaveALeashAttachedTo(this)) {
                    leashable.setLeashedTo(this, true);
                    flag = true;
                }
            }

            boolean flag1 = false;
            if (!flag && !pPlayer.isSecondaryUseActive()) {
                for (Leashable leashable1 : Leashable.leashableLeashedTo(this)) {
                    if (leashable1.canHaveALeashAttachedTo(pPlayer)) {
                        leashable1.setLeashedTo(pPlayer, true);
                        flag1 = true;
                    }
                }
            }

            if (!flag && !flag1) {
                return super.interact(pPlayer, pHand);
            } else {
                this.gameEvent(GameEvent.BLOCK_ATTACH, pPlayer);
                this.playSound(SoundEvents.LEAD_TIED);
                return InteractionResult.SUCCESS;
            }
        }
    }

    @Override
    public void notifyLeasheeRemoved(Leashable p_407496_) {
        if (Leashable.leashableLeashedTo(this).isEmpty()) {
            this.discard();
        }
    }

    @Override
    public boolean survives() {
        return this.level().getBlockState(this.pos).is(BlockTags.FENCES);
    }

    public static LeashFenceKnotEntity getOrCreateKnot(Level pLevel, BlockPos pPos) {
        int i = pPos.getX();
        int j = pPos.getY();
        int k = pPos.getZ();

        for (LeashFenceKnotEntity leashfenceknotentity : pLevel.getEntitiesOfClass(
            LeashFenceKnotEntity.class, new AABB(i - 1.0, j - 1.0, k - 1.0, i + 1.0, j + 1.0, k + 1.0)
        )) {
            if (leashfenceknotentity.getPos().equals(pPos)) {
                return leashfenceknotentity;
            }
        }

        LeashFenceKnotEntity leashfenceknotentity1 = new LeashFenceKnotEntity(pLevel, pPos);
        pLevel.addFreshEntity(leashfenceknotentity1);
        return leashfenceknotentity1;
    }

    public void playPlacementSound() {
        this.playSound(SoundEvents.LEAD_TIED, 1.0F, 1.0F);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_344045_) {
        return new ClientboundAddEntityPacket(this, 0, this.getPos());
    }

    @Override
    public Vec3 getRopeHoldPosition(float pPartialTicks) {
        return this.getPosition(pPartialTicks).add(0.0, 0.2, 0.0);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.LEAD);
    }
}