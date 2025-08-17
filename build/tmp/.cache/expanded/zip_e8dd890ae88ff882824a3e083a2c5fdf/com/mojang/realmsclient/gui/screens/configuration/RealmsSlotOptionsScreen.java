package com.mojang.realmsclient.gui.screens.configuration;

import com.google.common.collect.ImmutableList;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsLabel;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsSlotOptionsScreen extends RealmsScreen {
    private static final int DEFAULT_DIFFICULTY = 2;
    public static final List<Difficulty> DIFFICULTIES = ImmutableList.of(Difficulty.PEACEFUL, Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD);
    private static final int DEFAULT_GAME_MODE = 0;
    public static final List<GameType> GAME_MODES = ImmutableList.of(GameType.SURVIVAL, GameType.CREATIVE, GameType.ADVENTURE);
    private static final Component NAME_LABEL = Component.translatable("mco.configure.world.edit.slot.name");
    static final Component SPAWN_PROTECTION_TEXT = Component.translatable("mco.configure.world.spawnProtection");
    private EditBox nameEdit;
    protected final RealmsConfigureWorldScreen parentScreen;
    private int column1X;
    private int columnWidth;
    private final RealmsSlot slot;
    private final RealmsServer.WorldType worldType;
    private Difficulty difficulty;
    private GameType gameMode;
    private final String defaultSlotName;
    private String worldName;
    private boolean pvp;
    private boolean spawnMonsters;
    int spawnProtection;
    private boolean commandBlocks;
    private boolean forceGameMode;
    RealmsSlotOptionsScreen.SettingsSlider spawnProtectionButton;

    public RealmsSlotOptionsScreen(RealmsConfigureWorldScreen pParentScreen, RealmsSlot pSlot, RealmsServer.WorldType pWorldType, int pSlotIndex) {
        super(Component.translatable("mco.configure.world.buttons.options"));
        this.parentScreen = pParentScreen;
        this.slot = pSlot;
        this.worldType = pWorldType;
        this.difficulty = findByIndex(DIFFICULTIES, pSlot.options.difficulty, 2);
        this.gameMode = findByIndex(GAME_MODES, pSlot.options.gameMode, 0);
        this.defaultSlotName = pSlot.options.getDefaultSlotName(pSlotIndex);
        this.setWorldName(pSlot.options.getSlotName(pSlotIndex));
        if (pWorldType == RealmsServer.WorldType.NORMAL) {
            this.pvp = pSlot.options.pvp;
            this.spawnProtection = pSlot.options.spawnProtection;
            this.forceGameMode = pSlot.options.forceGameMode;
            this.spawnMonsters = pSlot.options.spawnMonsters;
            this.commandBlocks = pSlot.options.commandBlocks;
        } else {
            this.pvp = true;
            this.spawnProtection = 0;
            this.forceGameMode = false;
            this.spawnMonsters = true;
            this.commandBlocks = true;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }

    private static <T> T findByIndex(List<T> pValues, int pIndex, int pDefaultIndex) {
        try {
            return pValues.get(pIndex);
        } catch (IndexOutOfBoundsException indexoutofboundsexception) {
            return pValues.get(pDefaultIndex);
        }
    }

    private static <T> int findIndex(List<T> pValues, T pItem, int pDefaultIndex) {
        int i = pValues.indexOf(pItem);
        return i == -1 ? pDefaultIndex : i;
    }

    @Override
    public void init() {
        this.columnWidth = 170;
        this.column1X = this.width / 2 - this.columnWidth;
        int i = this.width / 2 + 10;
        if (this.worldType != RealmsServer.WorldType.NORMAL) {
            Component component;
            if (this.worldType == RealmsServer.WorldType.ADVENTUREMAP) {
                component = Component.translatable("mco.configure.world.edit.subscreen.adventuremap");
            } else if (this.worldType == RealmsServer.WorldType.INSPIRATION) {
                component = Component.translatable("mco.configure.world.edit.subscreen.inspiration");
            } else {
                component = Component.translatable("mco.configure.world.edit.subscreen.experience");
            }

            this.addLabel(new RealmsLabel(component, this.width / 2, 26, -65536));
        }

        this.nameEdit = this.addWidget(
            new EditBox(
                this.minecraft.font, this.column1X, row(1), this.columnWidth, 20, null, Component.translatable("mco.configure.world.edit.slot.name")
            )
        );
        this.nameEdit.setValue(this.worldName);
        this.nameEdit.setResponder(this::setWorldName);
        CycleButton<Boolean> cyclebutton5 = this.addRenderableWidget(
            CycleButton.onOffBuilder(this.pvp)
                .create(
                    i, row(1), this.columnWidth, 20, Component.translatable("mco.configure.world.pvp"), (p_406996_, p_409786_) -> this.pvp = p_409786_
                )
        );
        CycleButton<GameType> cyclebutton = this.addRenderableWidget(
            CycleButton.builder(GameType::getShortDisplayName)
                .withValues(GAME_MODES)
                .withInitialValue(this.gameMode)
                .create(
                    this.column1X,
                    row(3),
                    this.columnWidth,
                    20,
                    Component.translatable("selectWorld.gameMode"),
                    (p_409637_, p_408624_) -> this.gameMode = p_408624_
                )
        );
        this.spawnProtectionButton = this.addRenderableWidget(new RealmsSlotOptionsScreen.SettingsSlider(i, row(3), this.columnWidth, this.spawnProtection, 0.0F, 16.0F));
        Component component1 = Component.translatable("mco.configure.world.spawn_toggle.message");
        CycleButton<Boolean> cyclebutton1 = CycleButton.onOffBuilder(this.difficulty != Difficulty.PEACEFUL && this.spawnMonsters)
            .create(
                i,
                row(5),
                this.columnWidth,
                20,
                Component.translatable("mco.configure.world.spawnMonsters"),
                this.confirmDangerousOption(component1, p_407619_ -> this.spawnMonsters = p_407619_)
            );
        CycleButton<Difficulty> cyclebutton2 = this.addRenderableWidget(
            CycleButton.builder(Difficulty::getDisplayName)
                .withValues(DIFFICULTIES)
                .withInitialValue(this.difficulty)
                .create(this.column1X, row(5), this.columnWidth, 20, Component.translatable("options.difficulty"), (p_407662_, p_409388_) -> {
                    this.difficulty = p_409388_;
                    if (this.worldType == RealmsServer.WorldType.NORMAL) {
                        boolean flag = this.difficulty != Difficulty.PEACEFUL;
                        cyclebutton1.active = flag;
                        cyclebutton1.setValue(flag && this.spawnMonsters);
                    }
                })
        );
        this.addRenderableWidget(cyclebutton1);
        CycleButton<Boolean> cyclebutton3 = this.addRenderableWidget(
            CycleButton.onOffBuilder(this.forceGameMode)
                .create(
                    this.column1X,
                    row(7),
                    this.columnWidth,
                    20,
                    Component.translatable("mco.configure.world.forceGameMode"),
                    (p_407915_, p_409942_) -> this.forceGameMode = p_409942_
                )
        );
        CycleButton<Boolean> cyclebutton4 = this.addRenderableWidget(
            CycleButton.onOffBuilder(this.commandBlocks)
                .create(
                    i,
                    row(7),
                    this.columnWidth,
                    20,
                    Component.translatable("mco.configure.world.commandBlocks"),
                    (p_409614_, p_410525_) -> this.commandBlocks = p_410525_
                )
        );
        if (this.worldType != RealmsServer.WorldType.NORMAL) {
            cyclebutton5.active = false;
            cyclebutton1.active = false;
            this.spawnProtectionButton.active = false;
            cyclebutton3.active = false;
        }

        if (this.difficulty == Difficulty.PEACEFUL) {
            cyclebutton1.active = false;
        }

        if (this.slot.isHardcore()) {
            cyclebutton4.active = false;
            cyclebutton2.active = false;
            cyclebutton.active = false;
            cyclebutton1.active = false;
            cyclebutton3.active = false;
        }

        this.addRenderableWidget(
            Button.builder(Component.translatable("mco.configure.world.buttons.done"), p_407224_ -> this.saveSettings())
                .bounds(this.column1X, row(13), this.columnWidth, 20)
                .build()
        );
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, p_406277_ -> this.onClose()).bounds(i, row(13), this.columnWidth, 20).build());
    }

    private CycleButton.OnValueChange<Boolean> confirmDangerousOption(Component pText, Consumer<Boolean> pCallback) {
        return (p_408603_, p_407022_) -> {
            if (p_407022_) {
                pCallback.accept(true);
            } else {
                this.minecraft.setScreen(RealmsPopups.warningPopupScreen(this, pText, p_407700_ -> {
                    pCallback.accept(false);
                    p_407700_.onClose();
                }));
            }
        };
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(this.getTitle(), this.createLabelNarration());
    }

    @Override
    public void render(GuiGraphics p_406931_, int p_409237_, int p_405986_, float p_408960_) {
        super.render(p_406931_, p_409237_, p_405986_, p_408960_);
        p_406931_.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
        p_406931_.drawString(this.font, NAME_LABEL, this.column1X + this.columnWidth / 2 - this.font.width(NAME_LABEL) / 2, row(0) - 5, -1);
        this.nameEdit.render(p_406931_, p_409237_, p_405986_, p_408960_);
    }

    private void setWorldName(String pWorldName) {
        if (pWorldName.equals(this.defaultSlotName)) {
            this.worldName = "";
        } else {
            this.worldName = pWorldName;
        }
    }

    private void saveSettings() {
        int i = findIndex(DIFFICULTIES, this.difficulty, 2);
        int j = findIndex(GAME_MODES, this.gameMode, 0);
        if (this.worldType != RealmsServer.WorldType.ADVENTUREMAP
            && this.worldType != RealmsServer.WorldType.EXPERIENCE
            && this.worldType != RealmsServer.WorldType.INSPIRATION) {
            boolean flag = this.worldType == RealmsServer.WorldType.NORMAL && this.difficulty != Difficulty.PEACEFUL && this.spawnMonsters;
            this.parentScreen
                .saveSlotSettings(
                    new RealmsSlot(
                        this.slot.slotId,
                        new RealmsWorldOptions(
                            this.pvp,
                            flag,
                            this.spawnProtection,
                            this.commandBlocks,
                            i,
                            j,
                            this.forceGameMode,
                            this.worldName,
                            this.slot.options.version,
                            this.slot.options.compatibility
                        ),
                        this.slot.settings
                    )
                );
        } else {
            this.parentScreen
                .saveSlotSettings(
                    new RealmsSlot(
                        this.slot.slotId,
                        new RealmsWorldOptions(
                            this.slot.options.pvp,
                            this.slot.options.spawnMonsters,
                            this.slot.options.spawnProtection,
                            this.slot.options.commandBlocks,
                            i,
                            j,
                            this.slot.options.forceGameMode,
                            this.worldName,
                            this.slot.options.version,
                            this.slot.options.compatibility
                        ),
                        this.slot.settings
                    )
                );
        }
    }

    @OnlyIn(Dist.CLIENT)
    class SettingsSlider extends AbstractSliderButton {
        private final double minValue;
        private final double maxValue;

        public SettingsSlider(final int pX, final int pY, final int pWidth, final int pValue, final float pMinValue, final float pMaxValue) {
            super(pX, pY, pWidth, 20, CommonComponents.EMPTY, 0.0);
            this.minValue = pMinValue;
            this.maxValue = pMaxValue;
            this.value = (Mth.clamp(pValue, pMinValue, pMaxValue) - pMinValue) / (pMaxValue - pMinValue);
            this.updateMessage();
        }

        @Override
        public void applyValue() {
            if (RealmsSlotOptionsScreen.this.spawnProtectionButton.active) {
                RealmsSlotOptionsScreen.this.spawnProtection = (int)Mth.lerp(Mth.clamp(this.value, 0.0, 1.0), this.minValue, this.maxValue);
            }
        }

        @Override
        protected void updateMessage() {
            this.setMessage(
                CommonComponents.optionNameValue(
                    RealmsSlotOptionsScreen.SPAWN_PROTECTION_TEXT,
                    (Component)(RealmsSlotOptionsScreen.this.spawnProtection == 0
                        ? CommonComponents.OPTION_OFF
                        : Component.literal(String.valueOf(RealmsSlotOptionsScreen.this.spawnProtection)))
                )
            );
        }
    }
}