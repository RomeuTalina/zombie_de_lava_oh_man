package net.minecraft.client.gui.screens;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import java.net.URI;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.ScreenNarrationCollector;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class Screen extends AbstractContainerEventHandler implements Renderable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component USAGE_NARRATION = Component.translatable("narrator.screen.usage");
    public static final ResourceLocation MENU_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_background.png");
    public static final ResourceLocation HEADER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/header_separator.png");
    public static final ResourceLocation FOOTER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/footer_separator.png");
    private static final ResourceLocation INWORLD_MENU_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_background.png");
    public static final ResourceLocation INWORLD_HEADER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/inworld_header_separator.png");
    public static final ResourceLocation INWORLD_FOOTER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/inworld_footer_separator.png");
    protected static final float FADE_IN_TIME = 2000.0F;
    protected final Component title;
    private final List<GuiEventListener> children = Lists.newArrayList();
    private final List<NarratableEntry> narratables = Lists.newArrayList();
    @Nullable
    protected Minecraft minecraft;
    private boolean initialized;
    public int width;
    public int height;
    public final List<Renderable> renderables = Lists.newArrayList();
    protected Font font;
    private static final long NARRATE_SUPPRESS_AFTER_INIT_TIME = TimeUnit.SECONDS.toMillis(2L);
    private static final long NARRATE_DELAY_NARRATOR_ENABLED = NARRATE_SUPPRESS_AFTER_INIT_TIME;
    private static final long NARRATE_DELAY_MOUSE_MOVE = 750L;
    private static final long NARRATE_DELAY_MOUSE_ACTION = 200L;
    private static final long NARRATE_DELAY_KEYBOARD_ACTION = 200L;
    private final ScreenNarrationCollector narrationState = new ScreenNarrationCollector();
    private long narrationSuppressTime = Long.MIN_VALUE;
    private long nextNarrationTime = Long.MAX_VALUE;
    @Nullable
    protected CycleButton<NarratorStatus> narratorButton;
    @Nullable
    private NarratableEntry lastNarratable;
    protected final Executor screenExecutor = p_289626_ -> this.minecraft.execute(() -> {
        if (this.minecraft.screen == this) {
            p_289626_.run();
        }
    });

    protected Screen(Component pTitle) {
        this.title = pTitle;
    }

    public Component getTitle() {
        return this.title;
    }

    public Component getNarrationMessage() {
        return this.getTitle();
    }

    public final void renderWithTooltip(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.nextStratum();
        this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.nextStratum();
        this.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.renderDeferredTooltip();
    }

    @Override
    public void render(GuiGraphics p_281549_, int p_281550_, int p_282878_, float p_282465_) {
        for (Renderable renderable : this.renderables) {
            renderable.render(p_281549_, p_281550_, p_282878_, p_282465_);
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256 && this.shouldCloseOnEsc()) {
            this.onClose();
            return true;
        } else if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        } else {
            FocusNavigationEvent focusnavigationevent = (FocusNavigationEvent)(switch (pKeyCode) {
                case 258 -> this.createTabEvent();
                default -> null;
                case 262 -> this.createArrowEvent(ScreenDirection.RIGHT);
                case 263 -> this.createArrowEvent(ScreenDirection.LEFT);
                case 264 -> this.createArrowEvent(ScreenDirection.DOWN);
                case 265 -> this.createArrowEvent(ScreenDirection.UP);
            });
            if (focusnavigationevent != null) {
                ComponentPath componentpath = super.nextFocusPath(focusnavigationevent);
                if (componentpath == null && focusnavigationevent instanceof FocusNavigationEvent.TabNavigation) {
                    this.clearFocus();
                    componentpath = super.nextFocusPath(focusnavigationevent);
                }

                if (componentpath != null) {
                    this.changeFocus(componentpath);
                }
            }

            return false;
        }
    }

    private FocusNavigationEvent.TabNavigation createTabEvent() {
        boolean flag = !hasShiftDown();
        return new FocusNavigationEvent.TabNavigation(flag);
    }

    private FocusNavigationEvent.ArrowNavigation createArrowEvent(ScreenDirection pDirection) {
        return new FocusNavigationEvent.ArrowNavigation(pDirection);
    }

    protected void setInitialFocus() {
        if (this.minecraft.getLastInputType().isKeyboard()) {
            FocusNavigationEvent.TabNavigation focusnavigationevent$tabnavigation = new FocusNavigationEvent.TabNavigation(true);
            ComponentPath componentpath = super.nextFocusPath(focusnavigationevent$tabnavigation);
            if (componentpath != null) {
                this.changeFocus(componentpath);
            }
        }
    }

    protected void setInitialFocus(GuiEventListener pListener) {
        ComponentPath componentpath = ComponentPath.path(this, pListener.nextFocusPath(new FocusNavigationEvent.InitialFocus()));
        if (componentpath != null) {
            this.changeFocus(componentpath);
        }
    }

    public void clearFocus() {
        ComponentPath componentpath = this.getCurrentFocusPath();
        if (componentpath != null) {
            componentpath.applyFocus(false);
        }
    }

    @VisibleForTesting
    protected void changeFocus(ComponentPath pPath) {
        this.clearFocus();
        pPath.applyFocus(true);
    }

    public boolean shouldCloseOnEsc() {
        return true;
    }

    public void onClose() {
        this.minecraft.popGuiLayer();
    }

    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T pWidget) {
        this.renderables.add(pWidget);
        return this.addWidget(pWidget);
    }

    protected <T extends Renderable> T addRenderableOnly(T pRenderable) {
        this.renderables.add(pRenderable);
        return pRenderable;
    }

    protected <T extends GuiEventListener & NarratableEntry> T addWidget(T pListener) {
        this.children.add(pListener);
        this.narratables.add(pListener);
        return pListener;
    }

    protected void removeWidget(GuiEventListener pListener) {
        if (pListener instanceof Renderable) {
            this.renderables.remove((Renderable)pListener);
        }

        if (pListener instanceof NarratableEntry) {
            this.narratables.remove((NarratableEntry)pListener);
        }

        this.children.remove(pListener);
    }

    protected void clearWidgets() {
        this.renderables.clear();
        this.children.clear();
        this.narratables.clear();
    }

    public static List<Component> getTooltipFromItem(Minecraft pMinecraft, ItemStack pItem) {
        return pItem.getTooltipLines(
            Item.TooltipContext.of(pMinecraft.level),
            pMinecraft.player,
            pMinecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
        );
    }

    protected void insertText(String pText, boolean pOverwrite) {
    }

    public boolean handleComponentClicked(Style pStyle) {
        ClickEvent clickevent = pStyle.getClickEvent();
        if (hasShiftDown()) {
            if (pStyle.getInsertion() != null) {
                this.insertText(pStyle.getInsertion(), false);
            }
        } else if (clickevent != null) {
            this.handleClickEvent(this.minecraft, clickevent);
            return true;
        }

        return false;
    }

    protected void handleClickEvent(Minecraft pMinecraft, ClickEvent pClickEvent) {
        defaultHandleGameClickEvent(pClickEvent, pMinecraft, this);
    }

    protected static void defaultHandleGameClickEvent(ClickEvent pClickEvent, Minecraft pMinecraft, @Nullable Screen pScreen) {
        LocalPlayer localplayer = Objects.requireNonNull(pMinecraft.player, "Player not available");
        switch (pClickEvent) {
            case ClickEvent.RunCommand(String s):
                clickCommandAction(localplayer, s, pScreen);
                break;
            case ClickEvent.ShowDialog clickevent$showdialog:
                localplayer.connection.showDialog(clickevent$showdialog.dialog(), pScreen);
                break;
            case ClickEvent.Custom clickevent$custom:
                localplayer.connection.send(new ServerboundCustomClickActionPacket(clickevent$custom.id(), clickevent$custom.payload()));
                if (pMinecraft.screen != pScreen) {
                    pMinecraft.setScreen(pScreen);
                }
                break;
            default:
                defaultHandleClickEvent(pClickEvent, pMinecraft, pScreen);
        }
    }

    protected static void defaultHandleClickEvent(ClickEvent pClickEvent, Minecraft pMinecraft, @Nullable Screen pScreen) {
        boolean flag = switch (pClickEvent) {
            case ClickEvent.OpenUrl(URI uri) -> {
                clickUrlAction(pMinecraft, pScreen, uri);
                yield false;
            }
            case ClickEvent.OpenFile clickevent$openfile -> {
                Util.getPlatform().openFile(clickevent$openfile.file());
                yield true;
            }
            case ClickEvent.SuggestCommand(String s2) -> {
                String s1 = s2;
                if (pScreen != null) {
                    pScreen.insertText(s1, true);
                }

                yield true;
            }
            case ClickEvent.CopyToClipboard(String s) -> {
                pMinecraft.keyboardHandler.setClipboard(s);
                yield true;
            }
            default -> {
                LOGGER.error("Don't know how to handle {}", pClickEvent);
                yield true;
            }
        };
        if (flag && pMinecraft.screen != pScreen) {
            pMinecraft.setScreen(pScreen);
        }
    }

    protected static boolean clickUrlAction(Minecraft pMinecraft, @Nullable Screen pScreen, URI pUrl) {
        if (!pMinecraft.options.chatLinks().get()) {
            return false;
        } else {
            if (pMinecraft.options.chatLinksPrompt().get()) {
                pMinecraft.setScreen(new ConfirmLinkScreen(p_404851_ -> {
                    if (p_404851_) {
                        Util.getPlatform().openUri(pUrl);
                    }

                    pMinecraft.setScreen(pScreen);
                }, pUrl.toString(), false));
            } else {
                Util.getPlatform().openUri(pUrl);
            }

            return true;
        }
    }

    protected static void clickCommandAction(LocalPlayer pPlayer, String pCommand, @Nullable Screen pScreen) {
        pPlayer.connection.sendUnattendedCommand(Commands.trimOptionalPrefix(pCommand), pScreen);
    }

    public final void init(Minecraft pMinecraft, int pWidth, int pHeight) {
        this.minecraft = pMinecraft;
        this.font = pMinecraft.font;
        this.width = pWidth;
        this.height = pHeight;
        if (!this.initialized) {
            if (!net.minecraftforge.client.event.ScreenEvent.Init.Pre.BUS.post(new net.minecraftforge.client.event.ScreenEvent.Init.Pre(this, this.children, this::addEventWidget, this::removeWidget)))
            this.init();
            this.setInitialFocus();
            net.minecraftforge.client.event.ScreenEvent.Init.Post.BUS.post(new net.minecraftforge.client.event.ScreenEvent.Init.Post(this, this.children, this::addEventWidget, this::removeWidget));
        } else {
            this.repositionElements();
        }

        this.initialized = true;
        this.triggerImmediateNarration(false);
        this.suppressNarration(NARRATE_SUPPRESS_AFTER_INIT_TIME);
    }

    protected void rebuildWidgets() {
        this.clearWidgets();
        this.clearFocus();
        if (!net.minecraftforge.client.event.ScreenEvent.Init.Pre.BUS.post(new net.minecraftforge.client.event.ScreenEvent.Init.Pre(this, this.children, this::addEventWidget, this::removeWidget)))
        this.init();
        this.setInitialFocus();
        net.minecraftforge.client.event.ScreenEvent.Init.Post.BUS.post(new net.minecraftforge.client.event.ScreenEvent.Init.Post(this, this.children, this::addEventWidget, this::removeWidget));
    }

    protected void fadeWidgets(float pFade) {
        for (GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener instanceof AbstractWidget abstractwidget) {
                abstractwidget.setAlpha(pFade);
            }
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    protected void init() {
    }

    public void tick() {
    }

    public void removed() {
    }

    public void added() {
    }

    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.minecraft.level == null) {
            this.renderPanorama(pGuiGraphics, pPartialTick);
        }

        this.renderBlurredBackground(pGuiGraphics);
        this.renderMenuBackground(pGuiGraphics);
        net.minecraftforge.client.event.ForgeEventFactoryClient.onRenderScreenBackground(this, pGuiGraphics);
    }

    protected void renderBlurredBackground(GuiGraphics pGuiGraphics) {
        float f = this.minecraft.options.getMenuBackgroundBlurriness();
        if (f >= 1.0F) {
            pGuiGraphics.blurBeforeThisStratum();
        }
    }

    protected void renderPanorama(GuiGraphics pGuiGraphics, float pPartialTick) {
        this.minecraft.gameRenderer.getPanorama().render(pGuiGraphics, this.width, this.height, true);
    }

    protected void renderMenuBackground(GuiGraphics pPartialTick) {
        this.renderMenuBackground(pPartialTick, 0, 0, this.width, this.height);
    }

    protected void renderMenuBackground(GuiGraphics pGuiGraphics, int pX, int pY, int pWidth, int pHeight) {
        renderMenuBackgroundTexture(pGuiGraphics, this.minecraft.level == null ? MENU_BACKGROUND : INWORLD_MENU_BACKGROUND, pX, pY, 0.0F, 0.0F, pWidth, pHeight);
    }

    public static void renderMenuBackgroundTexture(
        GuiGraphics pGuiGraphics, ResourceLocation pTexture, int pX, int pY, float pUOffset, float pVOffset, int pWidth, int pHeight
    ) {
        int i = 32;
        pGuiGraphics.blit(RenderPipelines.GUI_TEXTURED, pTexture, pX, pY, pUOffset, pVOffset, pWidth, pHeight, 32, 32);
    }

    public void renderTransparentBackground(GuiGraphics pGuiGraphics) {
        pGuiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
    }

    public boolean isPauseScreen() {
        return true;
    }

    public static boolean hasControlDown() {
        return Minecraft.ON_OSX
            ? InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 343)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 347)
            : InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 341)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 345);
    }

    public static boolean hasShiftDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 340)
            || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 344);
    }

    public static boolean hasAltDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 342)
            || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 346);
    }

    public static boolean isCut(int pKeyCode) {
        return pKeyCode == 88 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    public static boolean isPaste(int pKeyCode) {
        return pKeyCode == 86 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    public static boolean isCopy(int pKeyCode) {
        return pKeyCode == 67 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    public static boolean isSelectAll(int pKeyCode) {
        return pKeyCode == 65 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    protected void repositionElements() {
        this.rebuildWidgets();
    }

    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        this.width = pWidth;
        this.height = pHeight;
        this.repositionElements();
    }

    public void fillCrashDetails(CrashReport pCrashReport) {
        CrashReportCategory crashreportcategory = pCrashReport.addCategory("Affected screen", 1);
        crashreportcategory.setDetail("Screen name", () -> this.getClass().getCanonicalName());
    }

    protected boolean isValidCharacterForName(String pText, char pCharTyped, int pCursorPos) {
        int i = pText.indexOf(58);
        int j = pText.indexOf(47);
        if (pCharTyped == ':') {
            return (j == -1 || pCursorPos <= j) && i == -1;
        } else {
            return pCharTyped == '/'
                ? pCursorPos > i
                : pCharTyped == '_' || pCharTyped == '-' || pCharTyped >= 'a' && pCharTyped <= 'z' || pCharTyped >= '0' && pCharTyped <= '9' || pCharTyped == '.';
        }
    }

    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return true;
    }

    public void onFilesDrop(List<Path> pPacks) {
    }

    @Nullable
    public Minecraft getMinecraft() {
        return minecraft;
    }

    private void addEventWidget(GuiEventListener guiEventListener) {
        if (guiEventListener instanceof Renderable r)
            this.renderables.add(r);
        if (guiEventListener instanceof NarratableEntry ne)
            this.narratables.add(ne);
        this.children.add(guiEventListener);
    }

    private void scheduleNarration(long pDelay, boolean pStopSuppression) {
        this.nextNarrationTime = Util.getMillis() + pDelay;
        if (pStopSuppression) {
            this.narrationSuppressTime = Long.MIN_VALUE;
        }
    }

    private void suppressNarration(long pTime) {
        this.narrationSuppressTime = Util.getMillis() + pTime;
    }

    public void afterMouseMove() {
        this.scheduleNarration(750L, false);
    }

    public void afterMouseAction() {
        this.scheduleNarration(200L, true);
    }

    public void afterKeyboardAction() {
        this.scheduleNarration(200L, true);
    }

    private boolean shouldRunNarration() {
        return this.minecraft.getNarrator().isActive();
    }

    public void handleDelayedNarration() {
        if (this.shouldRunNarration()) {
            long i = Util.getMillis();
            if (i > this.nextNarrationTime && i > this.narrationSuppressTime) {
                this.runNarration(true);
                this.nextNarrationTime = Long.MAX_VALUE;
            }
        }
    }

    public void triggerImmediateNarration(boolean pOnlyNarrateNew) {
        if (this.shouldRunNarration()) {
            this.runNarration(pOnlyNarrateNew);
        }
    }

    private void runNarration(boolean pOnlyNarrateNew) {
        this.narrationState.update(this::updateNarrationState);
        String s = this.narrationState.collectNarrationText(!pOnlyNarrateNew);
        if (!s.isEmpty()) {
            this.minecraft.getNarrator().saySystemNow(s);
        }
    }

    protected boolean shouldNarrateNavigation() {
        return true;
    }

    protected void updateNarrationState(NarrationElementOutput pOutput) {
        pOutput.add(NarratedElementType.TITLE, this.getNarrationMessage());
        if (this.shouldNarrateNavigation()) {
            pOutput.add(NarratedElementType.USAGE, USAGE_NARRATION);
        }

        this.updateNarratedWidget(pOutput);
    }

    protected void updateNarratedWidget(NarrationElementOutput pNarrationElementOutput) {
        List<? extends NarratableEntry> list = this.narratables
            .stream()
            .flatMap(p_374575_ -> p_374575_.getNarratables().stream())
            .filter(NarratableEntry::isActive)
            .sorted(Comparator.comparingInt(TabOrderedElement::getTabOrderGroup))
            .toList();
        Screen.NarratableSearchResult screen$narratablesearchresult = findNarratableWidget(list, this.lastNarratable);
        if (screen$narratablesearchresult != null) {
            if (screen$narratablesearchresult.priority.isTerminal()) {
                this.lastNarratable = screen$narratablesearchresult.entry;
            }

            if (list.size() > 1) {
                pNarrationElementOutput.add(
                    NarratedElementType.POSITION, Component.translatable("narrator.position.screen", screen$narratablesearchresult.index + 1, list.size())
                );
                if (screen$narratablesearchresult.priority == NarratableEntry.NarrationPriority.FOCUSED) {
                    pNarrationElementOutput.add(NarratedElementType.USAGE, this.getUsageNarration());
                }
            }

            screen$narratablesearchresult.entry.updateNarration(pNarrationElementOutput.nest());
        }
    }

    protected Component getUsageNarration() {
        return Component.translatable("narration.component_list.usage");
    }

    @Nullable
    public static Screen.NarratableSearchResult findNarratableWidget(List<? extends NarratableEntry> pEntries, @Nullable NarratableEntry pTarget) {
        Screen.NarratableSearchResult screen$narratablesearchresult = null;
        Screen.NarratableSearchResult screen$narratablesearchresult1 = null;
        int i = 0;

        for (int j = pEntries.size(); i < j; i++) {
            NarratableEntry narratableentry = pEntries.get(i);
            NarratableEntry.NarrationPriority narratableentry$narrationpriority = narratableentry.narrationPriority();
            if (narratableentry$narrationpriority.isTerminal()) {
                if (narratableentry != pTarget) {
                    return new Screen.NarratableSearchResult(narratableentry, i, narratableentry$narrationpriority);
                }

                screen$narratablesearchresult1 = new Screen.NarratableSearchResult(narratableentry, i, narratableentry$narrationpriority);
            } else if (narratableentry$narrationpriority.compareTo(
                    screen$narratablesearchresult != null ? screen$narratablesearchresult.priority : NarratableEntry.NarrationPriority.NONE
                )
                > 0) {
                screen$narratablesearchresult = new Screen.NarratableSearchResult(narratableentry, i, narratableentry$narrationpriority);
            }
        }

        return screen$narratablesearchresult != null ? screen$narratablesearchresult : screen$narratablesearchresult1;
    }

    public void updateNarratorStatus(boolean pNarratorEnabled) {
        if (pNarratorEnabled) {
            this.scheduleNarration(NARRATE_DELAY_NARRATOR_ENABLED, false);
        }

        if (this.narratorButton != null) {
            this.narratorButton.setValue(this.minecraft.options.narrator().get());
        }
    }

    public Font getFont() {
        return this.font;
    }

    public boolean showsActiveEffects() {
        return false;
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(0, 0, this.width, this.height);
    }

    @Nullable
    public Music getBackgroundMusic() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static class NarratableSearchResult {
        public final NarratableEntry entry;
        public final int index;
        public final NarratableEntry.NarrationPriority priority;

        public NarratableSearchResult(NarratableEntry pEntry, int pIndex, NarratableEntry.NarrationPriority pPriority) {
            this.entry = pEntry;
            this.index = pIndex;
            this.priority = pPriority;
        }
    }
}
