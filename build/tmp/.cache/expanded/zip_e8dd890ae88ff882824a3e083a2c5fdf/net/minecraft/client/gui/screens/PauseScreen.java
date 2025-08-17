package net.minecraft.client.gui.screens;

import com.mojang.realmsclient.RealmsMainScreen;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.NowPlayingToast;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.Dialogs;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DialogTags;
import net.minecraft.util.CommonLinks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PauseScreen extends Screen {
    private static final ResourceLocation DRAFT_REPORT_SPRITE = ResourceLocation.withDefaultNamespace("icon/draft_report");
    private static final int COLUMNS = 2;
    private static final int MENU_PADDING_TOP = 50;
    private static final int BUTTON_PADDING = 4;
    private static final int BUTTON_WIDTH_FULL = 204;
    private static final int BUTTON_WIDTH_HALF = 98;
    private static final Component RETURN_TO_GAME = Component.translatable("menu.returnToGame");
    private static final Component ADVANCEMENTS = Component.translatable("gui.advancements");
    private static final Component STATS = Component.translatable("gui.stats");
    private static final Component SEND_FEEDBACK = Component.translatable("menu.sendFeedback");
    private static final Component REPORT_BUGS = Component.translatable("menu.reportBugs");
    private static final Component FEEDBACK_SUBSCREEN = Component.translatable("menu.feedback");
    private static final Component OPTIONS = Component.translatable("menu.options");
    private static final Component SHARE_TO_LAN = Component.translatable("menu.shareToLan");
    private static final Component PLAYER_REPORTING = Component.translatable("menu.playerReporting");
    private static final Component GAME = Component.translatable("menu.game");
    private static final Component PAUSED = Component.translatable("menu.paused");
    private static final Tooltip CUSTOM_OPTIONS_TOOLTIP = Tooltip.create(Component.translatable("menu.custom_options.tooltip"));
    private final boolean showPauseMenu;
    @Nullable
    private Button disconnectButton;

    public PauseScreen(boolean pShowPauseMenu) {
        super(pShowPauseMenu ? GAME : PAUSED);
        this.showPauseMenu = pShowPauseMenu;
    }

    public boolean showsPauseMenu() {
        return this.showPauseMenu;
    }

    @Override
    protected void init() {
        if (this.showPauseMenu) {
            this.createPauseMenu();
        }

        this.addRenderableWidget(new StringWidget(0, this.showPauseMenu ? 40 : 10, this.width, 9, this.title, this.font));
    }

    private void createPauseMenu() {
        GridLayout gridlayout = new GridLayout();
        gridlayout.defaultCellSetting().padding(4, 4, 4, 0);
        GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(2);
        gridlayout$rowhelper.addChild(Button.builder(RETURN_TO_GAME, p_280814_ -> {
            this.minecraft.setScreen(null);
            this.minecraft.mouseHandler.grabMouse();
        }).width(204).build(), 2, gridlayout.newCellSettings().paddingTop(50));
        gridlayout$rowhelper.addChild(this.openScreenButton(ADVANCEMENTS, () -> new AdvancementsScreen(this.minecraft.player.connection.getAdvancements(), this)));
        gridlayout$rowhelper.addChild(this.openScreenButton(STATS, () -> new StatsScreen(this, this.minecraft.player.getStats())));
        Optional<? extends Holder<Dialog>> optional = this.getCustomAdditions();
        if (optional.isEmpty()) {
            addFeedbackButtons(this, gridlayout$rowhelper);
        } else {
            this.addFeedbackSubscreenAndCustomDialogButtons(this.minecraft, (Holder<Dialog>)optional.get(), gridlayout$rowhelper);
        }

        gridlayout$rowhelper.addChild(this.openScreenButton(OPTIONS, () -> new OptionsScreen(this, this.minecraft.options)));
        if (this.minecraft.hasSingleplayerServer() && !this.minecraft.getSingleplayerServer().isPublished()) {
            gridlayout$rowhelper.addChild(this.openScreenButton(SHARE_TO_LAN, () -> new ShareToLanScreen(this)));
        } else {
            gridlayout$rowhelper.addChild(this.openScreenButton(PLAYER_REPORTING, () -> new SocialInteractionsScreen(this)));
        }
        gridlayout$rowhelper.addChild(Button.builder(Component.translatable("fml.menu.mods"), button -> this.minecraft.setScreen(new net.minecraftforge.client.gui.ModListScreen(this))).width(BUTTON_WIDTH_FULL).build(), 2);

        this.disconnectButton = gridlayout$rowhelper.addChild(Button.builder(CommonComponents.disconnectButtonLabel(this.minecraft.isLocalServer()), p_280815_ -> {
            p_280815_.active = false;
            this.minecraft.getReportingContext().draftReportHandled(this.minecraft, this, () -> disconnectFromWorld(this.minecraft, ClientLevel.DEFAULT_QUIT_MESSAGE), true);
        }).width(204).build(), 2);
        gridlayout.arrangeElements();
        FrameLayout.alignInRectangle(gridlayout, 0, 0, this.width, this.height, 0.5F, 0.25F);
        gridlayout.visitWidgets(this::addRenderableWidget);
    }

    private Optional<? extends Holder<Dialog>> getCustomAdditions() {
        Registry<Dialog> registry = this.minecraft.player.connection.registryAccess().lookupOrThrow(Registries.DIALOG);
        Optional<? extends HolderSet<Dialog>> optional = registry.get(DialogTags.PAUSE_SCREEN_ADDITIONS);
        if (optional.isPresent()) {
            HolderSet<Dialog> holderset = (HolderSet<Dialog>)optional.get();
            if (holderset.size() > 0) {
                if (holderset.size() == 1) {
                    return Optional.of(holderset.get(0));
                }

                return registry.get(Dialogs.CUSTOM_OPTIONS);
            }
        }

        ServerLinks serverlinks = this.minecraft.player.connection.serverLinks();
        return !serverlinks.isEmpty() ? registry.get(Dialogs.SERVER_LINKS) : Optional.empty();
    }

    static void addFeedbackButtons(Screen pLastScreen, GridLayout.RowHelper pRowHelper) {
        pRowHelper.addChild(openLinkButton(pLastScreen, SEND_FEEDBACK, SharedConstants.getCurrentVersion().stable() ? CommonLinks.RELEASE_FEEDBACK : CommonLinks.SNAPSHOT_FEEDBACK));
        pRowHelper.addChild(openLinkButton(pLastScreen, REPORT_BUGS, CommonLinks.SNAPSHOT_BUGS_FEEDBACK)).active = !SharedConstants.getCurrentVersion().dataVersion().isSideSeries();
    }

    private void addFeedbackSubscreenAndCustomDialogButtons(Minecraft pMinecraft, Holder<Dialog> pCustomDialog, GridLayout.RowHelper pRowHelper) {
        pRowHelper.addChild(this.openScreenButton(FEEDBACK_SUBSCREEN, () -> new PauseScreen.FeedbackSubScreen(this)));
        pRowHelper.addChild(
            Button.builder(pCustomDialog.value().common().computeExternalTitle(), p_404847_ -> pMinecraft.player.connection.showDialog(pCustomDialog, this))
                .width(98)
                .tooltip(CUSTOM_OPTIONS_TOOLTIP)
                .build()
        );
    }

    public static void disconnectFromWorld(Minecraft pMinecraft, Component pReason) {
        boolean flag = pMinecraft.isLocalServer();
        ServerData serverdata = pMinecraft.getCurrentServer();
        if (pMinecraft.level != null) {
            pMinecraft.level.disconnect(pReason);
        }

        if (flag) {
            pMinecraft.disconnectWithSavingScreen();
        } else {
            pMinecraft.disconnectWithProgressScreen();
        }

        TitleScreen titlescreen = new TitleScreen();
        if (flag) {
            pMinecraft.setScreen(titlescreen);
        } else if (serverdata != null && serverdata.isRealm()) {
            pMinecraft.setScreen(new RealmsMainScreen(titlescreen));
        } else {
            pMinecraft.setScreen(new JoinMultiplayerScreen(titlescreen));
        }
    }

    @Override
    public void tick() {
        if (this.rendersNowPlayingToast()) {
            NowPlayingToast.tickMusicNotes();
        }
    }

    @Override
    public void render(GuiGraphics p_281899_, int p_281431_, int p_283183_, float p_281435_) {
        super.render(p_281899_, p_281431_, p_283183_, p_281435_);
        if (this.rendersNowPlayingToast()) {
            NowPlayingToast.renderToast(p_281899_, this.font);
        }

        if (this.showPauseMenu && this.minecraft != null && this.minecraft.getReportingContext().hasDraftReport() && this.disconnectButton != null) {
            p_281899_.blitSprite(
                RenderPipelines.GUI_TEXTURED, DRAFT_REPORT_SPRITE, this.disconnectButton.getX() + this.disconnectButton.getWidth() - 17, this.disconnectButton.getY() + 3, 15, 15
            );
        }
    }

    @Override
    public void renderBackground(GuiGraphics p_299656_, int p_297892_, int p_299995_, float p_300532_) {
        if (this.showPauseMenu) {
            super.renderBackground(p_299656_, p_297892_, p_299995_, p_300532_);
        }
    }

    public boolean rendersNowPlayingToast() {
        Options options = this.minecraft.options;
        return options.showNowPlayingToast().get() && options.getFinalSoundSourceVolume(SoundSource.MUSIC) > 0.0F && this.showPauseMenu;
    }

    private Button openScreenButton(Component pMessage, Supplier<Screen> pScreenSupplier) {
        return Button.builder(pMessage, p_280817_ -> this.minecraft.setScreen(pScreenSupplier.get())).width(98).build();
    }

    private static Button openLinkButton(Screen pLastScreen, Component pButtonText, URI pUri) {
        return Button.builder(pButtonText, ConfirmLinkScreen.confirmLink(pLastScreen, pUri)).width(98).build();
    }

    @OnlyIn(Dist.CLIENT)
    static class FeedbackSubScreen extends Screen {
        private static final Component TITLE = Component.translatable("menu.feedback.title");
        public final Screen parent;
        private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

        protected FeedbackSubScreen(Screen pParent) {
            super(TITLE);
            this.parent = pParent;
        }

        @Override
        protected void init() {
            this.layout.addTitleHeader(TITLE, this.font);
            GridLayout gridlayout = this.layout.addToContents(new GridLayout());
            gridlayout.defaultCellSetting().padding(4, 4, 4, 0);
            GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(2);
            PauseScreen.addFeedbackButtons(this, gridlayout$rowhelper);
            this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, p_342489_ -> this.onClose()).width(200).build());
            this.layout.visitWidgets(this::addRenderableWidget);
            this.repositionElements();
        }

        @Override
        protected void repositionElements() {
            this.layout.arrangeElements();
        }

        @Override
        public void onClose() {
            this.minecraft.setScreen(this.parent);
        }
    }
}
