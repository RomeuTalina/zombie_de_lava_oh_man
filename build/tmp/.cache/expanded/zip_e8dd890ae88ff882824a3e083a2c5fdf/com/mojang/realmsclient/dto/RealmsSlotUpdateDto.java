package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RealmsSlotUpdateDto implements ReflectionBasedSerialization {
    @SerializedName("slotId")
    public final int slotId;
    @SerializedName("pvp")
    private final boolean pvp;
    @SerializedName("spawnMonsters")
    private final boolean spawnMonsters;
    @SerializedName("spawnProtection")
    private final int spawnProtection;
    @SerializedName("commandBlocks")
    private final boolean commandBlocks;
    @SerializedName("forceGameMode")
    private final boolean forceGameMode;
    @SerializedName("difficulty")
    private final int difficulty;
    @SerializedName("gameMode")
    private final int gameMode;
    @SerializedName("slotName")
    private final String slotName;
    @SerializedName("version")
    private final String version;
    @SerializedName("compatibility")
    private final RealmsServer.Compatibility compatibility;
    @SerializedName("worldTemplateId")
    private final long templateId;
    @Nullable
    @SerializedName("worldTemplateImage")
    private final String templateImage;
    @SerializedName("hardcore")
    private final boolean hardcore;

    public RealmsSlotUpdateDto(int pSlotId, RealmsWorldOptions pOptions, boolean pHardcore) {
        this.slotId = pSlotId;
        this.pvp = pOptions.pvp;
        this.spawnMonsters = pOptions.spawnMonsters;
        this.spawnProtection = pOptions.spawnProtection;
        this.commandBlocks = pOptions.commandBlocks;
        this.forceGameMode = pOptions.forceGameMode;
        this.difficulty = pOptions.difficulty;
        this.gameMode = pOptions.gameMode;
        this.slotName = pOptions.getSlotName(pSlotId);
        this.version = pOptions.version;
        this.compatibility = pOptions.compatibility;
        this.templateId = pOptions.templateId;
        this.templateImage = pOptions.templateImage;
        this.hardcore = pHardcore;
    }
}