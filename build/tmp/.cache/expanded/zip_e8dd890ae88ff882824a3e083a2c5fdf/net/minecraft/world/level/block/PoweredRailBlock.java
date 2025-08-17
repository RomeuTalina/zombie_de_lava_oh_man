package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

public class PoweredRailBlock extends BaseRailBlock {
    public static final MapCodec<PoweredRailBlock> CODEC = simpleCodec(PoweredRailBlock::new);
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private final boolean isActivator;  // TRUE for an Activator Rail, FALSE for Powered Rail

    @Override
    public MapCodec<PoweredRailBlock> codec() {
        return CODEC;
    }

    public PoweredRailBlock(BlockBehaviour.Properties p_55218_) {
        this(p_55218_, false);
    }

    protected PoweredRailBlock(BlockBehaviour.Properties p_55218_, boolean isPoweredRail) {
        super(true, p_55218_);
        this.isActivator = !isPoweredRail;
        this.registerDefaultState();
    }

    protected void registerDefaultState() {
        this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(POWERED, false).setValue(WATERLOGGED, false));
    }

    protected boolean findPoweredRailSignal(Level pLevel, BlockPos pPos, BlockState pState, boolean pSearchForward, int pRecursionCount) {
        if (pRecursionCount >= 8) {
            return false;
        } else {
            int i = pPos.getX();
            int j = pPos.getY();
            int k = pPos.getZ();
            boolean flag = true;
            RailShape railshape = pState.getValue(getShapeProperty());
            switch (railshape) {
                case NORTH_SOUTH:
                    if (pSearchForward) {
                        k++;
                    } else {
                        k--;
                    }
                    break;
                case EAST_WEST:
                    if (pSearchForward) {
                        i--;
                    } else {
                        i++;
                    }
                    break;
                case ASCENDING_EAST:
                    if (pSearchForward) {
                        i--;
                    } else {
                        i++;
                        j++;
                        flag = false;
                    }

                    railshape = RailShape.EAST_WEST;
                    break;
                case ASCENDING_WEST:
                    if (pSearchForward) {
                        i--;
                        j++;
                        flag = false;
                    } else {
                        i++;
                    }

                    railshape = RailShape.EAST_WEST;
                    break;
                case ASCENDING_NORTH:
                    if (pSearchForward) {
                        k++;
                    } else {
                        k--;
                        j++;
                        flag = false;
                    }

                    railshape = RailShape.NORTH_SOUTH;
                    break;
                case ASCENDING_SOUTH:
                    if (pSearchForward) {
                        k++;
                        j++;
                        flag = false;
                    } else {
                        k--;
                    }

                    railshape = RailShape.NORTH_SOUTH;
            }

            return this.isSameRailWithPower(pLevel, new BlockPos(i, j, k), pSearchForward, pRecursionCount, railshape)
                ? true
                : flag && this.isSameRailWithPower(pLevel, new BlockPos(i, j - 1, k), pSearchForward, pRecursionCount, railshape);
        }
    }

    protected boolean isSameRailWithPower(Level pLevel, BlockPos pState, boolean pSearchForward, int pRecursionCount, RailShape pShape) {
        BlockState blockstate = pLevel.getBlockState(pState);
        if (!(blockstate.getBlock() instanceof PoweredRailBlock other)) {
            return false;
        } else {
            RailShape railshape = other.getRailDirection(blockstate, pLevel, pState, null);
            if (pShape != RailShape.EAST_WEST
                || railshape != RailShape.NORTH_SOUTH && railshape != RailShape.ASCENDING_NORTH && railshape != RailShape.ASCENDING_SOUTH) {
                if (pShape != RailShape.NORTH_SOUTH
                    || railshape != RailShape.EAST_WEST && railshape != RailShape.ASCENDING_EAST && railshape != RailShape.ASCENDING_WEST) {
                    if (!blockstate.getValue(POWERED)) {
                        return false;
                    } else {
                        return pLevel.hasNeighborSignal(pState) ? true : other.findPoweredRailSignal(pLevel, pState, blockstate, pSearchForward, pRecursionCount + 1);
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    protected void updateState(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock) {
        boolean flag = pState.getValue(POWERED);
        boolean flag1 = pLevel.hasNeighborSignal(pPos)
            || this.findPoweredRailSignal(pLevel, pPos, pState, true, 0)
            || this.findPoweredRailSignal(pLevel, pPos, pState, false, 0);
        if (flag1 != flag) {
            pLevel.setBlock(pPos, pState.setValue(POWERED, flag1), 3);
            pLevel.updateNeighborsAt(pPos.below(), this);
            if (pState.getValue(getShapeProperty()).isSlope()) {
                pLevel.updateNeighborsAt(pPos.above(), this);
            }
        }
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRot) {
        RailShape railshape = pState.getValue(SHAPE);
        RailShape railshape1 = this.rotate(railshape, pRot);
        return pState.setValue(SHAPE, railshape1);
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        RailShape railshape = pState.getValue(SHAPE);
        RailShape railshape1 = this.mirror(railshape, pMirror);
        return pState.setValue(SHAPE, railshape1);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(getShapeProperty(), POWERED, WATERLOGGED);
    }

    public boolean isActivatorRail() {
        return isActivator;
    }
}
