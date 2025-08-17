package com.mojang.realmsclient.gui.screens.configuration;

import com.google.common.collect.Lists;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.screens.RealmsResetWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsSelectWorldTemplateScreen;
import com.mojang.realmsclient.util.task.SwitchMinigameTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
class RealmsWorldsTab extends GridLayoutTab implements RealmsConfigurationTab {
    static final Component TITLE = Component.translatable("mco.configure.worlds.title");
    private final RealmsConfigureWorldScreen configurationScreen;
    private final Minecraft minecraft;
    private RealmsServer serverData;
    private final Button optionsButton;
    private final Button backupButton;
    private final Button resetWorldButton;
    private final List<RealmsWorldSlotButton> slotButtonList = Lists.newArrayList();

    RealmsWorldsTab(RealmsConfigureWorldScreen pConfigurationScreen, Minecraft pMinecraft, RealmsServer pServerData) {
        super(TITLE);
        this.configurationScreen = pConfigurationScreen;
        this.minecraft = pMinecraft;
        this.serverData = pServerData;
        GridLayout.RowHelper gridlayout$rowhelper = this.layout.spacing(20).createRowHelper(1);
        GridLayout.RowHelper gridlayout$rowhelper1 = new GridLayout().spacing(16).createRowHelper(4);
        this.slotButtonList.clear();

        for (int i = 1; i < 5; i++) {
            this.slotButtonList.add(gridlayout$rowhelper1.addChild(this.createSlotButton(i), LayoutSettings.defaults().alignVerticallyBottom()));
        }

        gridlayout$rowhelper.addChild(gridlayout$rowhelper1.getGrid());
        GridLayout.RowHelper gridlayout$rowhelper2 = new GridLayout().spacing(8).createRowHelper(1);
        this.optionsButton = gridlayout$rowhelper2.addChild(
            Button.builder(
                    Component.translatable("mco.configure.world.buttons.options"),
                    p_409068_ -> pMinecraft.setScreen(
                        new RealmsSlotOptionsScreen(pConfigurationScreen, pServerData.slots.get(pServerData.activeSlot).clone(), pServerData.worldType, pServerData.activeSlot)
                    )
                )
                .bounds(0, 0, 150, 20)
                .build()
        );
        this.backupButton = gridlayout$rowhelper2.addChild(
            Button.builder(
                    Component.translatable("mco.configure.world.backup"),
                    p_408278_ -> pMinecraft.setScreen(new RealmsBackupScreen(pConfigurationScreen, pServerData.clone(), pServerData.activeSlot))
                )
                .bounds(0, 0, 150, 20)
                .build()
        );
        this.resetWorldButton = gridlayout$rowhelper2.addChild(
            Button.builder(Component.empty(), p_410267_ -> this.resetButtonPressed()).bounds(0, 0, 150, 20).build()
        );
        gridlayout$rowhelper.addChild(gridlayout$rowhelper2.getGrid(), LayoutSettings.defaults().alignHorizontallyCenter());
        this.backupButton.active = true;
        this.updateData(pServerData);
    }

