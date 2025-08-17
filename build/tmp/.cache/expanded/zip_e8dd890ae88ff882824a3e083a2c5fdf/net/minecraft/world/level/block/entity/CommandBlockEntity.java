package net.minecraft.world.level.block.entity;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class CommandBlockEntity extends BlockEntity {
    private static final boolean DEFAULT_POWERED = false;
    private static final boolean DEFAULT_CONDITION_MET = false;
    private static final boolean DEFAULT_AUTOMATIC = false;
    private boolean powered = false;
    private boolean auto = false;
    private boolean conditionMet = false;
    private final BaseCommandBlock commandBlock = new BaseCommandBlock() {
        @Override
        public void setCommand(String p_59157_) {
            super.setCommand(p_59157_);
            CommandBlockEntity.this.setChanged();
        }

        @Override
        public ServerLevel getLevel() {
            return (ServerLevel)CommandBlockEntity.this.level;
        }

        @Override
        public void onUpdated() {
            BlockState blockstate = CommandBlockEntity.this.level.getBlockState(CommandBlockEntity.this.worldPosition);
            this.getLevel().sendBlockUpdated(CommandBlockEntity.this.worldPosition, blockstate, blockstate, 3);
        }

        @Override
        public Vec3 getPosition() {
            return Vec3.atCenterOf(CommandBlockEntity.this.worldPosition);
        }

        @Override
        public CommandSourceStack createCommandSourceStack() {
            Direction direction = CommandBlockEntity.this.getBlockState().getValue(CommandBlock.FACING);
            return new CommandSourceStack(
                this,
                Vec3.atCenterOf(CommandBlockEntity.this.worldPosition),
                new Vec2(0.0F, direction.toYRot()),
                this.getLevel(),
                2,
                this.getName().getString(),
                this.getName(),
                this.getLevel().getServer(),
                null
            );
        }

        @Override
        public boolean isValid() {
            return !CommandBlockEntity.this.isRemoved();
        }
    };

    public CommandBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.COMMAND_BLOCK, pPos, pBlockState);
    }

    @Override
    protected void saveAdditional(ValueOutput p_410309_) {
        super.saveAdditional(p_410309_);
        this.commandBlock.save(p_410309_);
        p_410309_.putBoolean("powered", this.isPowered());
        p_410309_.putBoolean("conditionMet", this.wasConditionMet());
        p_410309_.putBoolean("auto", this.isAutomatic());
    }

    @Override
    protected void loadAdditional(ValueInput p_409355_) {
        super.loadAdditional(p_409355_);
        this.commandBlock.load(p_409355_);
        this.powered = p_409355_.getBooleanOr("powered", false);
        this.conditionMet = p_409355_.getBooleanOr("conditionMet", false);
        this.setAutomatic(p_409355_.getBooleanOr("auto", false));
    }

    public BaseCommandBlock getCommandBlock() {
        return this.commandBlock;
    }

    public void setPowered(boolean pPowered) {
        this.powered = pPowered;
    }

    public boolean isPowered() {
        return this.powered;
    }

    public boolean isAutomatic() {
        return this.auto;
    }

    public void setAutomatic(boolean pAuto) {
        boolean flag = this.auto;
        this.auto = pAuto;
        if (!flag && pAuto && !this.powered && this.level != null && this.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
            this.scheduleTick();
        }
    }

    public void onModeSwitch() {
        CommandBlockEntity.Mode commandblockentity$mode = this.getMode();
        if (commandblockentity$mode == CommandBlockEntity.Mode.AUTO && (this.powered || this.auto) && this.level != null) {
            this.scheduleTick();
        }
    }

    private void scheduleTick() {
        Block block = this.getBlockState().getBlock();
        if (block instanceof CommandBlock) {
            this.markConditionMet();
            this.level.scheduleTick(this.worldPosition, block, 1);
        }
    }

    public boolean wasConditionMet() {
        return this.conditionMet;
    }

    public boolean markConditionMet() {
        this.conditionMet = true;
        if (this.isConditional()) {
            BlockPos blockpos = this.worldPosition.relative(this.level.getBlockState(this.worldPosition).getValue(CommandBlock.FACING).getOpposite());
            if (this.level.getBlockState(blockpos).getBlock() instanceof CommandBlock) {
                BlockEntity blockentity = this.level.getBlockEntity(blockpos);
                this.conditionMet = blockentity instanceof CommandBlockEntity && ((CommandBlockEntity)blockentity).getCommandBlock().getSuccessCount() > 0;
            } else {
                this.conditionMet = false;
            }
        }

        return this.conditionMet;
    }

    public CommandBlockEntity.Mode getMode() {
        BlockState blockstate = this.getBlockState();
        if (blockstate.is(Blocks.COMMAND_BLOCK)) {
            return CommandBlockEntity.Mode.REDSTONE;
        } else if (blockstate.is(Blocks.REPEATING_COMMAND_BLOCK)) {
            return CommandBlockEntity.Mode.AUTO;
        } else {
            return blockstate.is(Blocks.CHAIN_COMMAND_BLOCK) ? CommandBlockEntity.Mode.SEQUENCE : CommandBlockEntity.Mode.REDSTONE;
        }
    }

    public boolean isConditional() {
        BlockState blockstate = this.level.getBlockState(this.getBlockPos());
        return blockstate.getBlock() instanceof CommandBlock ? blockstate.getValue(CommandBlock.CONDITIONAL) : false;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_397908_) {
        super.applyImplicitComponents(p_397908_);
        this.commandBlock.setCustomName(p_397908_.get(DataComponents.CUSTOM_NAME));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_329197_) {
        super.collectImplicitComponents(p_329197_);
        p_329197_.set(DataComponents.CUSTOM_NAME, this.commandBlock.getCustomName());
    }

    @Override
    public void removeComponentsFromTag(ValueOutput p_408069_) {
        super.removeComponentsFromTag(p_408069_);
        p_408069_.discard("CustomName");
        p_408069_.discard("conditionMet");
        p_408069_.discard("powered");
    }

    public static enum Mode {
        SEQUENCE,
        AUTO,
        REDSTONE;
    }
}