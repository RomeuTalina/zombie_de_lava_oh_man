package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsWorldOptions extends ValueObject implements ReflectionBasedSerialization {
    @SerializedName("pvp")
    public boolean pvp = true;
    @SerializedName("spawnMonsters")
    public boolean spawnMonsters = true;
    @SerializedName("spawnProtection")
    public int spawnProtection = 0;
    @SerializedName("commandBlocks")
    public boolean commandBlocks = false;
    @SerializedName("forceGameMode")
    public boolean forceGameMode = false;
    @SerializedName("difficulty")
    public int difficulty = 2;
    @SerializedName("gameMode")
    public int gameMode = 0;
    @SerializedName("slotName")
    private String slotName = "";
    @SerializedName("version")
    public String version = "";
    @SerializedName("compatibility")
    public RealmsServer.Compatibility compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
    @SerializedName("worldTemplateId")
    public long templateId = -1L;
    @Nullable
    @SerializedName("worldTemplateImage")
    public String templateImage = null;
    public boolean empty;

    private RealmsWorldOptions() {
    }

    public RealmsWorldOptions(
        boolean pPvp,
        boolean pSpawnMonsters,
        int pSpawnProtection,
        boolean pCommandBlocks,
        int pDifficulty,
        int pGameMode,
        boolean pForceGameMode,
        String pSlotName,
        String pVersion,
        RealmsServer.Compatibility pCompatibility
    ) {
        this.pvp = pPvp;
        this.spawnMonsters = pSpawnMonsters;
        this.spawnProtection = pSpawnProtection;
        this.commandBlocks = pCommandBlocks;
        this.difficulty = pDifficulty;
        this.gameMode = pGameMode;
        this.forceGameMode = pForceGameMode;
        this.slotName = pSlotName;
        this.version = pVersion;
        this.compatibility = pCompatibility;
    }

    public static RealmsWorldOptions createDefaults() {
        return new RealmsWorldOptions();
    }

    public static RealmsWorldOptions createDefaultsWith(
        GameType pGameType, boolean pCommandBlocks, Difficulty pDifficulty, boolean pHardcore, String pVersion, String pSlotName
    ) {
        RealmsWorldOptions realmsworldoptions = createDefaults();
        realmsworldoptions.commandBlocks = pCommandBlocks;
        realmsworldoptions.difficulty = pDifficulty.getId();
        realmsworldoptions.gameMode = pGameType.getId();
        realmsworldoptions.slotName = pSlotName;
        realmsworldoptions.version = pVersion;
        return realmsworldoptions;
    }

    public static RealmsWorldOptions createFromSettings(LevelSettings pLevelSettings, boolean pCommandBlocks, String pVersion) {
        return createDefaultsWith(pLevelSettings.gameType(), pCommandBlocks, pLevelSettings.difficulty(), pLevelSettings.hardcore(), pVersion, pLevelSettings.levelName());
    }

    public static RealmsWorldOptions createEmptyDefaults() {
        RealmsWorldOptions realmsworldoptions = createDefaults();
        realmsworldoptions.setEmpty(true);
        return realmsworldoptions;
    }

    public void setEmpty(boolean pEmpty) {
        this.empty = pEmpty;
    }

    public static RealmsWorldOptions parse(GuardedSerializer pSerializer, String pJson) {
        RealmsWorldOptions realmsworldoptions = pSerializer.fromJson(pJson, RealmsWorldOptions.class);
        if (realmsworldoptions == null) {
            return createDefaults();
        } else {
            finalize(realmsworldoptions);
            return realmsworldoptions;
        }
    }

    private static void finalize(RealmsWorldOptions pOptions) {
        if (pOptions.slotName == null) {
            pOptions.slotName = "";
        }

        if (pOptions.version == null) {
            pOptions.version = "";
        }

        if (pOptions.compatibility == null) {
            pOptions.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
        }
    }

    public String getSlotName(int pSlotIndex) {
        if (StringUtil.isBlank(this.slotName)) {
            return this.empty ? I18n.get("mco.configure.world.slot.empty") : this.getDefaultSlotName(pSlotIndex);
        } else {
            return this.slotName;
        }
    }

    public String getDefaultSlotName(int pSlotIndex) {
        return I18n.get("mco.configure.world.slot", pSlotIndex);
    }

    public RealmsWorldOptions clone() {
        return new RealmsWorldOptions(
            this.pvp,
            this.spawnMonsters,
            this.spawnProtection,
            this.commandBlocks,
            this.difficulty,
            this.gameMode,
            this.forceGameMode,
            this.slotName,
            this.version,
            this.compatibility
        );
    }
}