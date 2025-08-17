package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.PreferredRegionsDto;
import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RegionDataDto;
import com.mojang.realmsclient.dto.RegionSelectionPreference;
import com.mojang.realmsclient.dto.RegionSelectionPreferenceDto;
import com.mojang.realmsclient.dto.ServiceQuality;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.CloseServerTask;
import com.mojang.realmsclient.util.task.OpenServerTask;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.LoadingTab;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsConfigureWorldScreen extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
    private final RealmsMainScreen lastScreen;
    @Nullable
    private RealmsServer serverData;
    @Nullable
    private PreferredRegionsDto regions;
    private final Map<RealmsRegion, ServiceQuality> regionServiceQuality = new LinkedHashMap<>();
    private final long serverId;
    private boolean stateChanged;
    private final TabManager tabManager = new TabManager(p_406538_ -> {
        AbstractWidget abstractwidget = this.addRenderableWidget(p_406538_);
    }, p_407853_ -> this.removeWidget(p_407853_), this::onTabSelected, this::onTabDeselected);
    @Nullable
    private Button playButton;
    @Nullable
    private TabNavigationBar tabNavigationBar;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public RealmsConfigureWorldScreen(RealmsMainScreen pLastScreen, long pServerId, @Nullable RealmsServer pServerData, @Nullable PreferredRegionsDto pRegions) {
        super(Component.empty());
        this.lastScreen = pLastScreen;
        this.serverId = pServerId;
        this.serverData = pServerData;
        this.regions = pRegions;
    }

    public RealmsConfigureWorldScreen(RealmsMainScreen pLastScreen, long pServerId) {
        this(pLastScreen, pServerId, null, null);
    }

    @Override
    public void init() {
        if (this.serverData == null) {
            this.fetchServerData(this.serverId);
        }

        if (this.regions == null) {
            this.fetchRegionData();
        }

        Component component = Component.translatable("mco.configure.world.loading");
        this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
            .addTabs(
                new LoadingTab(this.getFont(), RealmsWorldsTab.TITLE, component),
                new LoadingTab(this.getFont(), RealmsPlayersTab.TITLE, component),
                new LoadingTab(this.getFont(), RealmsSubscriptionTab.TITLE, component),
                new LoadingTab(this.getFont(), RealmsSettingsTab.TITLE, component)
            )
            .build();
        this.addRenderableWidget(this.tabNavigationBar);
        LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.playButton = linearlayout.addChild(Button.builder(PLAY_TEXT, p_410021_ -> {
            this.onClose();
            RealmsMainScreen.play(this.serverData, this);
        }).width(150).build());
        this.playButton.active = false;
        linearlayout.addChild(Button.builder(CommonComponents.GUI_BACK, p_408656_ -> this.onClose()).build());
        this.layout.visitWidgets(p_408073_ -> {
            p_408073_.setTabOrderGroup(1);
            this.addRenderableWidget(p_408073_);
        });
        this.tabNavigationBar.selectTab(0, false);
        this.repositionElements();
        if (this.serverData != null && this.regions != null) {
            this.onRealmsDataFetched();
        }
    }

    private void onTabSelected(Tab pTab) {
        if (this.serverData != null && pTab instanceof RealmsConfigurationTab realmsconfigurationtab) {
            realmsconfigurationtab.onSelected(this.serverData);
        }
    }

    private void onTabDeselected(Tab pTab) {
        if (this.serverData != null && pTab instanceof RealmsConfigurationTab realmsconfigurationtab) {
            realmsconfigurationtab.onDeselected(this.serverData);
        }
    }

    public int getContentHeight() {
        return this.layout.getContentHeight();
    }

    public int getHeaderHeight() {
        return this.layout.getHeaderHeight();
    }

    public Screen getLastScreen() {
        return this.lastScreen;
    }

    public Screen createErrorScreen(RealmsServiceException pException) {
        return new RealmsGenericErrorScreen(pException, this.lastScreen);
    }

    @Override
    public void repositionElements() {
        if (this.tabNavigationBar != null) {
            this.tabNavigationBar.setWidth(this.width);
            this.tabNavigationBar.arrangeElements();
            int i = this.tabNavigationBar.getRectangle().bottom();
            ScreenRectangle screenrectangle = new ScreenRectangle(0, i, this.width, this.height - this.layout.getFooterHeight() - i);
            this.tabManager.setTabArea(screenrectangle);
            this.layout.setHeaderHeight(i);
            this.layout.arrangeElements();
        }
    }

    private void updateButtonStates() {
        if (this.serverData != null && this.playButton != null) {
            this.playButton.active = this.serverData.shouldPlayButtonBeActive();
            if (!this.playButton.active && this.serverData.state == RealmsServer.State.CLOSED) {
                this.playButton.setTooltip(Tooltip.create(RealmsServer.WORLD_CLOSED_COMPONENT));
            }
        }
    }

    @Override
    public void render(GuiGraphics p_406719_, int p_410627_, int p_410049_, float p_408215_) {
        super.render(p_406719_, p_410627_, p_410049_, p_408215_);
        p_406719_.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
    }

    @Override
    public boolean keyPressed(int p_408663_, int p_409997_, int p_410477_) {
        return this.tabNavigationBar.keyPressed(p_408663_) ? true : super.keyPressed(p_408663_, p_409997_, p_410477_);
    }

    @Override
    protected void renderMenuBackground(GuiGraphics p_406523_) {
        p_406523_.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
        this.renderMenuBackground(p_406523_, 0, this.layout.getHeaderHeight(), this.width, this.height);
    }

    @Override
    public void onClose() {
        if (this.serverData != null && this.tabManager.getCurrentTab() instanceof RealmsConfigurationTab realmsconfigurationtab) {
            realmsconfigurationtab.onDeselected(this.serverData);
        }

        this.minecraft.setScreen(this.lastScreen);
        if (this.stateChanged) {
            this.lastScreen.resetScreen();
        }
    }

    public void fetchRegionData() {
        RealmsUtil.supplyAsync(RealmsClient::getPreferredRegionSelections, RealmsUtil.openScreenAndLogOnFailure(this::createErrorScreen, "Couldn't get realms region data")).thenAcceptAsync(p_407784_ -> {
            this.regions = p_407784_;
            this.onRealmsDataFetched();
        }, this.minecraft);
    }

    public void fetchServerData(long pServerId) {
        RealmsUtil.<RealmsServer>supplyAsync(p_410599_ -> p_410599_.getOwnRealm(pServerId), RealmsUtil.openScreenAndLogOnFailure(this::createErrorScreen, "Couldn't get own world"))
            .thenAcceptAsync(p_407133_ -> {
                this.serverData = p_407133_;
                this.onRealmsDataFetched();
            }, this.minecraft);
    }

    private void onRealmsDataFetched() {
        if (this.serverData != null && this.regions != null) {
            this.regionServiceQuality.clear();

            for (RegionDataDto regiondatadto : this.regions.regionData()) {
                if (regiondatadto.region() != RealmsRegion.INVALID_REGION) {
                    this.regionServiceQuality.put(regiondatadto.region(), regiondatadto.serviceQuality());
                }
            }

            if (this.tabNavigationBar != null) {
                this.removeWidget(this.tabNavigationBar);
            }

            this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
                .addTabs(
                    new RealmsWorldsTab(this, Objects.requireNonNull(this.minecraft), this.serverData),
                    new RealmsPlayersTab(this, this.minecraft, this.serverData),
                    new RealmsSubscriptionTab(this, this.minecraft, this.serverData),
                    new RealmsSettingsTab(this, this.minecraft, this.serverData, this.regionServiceQuality)
                )
                .build();
            this.addRenderableWidget(this.tabNavigationBar);
            this.tabNavigationBar.selectTab(0, false);
            this.tabNavigationBar.setTabActiveState(3, !this.serverData.expired);
            if (this.serverData.expired) {
                this.tabNavigationBar.setTabTooltip(3, Tooltip.create(Component.translatable("mco.configure.world.settings.expired")));
            } else {
                this.tabNavigationBar.setTabTooltip(3, null);
            }

            this.updateButtonStates();
            this.repositionElements();
        }
    }

    public void saveSlotSettings(RealmsSlot pSlot) {
        RealmsSlot realmsslot = this.serverData.slots.get(this.serverData.activeSlot);
        pSlot.options.templateId = realmsslot.options.templateId;
        pSlot.options.templateImage = realmsslot.options.templateImage;
        RealmsClient realmsclient = RealmsClient.getOrCreate();

        try {
            if (this.serverData.activeSlot != pSlot.slotId) {
                throw new RealmsServiceException(RealmsError.CustomError.configurationError());
            }

            realmsclient.updateSlot(this.serverData.id, pSlot.slotId, pSlot.options, pSlot.settings);
            this.serverData.slots.put(this.serverData.activeSlot, pSlot);
            if (pSlot.options.gameMode != realmsslot.options.gameMode || pSlot.isHardcore() != realmsslot.isHardcore()) {
                RealmsMainScreen.refreshServerList();
            }

            this.stateChanged();
        } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't save slot settings", (Throwable)realmsserviceexception);
            this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this));
            return;
        }

        this.minecraft.setScreen(this);
    }

    public void saveSettings(String pName, String pDescription, RegionSelectionPreference pRegionSelectionPreference, @Nullable RealmsRegion pPreferredRegiom) {
        String s = StringUtil.isBlank(pDescription) ? "" : pDescription;
        String s1 = StringUtil.isBlank(pName) ? "" : pName;
        RealmsClient realmsclient = RealmsClient.getOrCreate();

        try {
            RealmsSlot realmsslot = this.serverData.slots.get(this.serverData.activeSlot);
            RealmsRegion realmsregion = pRegionSelectionPreference == RegionSelectionPreference.MANUAL ? pPreferredRegiom : null;
            RegionSelectionPreferenceDto regionselectionpreferencedto = new RegionSelectionPreferenceDto(pRegionSelectionPreference, realmsregion);
            realmsclient.updateConfiguration(
                this.serverData.id, s1, s, regionselectionpreferencedto, realmsslot.slotId, realmsslot.options, realmsslot.settings
            );
            this.serverData.regionSelectionPreference = regionselectionpreferencedto;
            this.serverData.name = pName;
            this.serverData.motd = s;
            this.stateChanged();
        } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't save settings", (Throwable)realmsserviceexception);
            this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this));
            return;
        }

        this.minecraft.setScreen(this);
    }

    public void openTheWorld(boolean pJoin) {
        RealmsConfigureWorldScreen realmsconfigureworldscreen = this.getNewScreenWithKnownData(this.serverData);
        this.minecraft
            .setScreen(
                new RealmsLongRunningMcoTaskScreen(this.getNewScreen(), new OpenServerTask(this.serverData, realmsconfigureworldscreen, pJoin, this.minecraft))
            );
    }

    public void closeTheWorld() {
        RealmsConfigureWorldScreen realmsconfigureworldscreen = this.getNewScreenWithKnownData(this.serverData);
        this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.getNewScreen(), new CloseServerTask(this.serverData, realmsconfigureworldscreen)));
    }

    public void stateChanged() {
        this.stateChanged = true;
        if (this.tabNavigationBar != null) {
            for (Tab tab : this.tabNavigationBar.getTabs()) {
                if (tab instanceof RealmsConfigurationTab realmsconfigurationtab) {
                    realmsconfigurationtab.updateData(this.serverData);
                }
            }
        }
    }

    public boolean invitePlayer(long pWorldId, String pPlayerName) {
        RealmsClient realmsclient = RealmsClient.getOrCreate();

        try {
            List<PlayerInfo> list = realmsclient.invite(pWorldId, pPlayerName);
            if (this.serverData != null) {
                this.serverData.players = list;
            } else {
                this.serverData = realmsclient.getOwnRealm(pWorldId);
            }

            this.stateChanged();
            return true;
        } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't invite user", (Throwable)realmsserviceexception);
            return false;
        }
    }

    public RealmsConfigureWorldScreen getNewScreen() {
        RealmsConfigureWorldScreen realmsconfigureworldscreen = new RealmsConfigureWorldScreen(this.lastScreen, this.serverId);
        realmsconfigureworldscreen.stateChanged = this.stateChanged;
        return realmsconfigureworldscreen;
    }

    public RealmsConfigureWorldScreen getNewScreenWithKnownData(RealmsServer pServer) {
        RealmsConfigureWorldScreen realmsconfigureworldscreen = new RealmsConfigureWorldScreen(this.lastScreen, this.serverId, pServer, this.regions);
        realmsconfigureworldscreen.stateChanged = this.stateChanged;
        return realmsconfigureworldscreen;
    }
}