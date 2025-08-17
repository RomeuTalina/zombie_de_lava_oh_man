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

    protected Screen(Component p_96550_) {
        this.title = p_96550_;
    }

    public Component getTitle() {
        return this.title;
    }

    public Component getNarrationMessage() {
        return this.getTitle();
    }

    public final void renderWithTooltip(GuiGraphics p_282345_, int p_283456_, int p_283586_, float p_282339_) {
        p_282345_.nextStratum();
        this.renderBackground(p_282345_, p_283456_, p_283586_, p_282339_);
        p_282345_.nextStratum();
        this.render(p_282345_, p_283456_, p_283586_, p_282339_);
        p_282345_.renderDeferredTooltip();
    }

    @Override
    public void render(GuiGraphics p_281549_, int p_281550_, int p_282878_, float p_282465_) {
        for (Renderable renderable : this.renderables) {
            renderable.render(p_281549_, p_281550_, p_282878_, p_282465_);
        }
    }

    @Override
    public boolean keyPressed(int p_96552_, int p_96553_, int p_96554_) {
        if (p_96552_ == 256 && this.shouldCloseOnEsc()) {
            this.onClose();
            return true;
        } else if (super.keyPressed(p_96552_, p_96553_, p_96554_)) {
            return true;
        } else {
            FocusNavigationEvent focusnavigationevent = (FocusNavigationEvent)(switch (p_96552_) {
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

    private FocusNavigationEvent.ArrowNavigation createArrowEvent(ScreenDirection p_265049_) {
        return new FocusNavigationEvent.ArrowNavigation(p_265049_);
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

    protected void setInitialFocus(GuiEventListener p_265756_) {
        ComponentPath componentpath = ComponentPath.path(this, p_265756_.nextFocusPath(new FocusNavigationEvent.InitialFocus()));
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
    protected void changeFocus(ComponentPath p_265308_) {
        this.clearFocus();
        p_265308_.applyFocus(true);
    }

    public boolean shouldCloseOnEsc() {
        return true;
    }

    public void onClose() {
        this.minecraft.popGuiLayer();
    }

    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T p_169406_) {
        this.renderables.add(p_169406_);
        return this.addWidget(p_169406_);
    }

    protected <T extends Renderable> T addRenderableOnly(T p_254514_) {
        this.renderables.add(p_254514_);
        return p_254514_;
    }

    protected <T extends GuiEventListener & NarratableEntry> T addWidget(T p_96625_) {
        this.children.add(p_96625_);
        this.narratables.add(p_96625_);
        return p_96625_;
    }

    protected void removeWidget(GuiEventListener p_169412_) {
        if (p_169412_ instanceof Renderable) {
            this.renderables.remove((Renderable)p_169412_);
        }

        if (p_169412_ instanceof NarratableEntry) {
            this.narratables.remove((NarratableEntry)p_169412_);
        }

        this.children.remove(p_169412_);
    }

    protected void clearWidgets() {
        this.renderables.clear();
        this.children.clear();
        this.narratables.clear();
    }

    public static List<Component> getTooltipFromItem(Minecraft p_281881_, ItemStack p_282833_) {
        return p_282833_.getTooltipLines(
            Item.TooltipContext.of(p_281881_.level),
            p_281881_.player,
            p_281881_.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
        );
    }

    protected void insertText(String p_96587_, boolean p_96588_) {
    }

    public boolean handleComponentClicked(Style p_96592_) {
        ClickEvent clickevent = p_96592_.getClickEvent();
        if (hasShiftDown()) {
            if (p_96592_.getInsertion() != null) {
                this.insertText(p_96592_.getInsertion(), false);
            }
        } else if (clickevent != null) {
            this.handleClickEvent(this.minecraft, clickevent);
            return true;
        }

        return false;
    }

    protected void handleClickEvent(Minecraft p_409550_, ClickEvent p_410189_) {
        defaultHandleGameClickEvent(p_410189_, p_409550_, this);
    }

    protected static void defaultHandleGameClickEvent(ClickEvent p_408337_, Minecraft p_407842_, @Nullable Screen p_407429_) {
        LocalPlayer localplayer = Objects.requireNonNull(p_407842_.player, "Player not available");
        switch (p_408337_) {
            case ClickEvent.RunCommand(String s):
                clickCommandAction(localplayer, s, p_407429_);
                break;
            case ClickEvent.ShowDialog clickevent$showdialog:
                localplayer.connection.showDialog(clickevent$showdialog.dialog(), p_407429_);
                break;
            case ClickEvent.Custom clickevent$custom:
                localplayer.connection.send(new ServerboundCustomClickActionPacket(clickevent$custom.id(), clickevent$custom.payload()));
                if (p_407842_.screen != p_407429_) {
                    p_407842_.setScreen(p_407429_);
                }
                break;
            default:
                defaultHandleClickEvent(p_408337_, p_407842_, p_407429_);
        }
    }

    protected static void defaultHandleClickEvent(ClickEvent p_407893_, Minecraft p_410530_, @Nullable Screen p_407004_) {
        boolean flag = switch (p_407893_) {
            case ClickEvent.OpenUrl(URI uri) -> {
                clickUrlAction(p_410530_, p_407004_, uri);
                yield false;
            }
            case ClickEvent.OpenFile clickevent$openfile -> {
                Util.getPlatform().openFile(clickevent$openfile.file());
                yield true;
            }
            case ClickEvent.SuggestCommand(String s2) -> {
                String s1 = s2;
                if (p_407004_ != null) {
                    p_407004_.insertText(s1, true);
                }

                yield true;
            }
            case ClickEvent.CopyToClipboard(String s) -> {
                p_410530_.keyboardHandler.setClipboard(s);
                yield true;
            }
            default -> {
                LOGGER.error("Don't know how to handle {}", p_407893_);
                yield true;
            }
        };
        if (flag && p_410530_.screen != p_407004_) {
            p_410530_.setScreen(p_407004_);
        }
    }

    protected static boolean clickUrlAction(Minecraft p_406192_, @Nullable Screen p_408543_, URI p_408598_) {
        if (!p_406192_.options.chatLinks().get()) {
            return false;
        } else {
            if (p_406192_.options.chatLinksPrompt().get()) {
                p_406192_.setScreen(new ConfirmLinkScreen(p_404851_ -> {
                    if (p_404851_) {
                        Util.getPlatform().openUri(p_408598_);
                    }

                    p_406192_.setScreen(p_408543_);
                }, p_408598_.toString(), false));
            } else {
                Util.getPlatform().openUri(p_408598_);
            }

            return true;
        }
    }

    protected static void clickCommandAction(LocalPlayer p_409065_, String p_408291_, @Nullable Screen p_409951_) {
        p_409065_.connection.sendUnattendedCommand(Commands.trimOptionalPrefix(p_408291_), p_409951_);
    }

    public final void init(Minecraft p_96607_, int p_96608_, int p_96609_) {
        this.minecraft = p_96607_;
        this.font = p_96607_.font;
        this.width = p_96608_;
        this.height = p_96609_;
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

    protected void fadeWidgets(float p_409453_) {
        for (GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener instanceof AbstractWidget abstractwidget) {
                abstractwidget.setAlpha(p_409453_);
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

    public void renderBackground(GuiGraphics p_283688_, int p_299421_, int p_298679_, float p_297268_) {
        if (this.minecraft.level == null) {
            this.renderPanorama(p_283688_, p_297268_);
        }

        this.renderBlurredBackground(p_283688_);
        this.renderMenuBackground(p_283688_);
        net.minecraftforge.client.event.ForgeEventFactoryClient.onRenderScreenBackground(this, p_283688_);
    }

    protected void renderBlurredBackground(GuiGraphics p_407253_) {
        float f = this.minecraft.options.getMenuBackgroundBlurriness();
        if (f >= 1.0F) {
            p_407253_.blurBeforeThisStratum();
        }
    }

    protected void renderPanorama(GuiGraphics p_332550_, float p_335227_) {
        this.minecraft.gameRenderer.getPanorama().render(p_332550_, this.width, this.height, true);
    }

    protected void renderMenuBackground(GuiGraphics p_332667_) {
        this.renderMenuBackground(p_332667_, 0, 0, this.width, this.height);
    }

    protected void renderMenuBackground(GuiGraphics p_334761_, int p_328355_, int p_328091_, int p_332954_, int p_331811_) {
        renderMenuBackgroundTexture(p_334761_, this.minecraft.level == null ? MENU_BACKGROUND : INWORLD_MENU_BACKGROUND, p_328355_, p_328091_, 0.0F, 0.0F, p_332954_, p_331811_);
    }

    public static void renderMenuBackgroundTexture(
        GuiGraphics p_331670_, ResourceLocation p_330833_, int p_332491_, int p_335034_, float p_330279_, float p_334888_, int p_331386_, int p_330145_
    ) {
        int i = 32;
        p_331670_.blit(RenderPipelines.GUI_TEXTURED, p_330833_, p_332491_, p_335034_, p_330279_, p_334888_, p_331386_, p_330145_, 32, 32);
    }

    public void renderTransparentBackground(GuiGraphics p_300203_) {
        p_300203_.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
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

    public static boolean isCut(int p_96629_) {
        return p_96629_ == 88 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    public static boolean isPaste(int p_96631_) {
        return p_96631_ == 86 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    public static boolean isCopy(int p_96633_) {
        return p_96633_ == 67 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    public static boolean isSelectAll(int p_96635_) {
        return p_96635_ == 65 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    protected void repositionElements() {
        this.rebuildWidgets();
    }

    public void resize(Minecraft p_96575_, int p_96576_, int p_96577_) {
        this.width = p_96576_;
        this.height = p_96577_;
        this.repositionElements();
    }

    public void fillCrashDetails(CrashReport p_363781_) {
        CrashReportCategory crashreportcategory = p_363781_.addCategory("Affected screen", 1);
        crashreportcategory.setDetail("Screen name", () -> this.getClass().getCanonicalName());
    }

    protected boolean isValidCharacterForName(String p_96584_, char p_96585_, int p_96586_) {
        int i = p_96584_.indexOf(58);
        int j = p_96584_.indexOf(47);
        if (p_96585_ == ':') {
            return (j == -1 || p_96586_ <= j) && i == -1;
        } else {
            return p_96585_ == '/'
                ? p_96586_ > i
                : p_96585_ == '_' || p_96585_ == '-' || p_96585_ >= 'a' && p_96585_ <= 'z' || p_96585_ >= '0' && p_96585_ <= '9' || p_96585_ == '.';
        }
    }

    @Override
    public boolean isMouseOver(double p_96595_, double p_96596_) {
        return true;
    }

    public void onFilesDrop(List<Path> p_96591_) {
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

    private void scheduleNarration(long p_169381_, boolean p_169382_) {
        this.nextNarrationTime = Util.getMillis() + p_169381_;
        if (p_169382_) {
            this.narrationSuppressTime = Long.MIN_VALUE;
        }
    }

    private void suppressNarration(long p_169379_) {
        this.narrationSuppressTime = Util.getMillis() + p_169379_;
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

    public void triggerImmediateNarration(boolean p_169408_) {
        if (this.shouldRunNarration()) {
            this.runNarration(p_169408_);
        }
    }

    private void runNarration(boolean p_169410_) {
        this.narrationState.update(this::updateNarrationState);
        String s = this.narrationState.collectNarrationText(!p_169410_);
        if (!s.isEmpty()) {
            this.minecraft.getNarrator().saySystemNow(s);
        }
    }

    protected boolean shouldNarrateNavigation() {
        return true;
    }

    protected void updateNarrationState(NarrationElementOutput p_169396_) {
        p_169396_.add(NarratedElementType.TITLE, this.getNarrationMessage());
        if (this.shouldNarrateNavigation()) {
            p_169396_.add(NarratedElementType.USAGE, USAGE_NARRATION);
        }

        this.updateNarratedWidget(p_169396_);
    }

    protected void updateNarratedWidget(NarrationElementOutput p_169403_) {
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
                p_169403_.add(
                    NarratedElementType.POSITION, Component.translatable("narrator.position.screen", screen$narratablesearchresult.index + 1, list.size())
                );
                if (screen$narratablesearchresult.priority == NarratableEntry.NarrationPriority.FOCUSED) {
                    p_169403_.add(NarratedElementType.USAGE, this.getUsageNarration());
                }
            }

            screen$narratablesearchresult.entry.updateNarration(p_169403_.nest());
        }
    }

    protected Component getUsageNarration() {
        return Component.translatable("narration.component_list.usage");
    }

    @Nullable
    public static Screen.NarratableSearchResult findNarratableWidget(List<? extends NarratableEntry> p_169401_, @Nullable NarratableEntry p_169402_) {
        Screen.NarratableSearchResult screen$narratablesearchresult = null;
        Screen.NarratableSearchResult screen$narratablesearchresult1 = null;
        int i = 0;

        for (int j = p_169401_.size(); i < j; i++) {
            NarratableEntry narratableentry = p_169401_.get(i);
            NarratableEntry.NarrationPriority narratableentry$narrationpriority = narratableentry.narrationPriority();
            if (narratableentry$narrationpriority.isTerminal()) {
                if (narratableentry != p_169402_) {
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

    public void updateNarratorStatus(boolean p_345154_) {
        if (p_345154_) {
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

        public NarratableSearchResult(NarratableEntry p_169424_, int p_169425_, NarratableEntry.NarrationPriority p_169426_) {
            this.entry = p_169424_;
            this.index = p_169425_;
            this.priority = p_169426_;
        }
    }
}
