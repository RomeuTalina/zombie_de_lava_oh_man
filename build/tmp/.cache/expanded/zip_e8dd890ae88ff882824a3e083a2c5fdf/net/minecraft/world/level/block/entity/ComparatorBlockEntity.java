package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ComparatorBlockEntity extends BlockEntity {
    private static final int DEFAULT_OUTPUT = 0;
    private int output = 0;

    public ComparatorBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.COMPARATOR, pPos, pBlockState);
    }

    @Override
    protected void saveAdditional(ValueOutput p_407501_) {
        super.saveAdditional(p_407501_);
        p_407501_.putInt("OutputSignal", this.output);
    }

    @Override
    protected void loadAdditional(ValueInput p_407475_) {
        super.loadAdditional(p_407475_);
        this.output = p_407475_.getIntOr("OutputSignal", 0);
    }

    public int getOutputSignal() {
        return this.output;
    }

    public void setOutputSignal(int pOutput) {
        this.output = pOutput;
    }
}