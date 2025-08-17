package net.minecraft.world.level.block.sounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class AmbientDesertBlockSoundsPlayer {
    private static final int IDLE_SOUND_CHANCE = 2100;
    private static final int DRY_GRASS_SOUND_CHANCE = 200;
    private static final int DEAD_BUSH_SOUND_CHANCE = 130;
    private static final int DEAD_BUSH_SOUND_BADLANDS_DECREASED_CHANCE = 3;
    private static final int SURROUNDING_BLOCKS_PLAY_SOUND_THRESHOLD = 3;
    private static final int SURROUNDING_BLOCKS_DISTANCE_HORIZONTAL_CHECK = 8;
    private static final int SURROUNDING_BLOCKS_DISTANCE_VERTICAL_CHECK = 5;
    private static final int HORIZONTAL_DIRECTIONS = 4;

    public static void playAmbientSandSounds(Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pLevel.getBlockState(pPos.above()).is(Blocks.AIR)) {
            if (pRandom.nextInt(2100) == 0 && shouldPlayAmbientSandSound(pLevel, pPos)) {
                pLevel.playLocalSound(
                    pPos.getX(), pPos.getY(), pPos.getZ(), SoundEvents.SAND_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false
                );
            }
        }
    }

    public static void playAmbientDryGrassSounds(Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pRandom.nextInt(200) == 0 && shouldPlayDesertDryVegetationBlockSounds(pLevel, pPos.below())) {
            pLevel.playPlayerSound(SoundEvents.DRY_GRASS, SoundSource.AMBIENT, 1.0F, 1.0F);
        }
    }

    public static void playAmbientDeadBushSounds(Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pRandom.nextInt(130) == 0) {
            BlockState blockstate = pLevel.getBlockState(pPos.below());
            if ((blockstate.is(Blocks.RED_SAND) || blockstate.is(BlockTags.TERRACOTTA)) && pRandom.nextInt(3) != 0) {
                return;
            }

            if (shouldPlayDesertDryVegetationBlockSounds(pLevel, pPos.below())) {
                pLevel.playLocalSound(
                    pPos.getX(), pPos.getY(), pPos.getZ(), SoundEvents.DEAD_BUSH_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false
                );
            }
        }
    }

    public static boolean shouldPlayDesertDryVegetationBlockSounds(Level pLevel, BlockPos pPos) {
        return pLevel.getBlockState(pPos).is(BlockTags.TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS) && pLevel.getBlockState(pPos.below()).is(BlockTags.TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS);
    }

    private static boolean shouldPlayAmbientSandSound(Level pLevel, BlockPos pPos) {
        int i = 0;
        int j = 0;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            blockpos$mutableblockpos.set(pPos).move(direction, 8);
            if (columnContainsTriggeringBlock(pLevel, blockpos$mutableblockpos) && i++ >= 3) {
                return true;
            }

            j++;
            int k = 4 - j;
            int l = k + i;
            boolean flag = l >= 3;
            if (!flag) {
                return false;
            }
        }

        return false;
    }

    private static boolean columnContainsTriggeringBlock(Level pLevel, BlockPos.MutableBlockPos pPos) {
        int i = pLevel.getHeight(Heightmap.Types.WORLD_SURFACE, pPos) - 1;
        if (Math.abs(i - pPos.getY()) > 5) {
            pPos.move(Direction.UP, 6);
            BlockState blockstate1 = pLevel.getBlockState(pPos);
            pPos.move(Direction.DOWN);

            for (int j = 0; j < 10; j++) {
                BlockState blockstate = pLevel.getBlockState(pPos);
                if (blockstate1.isAir() && canTriggerAmbientDesertSandSounds(blockstate)) {
                    return true;
                }

                blockstate1 = blockstate;
                pPos.move(Direction.DOWN);
            }

            return false;
        } else {
            boolean flag = pLevel.getBlockState(pPos.setY(i + 1)).isAir();
            return flag && canTriggerAmbientDesertSandSounds(pLevel.getBlockState(pPos.setY(i)));
        }
    }

    private static boolean canTriggerAmbientDesertSandSounds(BlockState pState) {
        return pState.is(BlockTags.TRIGGERS_AMBIENT_DESERT_SAND_BLOCK_SOUNDS);
    }
}