    private void resetButtonPressed() {
        if (this.isMinigame()) {
            this.minecraft
                .setScreen(
                    new RealmsSelectWorldTemplateScreen(Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME)
                );
        } else {
            this.minecraft
                .setScreen(
                    RealmsResetWorldScreen.forResetSlot(
                        this.configurationScreen, this.serverData.clone(), () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.configurationScreen.getNewScreen()))
                    )
                );
        }
    }

    private void templateSelectionCallback(@Nullable WorldTemplate pTemplate) {
        if (pTemplate != null && WorldTemplate.WorldTemplateType.MINIGAME == pTemplate.type) {
            this.configurationScreen.stateChanged();
            RealmsConfigureWorldScreen realmsconfigureworldscreen = this.configurationScreen.getNewScreen();
            this.minecraft
                .setScreen(
                    new RealmsLongRunningMcoTaskScreen(
                        realmsconfigureworldscreen, new SwitchMinigameTask(this.serverData.id, pTemplate, realmsconfigureworldscreen)
                    )
                );
        } else {
            this.minecraft.setScreen(this.configurationScreen);
        }
    }

    private boolean isMinigame() {
        return this.serverData.isMinigameActive();
    }

    @Override
    public void onSelected(RealmsServer p_408580_) {
        this.updateData(p_408580_);
    }

    @Override
    public void updateData(RealmsServer p_408768_) {
        this.serverData = p_408768_;
        this.optionsButton.active = !p_408768_.expired && !this.isMinigame();
        this.resetWorldButton.active = !p_408768_.expired;
        if (this.isMinigame()) {
            this.resetWorldButton.setMessage(Component.translatable("mco.configure.world.buttons.switchminigame"));
        } else {
            boolean flag = p_408768_.slots.containsKey(p_408768_.activeSlot) && p_408768_.slots.get(p_408768_.activeSlot).options.empty;
            if (flag) {
                this.resetWorldButton.setMessage(Component.translatable("mco.configure.world.buttons.newworld"));
            } else {
                this.resetWorldButton.setMessage(Component.translatable("mco.configure.world.buttons.resetworld"));
            }
        }

        this.backupButton.active = !this.isMinigame();

        for (RealmsWorldSlotButton realmsworldslotbutton : this.slotButtonList) {
            RealmsWorldSlotButton.State realmsworldslotbutton$state = realmsworldslotbutton.setServerData(p_408768_);
            if (realmsworldslotbutton$state.activeSlot) {
                realmsworldslotbutton.setSize(80, 80);
            } else {
                realmsworldslotbutton.setSize(50, 50);
            }
        }
    }

    private RealmsWorldSlotButton createSlotButton(int pSlotIndex) {
        return new RealmsWorldSlotButton(0, 0, 80, 80, pSlotIndex, this.serverData, p_406823_ -> {
            RealmsWorldSlotButton.State realmsworldslotbutton$state = ((RealmsWorldSlotButton)p_406823_).getState();
            switch (realmsworldslotbutton$state.action) {
                case SWITCH_SLOT:
                    if (realmsworldslotbutton$state.minigame) {
                        this.switchToMinigame();
                    } else if (realmsworldslotbutton$state.empty) {
                        this.switchToEmptySlot(pSlotIndex, this.serverData);
                    } else {
                        this.switchToFullSlot(pSlotIndex, this.serverData);
                    }
                case NOTHING:
                    return;
                default:
                    throw new IllegalStateException("Unknown action " + realmsworldslotbutton$state.action);
            }
        });
    }

    private void switchToMinigame() {
        RealmsSelectWorldTemplateScreen realmsselectworldtemplatescreen = new RealmsSelectWorldTemplateScreen(
            Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME
        );
        realmsselectworldtemplatescreen.setWarning(Component.translatable("mco.minigame.world.info.line1"), Component.translatable("mco.minigame.world.info.line2"));
        this.minecraft.setScreen(realmsselectworldtemplatescreen);
    }

    private void switchToFullSlot(int pSlotIndex, RealmsServer pServerData) {
        this.minecraft
            .setScreen(
                RealmsPopups.infoPopupScreen(
                    this.configurationScreen,
                    Component.translatable("mco.configure.world.slot.switch.question.line1"),
                    p_409812_ -> {
                        RealmsConfigureWorldScreen realmsconfigureworldscreen = this.configurationScreen.getNewScreen();
                        this.configurationScreen.stateChanged();
                        this.minecraft
                            .setScreen(
                                new RealmsLongRunningMcoTaskScreen(
                                    realmsconfigureworldscreen,
                                    new SwitchSlotTask(
                                        pServerData.id, pSlotIndex, () -> this.minecraft.execute(() -> this.minecraft.setScreen(realmsconfigureworldscreen))
                                    )
                                )
                            );
                    }
                )
            );
    }

    private void switchToEmptySlot(int pSlotIndex, RealmsServer pServerData) {
        this.minecraft
            .setScreen(
                RealmsPopups.infoPopupScreen(
                    this.configurationScreen,
                    Component.translatable("mco.configure.world.slot.switch.question.line1"),
                    p_406450_ -> {
                        this.configurationScreen.stateChanged();
                        RealmsResetWorldScreen realmsresetworldscreen = RealmsResetWorldScreen.forEmptySlot(
                            this.configurationScreen, pSlotIndex, pServerData, () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.configurationScreen.getNewScreen()))
                        );
                        this.minecraft.setScreen(realmsresetworldscreen);
                    }
                )
            );
    }
}