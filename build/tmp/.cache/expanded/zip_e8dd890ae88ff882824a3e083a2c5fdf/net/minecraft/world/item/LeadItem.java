package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class LeadItem extends Item {
    public LeadItem(Item.Properties p_42828_) {
        super(p_42828_);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        if (blockstate.is(BlockTags.FENCES)) {
            Player player = pContext.getPlayer();
            if (!level.isClientSide && player != null) {
                return bindPlayerMobs(player, level, blockpos);
            }
        }

        return InteractionResult.PASS;
    }

    public static InteractionResult bindPlayerMobs(Player pPlayer, Level pLevel, BlockPos pPos) {
        LeashFenceKnotEntity leashfenceknotentity = null;
        List<Leashable> list = Leashable.leashableInArea(pLevel, Vec3.atCenterOf(pPos), p_341570_ -> p_341570_.getLeashHolder() == pPlayer);
        boolean flag = false;

        for (Leashable leashable : list) {
            if (leashfenceknotentity == null) {
                leashfenceknotentity = LeashFenceKnotEntity.getOrCreateKnot(pLevel, pPos);
                leashfenceknotentity.playPlacementSound();
            }

            if (leashable.canHaveALeashAttachedTo(leashfenceknotentity)) {
                leashable.setLeashedTo(leashfenceknotentity, true);
                flag = true;
            }
        }

        if (flag) {
            pLevel.gameEvent(GameEvent.BLOCK_ATTACH, pPos, GameEvent.Context.of(pPlayer));
            return InteractionResult.SUCCESS_SERVER;
        } else {
            return InteractionResult.PASS;
        }
    }
}