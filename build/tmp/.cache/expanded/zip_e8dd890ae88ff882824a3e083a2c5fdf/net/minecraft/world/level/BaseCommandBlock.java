package net.minecraft.world.level;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public abstract class BaseCommandBlock implements CommandSource {
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final Component DEFAULT_NAME = Component.literal("@");
    private static final int NO_LAST_EXECUTION = -1;
    private long lastExecution = -1L;
    private boolean updateLastExecution = true;
    private int successCount;
    private boolean trackOutput = true;
    @Nullable
    private Component lastOutput;
    private String command = "";
    @Nullable
    private Component customName;

    public int getSuccessCount() {
        return this.successCount;
    }

    public void setSuccessCount(int pSuccessCount) {
        this.successCount = pSuccessCount;
    }

    public Component getLastOutput() {
        return this.lastOutput == null ? CommonComponents.EMPTY : this.lastOutput;
    }

    public void save(ValueOutput pOutput) {
        pOutput.putString("Command", this.command);
        pOutput.putInt("SuccessCount", this.successCount);
        pOutput.storeNullable("CustomName", ComponentSerialization.CODEC, this.customName);
        pOutput.putBoolean("TrackOutput", this.trackOutput);
        if (this.trackOutput) {
            pOutput.storeNullable("LastOutput", ComponentSerialization.CODEC, this.lastOutput);
        }

        pOutput.putBoolean("UpdateLastExecution", this.updateLastExecution);
        if (this.updateLastExecution && this.lastExecution != -1L) {
            pOutput.putLong("LastExecution", this.lastExecution);
        }
    }

    public void load(ValueInput pInput) {
        this.command = pInput.getStringOr("Command", "");
        this.successCount = pInput.getIntOr("SuccessCount", 0);
        this.setCustomName(BlockEntity.parseCustomNameSafe(pInput, "CustomName"));
        this.trackOutput = pInput.getBooleanOr("TrackOutput", true);
        if (this.trackOutput) {
            this.lastOutput = BlockEntity.parseCustomNameSafe(pInput, "LastOutput");
        } else {
            this.lastOutput = null;
        }

        this.updateLastExecution = pInput.getBooleanOr("UpdateLastExecution", true);
        if (this.updateLastExecution) {
            this.lastExecution = pInput.getLongOr("LastExecution", -1L);
        } else {
            this.lastExecution = -1L;
        }
    }

    public void setCommand(String pCommand) {
        this.command = pCommand;
        this.successCount = 0;
    }

    public String getCommand() {
        return this.command;
    }

    public boolean performCommand(Level pLevel) {
        if (pLevel.isClientSide || pLevel.getGameTime() == this.lastExecution) {
            return false;
        } else if ("Searge".equalsIgnoreCase(this.command)) {
            this.lastOutput = Component.literal("#itzlipofutzli");
            this.successCount = 1;
            return true;
        } else {
            this.successCount = 0;
            MinecraftServer minecraftserver = this.getLevel().getServer();
            if (minecraftserver.isCommandBlockEnabled() && !StringUtil.isNullOrEmpty(this.command)) {
                try {
                    this.lastOutput = null;
                    CommandSourceStack commandsourcestack = this.createCommandSourceStack().withCallback((p_45418_, p_45419_) -> {
                        if (p_45418_) {
                            this.successCount++;
                        }
                    });
                    minecraftserver.getCommands().performPrefixedCommand(commandsourcestack, this.command);
                } catch (Throwable throwable) {
                    CrashReport crashreport = CrashReport.forThrowable(throwable, "Executing command block");
                    CrashReportCategory crashreportcategory = crashreport.addCategory("Command to be executed");
                    crashreportcategory.setDetail("Command", this::getCommand);
                    crashreportcategory.setDetail("Name", () -> this.getName().getString());
                    throw new ReportedException(crashreport);
                }
            }

            if (this.updateLastExecution) {
                this.lastExecution = pLevel.getGameTime();
            } else {
                this.lastExecution = -1L;
            }

            return true;
        }
    }

    public Component getName() {
        return this.customName != null ? this.customName : DEFAULT_NAME;
    }

    @Nullable
    public Component getCustomName() {
        return this.customName;
    }

    public void setCustomName(@Nullable Component pCustomName) {
        this.customName = pCustomName;
    }

    @Override
    public void sendSystemMessage(Component p_220330_) {
        if (this.trackOutput) {
            this.lastOutput = Component.literal("[" + TIME_FORMAT.format(new Date()) + "] ").append(p_220330_);
            this.onUpdated();
        }
    }

    public abstract ServerLevel getLevel();

    public abstract void onUpdated();

    public void setLastOutput(@Nullable Component pLastOutputMessage) {
        this.lastOutput = pLastOutputMessage;
    }

    public void setTrackOutput(boolean pShouldTrackOutput) {
        this.trackOutput = pShouldTrackOutput;
    }

    public boolean isTrackOutput() {
        return this.trackOutput;
    }

    public InteractionResult usedBy(Player pPlayer) {
        if (!pPlayer.canUseGameMasterBlocks()) {
            return InteractionResult.PASS;
        } else {
            if (pPlayer.level().isClientSide) {
                pPlayer.openMinecartCommandBlock(this);
            }

            return InteractionResult.SUCCESS;
        }
    }

    public abstract Vec3 getPosition();

    public abstract CommandSourceStack createCommandSourceStack();

    @Override
    public boolean acceptsSuccess() {
        return this.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK) && this.trackOutput;
    }

    @Override
    public boolean acceptsFailure() {
        return this.trackOutput;
    }

    @Override
    public boolean shouldInformAdmins() {
        return this.getLevel().getGameRules().getBoolean(GameRules.RULE_COMMANDBLOCKOUTPUT);
    }

    public abstract boolean isValid();
}