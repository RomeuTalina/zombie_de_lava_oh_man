package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

public class RailBlock extends BaseRailBlock {
    public static final MapCodec<RailBlock> CODEC = simpleCodec(RailBlock::new);
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE;

    @Override
    public MapCodec<RailBlock> codec() {
        return CODEC;
    }

    public RailBlock(BlockBehaviour.Properties p_55395_) {
        super(false, p_55395_);
        this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(WATERLOGGED, false));
    }

    @Override
    protected void updateState(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock) {
        if (pBlock.defaultBlockState().isSignalSource() && new RailState(pLevel, pPos, pState).countPotentialConnections() == 3) {
            this.updateDir(pLevel, pPos, pState, false);
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
        pBuilder.add(SHAPE, WATERLOGGED);
    }
}