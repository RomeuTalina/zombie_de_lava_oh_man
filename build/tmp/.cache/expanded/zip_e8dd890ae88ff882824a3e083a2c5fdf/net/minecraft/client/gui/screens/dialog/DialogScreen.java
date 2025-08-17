package net.minecraft.client.gui.screens.dialog;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.body.DialogBodyHandlers;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableObject;

@OnlyIn(Dist.CLIENT)
public abstract class DialogScreen<T extends Dialog> extends Screen {
    public static final Component DISCONNECT = Component.translatable("menu.custom_screen_info.disconnect");
    private static final int WARNING_BUTTON_SIZE = 20;
    private static final WidgetSprites WARNING_BUTTON_SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("dialog/warning_button"),
        ResourceLocation.withDefaultNamespace("dialog/warning_button_disabled"),
        ResourceLocation.withDefaultNamespace("dialog/warning_button_highlighted")
    );
    private final T dialog;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    @Nullable
    private final Screen previousScreen;
    @Nullable
    private ScrollableLayout bodyScroll;
    private Button warningButton;
    private final DialogConnectionAccess connectionAccess;
    private Supplier<Optional<ClickEvent>> onClose = DialogControlSet.EMPTY_ACTION;

    public DialogScreen(@Nullable Screen pPreviousScreen, T pDialog, DialogConnectionAccess pConnectionAccess) {
        super(pDialog.common().title());
        this.dialog = pDialog;
        this.previousScreen = pPreviousScreen;
        this.connectionAccess = pConnectionAccess;
    }

    @Override
    protected final void init() {
        super.init();
        this.warningButton = this.createWarningButton();
        this.warningButton.setTabOrderGroup(-10);
        DialogControlSet dialogcontrolset = new DialogControlSet(this);
        LinearLayout linearlayout = LinearLayout.vertical().spacing(10);
        linearlayout.defaultCellSetting().alignHorizontallyCenter();
        this.layout.addToHeader(this.createTitleWithWarningButton());

        for (DialogBody dialogbody : this.dialog.common().body()) {
            LayoutElement layoutelement = DialogBodyHandlers.createBodyElement(this, dialogbody);
            if (layoutelement != null) {
                linearlayout.addChild(layoutelement);
            }
        }

        for (Input input : this.dialog.common().inputs()) {
            dialogcontrolset.addInput(input, linearlayout::addChild);
        }

        this.populateBodyElements(linearlayout, dialogcontrolset, this.dialog, this.connectionAccess);
        this.bodyScroll = new ScrollableLayout(this.minecraft, linearlayout, this.layout.getContentHeight());
        this.layout.addToContents(this.bodyScroll);
        this.updateHeaderAndFooter(this.layout, dialogcontrolset, this.dialog, this.connectionAccess);
        this.onClose = dialogcontrolset.bindAction(this.dialog.onCancel());
        this.layout.visitWidgets(p_408473_ -> {
            if (p_408473_ != this.warningButton) {
                this.addRenderableWidget(p_408473_);
            }
        });
        this.addRenderableWidget(this.warningButton);
        this.repositionElements();
    }

    protected void populateBodyElements(LinearLayout pLayout, DialogControlSet pControls, T pDialog, DialogConnectionAccess pConnectionAccess) {
    }

    protected void updateHeaderAndFooter(HeaderAndFooterLayout pLayout, DialogControlSet pControls, T pDialog, DialogConnectionAccess pConnectionAccess) {
    }

    @Override
    protected void repositionElements() {
        this.bodyScroll.setMaxHeight(this.layout.getContentHeight());
        this.layout.arrangeElements();
        this.makeSureWarningButtonIsInBounds();
    }

    protected LayoutElement createTitleWithWarningButton() {
        LinearLayout linearlayout = LinearLayout.horizontal().spacing(10);
        linearlayout.defaultCellSetting().alignHorizontallyCenter().alignVerticallyMiddle();
        linearlayout.addChild(new StringWidget(this.title, this.font));
        linearlayout.addChild(this.warningButton);
        return linearlayout;
    }

    protected void makeSureWarningButtonIsInBounds() {
        int i = this.warningButton.getX();
        int j = this.warningButton.getY();
        if (i < 0 || j < 0 || i > this.width - 20 || j > this.height - 20) {
            this.warningButton.setX(Math.max(0, this.width - 40));
            this.warningButton.setY(Math.min(5, this.height));
        }
    }

    private Button createWarningButton() {
        ImageButton imagebutton = new ImageButton(
            0,
            0,
            20,
            20,
            WARNING_BUTTON_SPRITES,
            p_407046_ -> this.minecraft.setScreen(DialogScreen.WarningScreen.create(this.minecraft, this)),
            Component.translatable("menu.custom_screen_info.button_narration")
        );
        imagebutton.setTooltip(Tooltip.create(Component.translatable("menu.custom_screen_info.tooltip")));
        return imagebutton;
    }

    @Override
    public boolean isPauseScreen() {
        return this.dialog.common().pause();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.dialog.common().canCloseWithEscape();
    }

    @Override
    public void onClose() {
        this.runAction(this.onClose.get(), DialogAction.CLOSE);
    }

    public void runAction(Optional<ClickEvent> pClickEvent) {
        this.runAction(pClickEvent, this.dialog.common().afterAction());
    }

    public void runAction(Optional<ClickEvent> pClickEvent, DialogAction pAction) {
        Screen screen = (Screen)(switch (pAction) {
            case NONE -> this;
            case CLOSE -> this.previousScreen;
            case WAIT_FOR_RESPONSE -> new WaitingForResponseScreen(this.previousScreen);
        });
        if (pClickEvent.isPresent()) {
            this.handleDialogClickEvent(pClickEvent.get(), screen);
        } else {
            this.minecraft.setScreen(screen);
        }
    }

    private void handleDialogClickEvent(ClickEvent pClickEvent, @Nullable Screen pPreviousScreen) {
        switch (pClickEvent) {
            case ClickEvent.RunCommand(String s):
                this.connectionAccess.runCommand(Commands.trimOptionalPrefix(s), pPreviousScreen);
                break;
            case ClickEvent.ShowDialog clickevent$showdialog:
                this.connectionAccess.openDialog(clickevent$showdialog.dialog(), pPreviousScreen);
                break;
            case ClickEvent.Custom clickevent$custom:
                this.connectionAccess.sendCustomAction(clickevent$custom.id(), clickevent$custom.payload());
                this.minecraft.setScreen(pPreviousScreen);
                break;
            default:
                defaultHandleClickEvent(pClickEvent, this.minecraft, pPreviousScreen);
        }
    }

    @Nullable
    public Screen previousScreen() {
        return this.previousScreen;
    }

    protected static LayoutElement packControlsIntoColumns(List<? extends LayoutElement> pControls, int pColumns) {
        GridLayout gridlayout = new GridLayout();
        gridlayout.defaultCellSetting().alignHorizontallyCenter();
        gridlayout.columnSpacing(2).rowSpacing(2);
        int i = pControls.size();
        int j = i / pColumns;
        int k = j * pColumns;

        for (int l = 0; l < k; l++) {
            gridlayout.addChild(pControls.get(l), l / pColumns, l % pColumns);
        }

        if (i != k) {
            LinearLayout linearlayout = LinearLayout.horizontal().spacing(2);
            linearlayout.defaultCellSetting().alignHorizontallyCenter();

            for (int i1 = k; i1 < i; i1++) {
                linearlayout.addChild(pControls.get(i1));
            }

            gridlayout.addChild(linearlayout, j, 0, 1, pColumns);
        }

        return gridlayout;
    }

    @OnlyIn(Dist.CLIENT)
    public static class WarningScreen extends ConfirmScreen {
        private final MutableObject<Screen> returnScreen;

        public static Screen create(Minecraft pMinecraft, Screen pReturnScreen) {
            return new DialogScreen.WarningScreen(pMinecraft, new MutableObject<>(pReturnScreen));
        }

        private WarningScreen(Minecraft pMinecraft, MutableObject<Screen> pReturnScreen) {
            super(
                p_408993_ -> {
                    if (p_408993_) {
                        PauseScreen.disconnectFromWorld(pMinecraft, DialogScreen.DISCONNECT);
                    } else {
                        pMinecraft.setScreen(pReturnScreen.getValue());
                    }
                },
                Component.translatable("menu.custom_screen_info.title"),
                Component.translatable("menu.custom_screen_info.contents"),
                CommonComponents.disconnectButtonLabel(pMinecraft.isLocalServer()),
                CommonComponents.GUI_BACK
            );
            this.returnScreen = pReturnScreen;
        }

        @Nullable
        public Screen returnScreen() {
            return this.returnScreen.getValue();
        }

        public void updateReturnScreen(@Nullable Screen pReturnScreen) {
            this.returnScreen.setValue(pReturnScreen);
        }
    }
}