package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConfirmScreen extends Screen {
    private final Component message;
    protected LinearLayout layout = LinearLayout.vertical().spacing(8);
    protected Component yesButtonComponent;
    protected Component noButtonComponent;
    @Nullable
    protected Button yesButton;
    @Nullable
    protected Button noButton;
    private int delayTicker;
    protected final BooleanConsumer callback;

    public ConfirmScreen(BooleanConsumer pCallback, Component pTitle, Component pMessage) {
        this(pCallback, pTitle, pMessage, CommonComponents.GUI_YES, CommonComponents.GUI_NO);
    }

    public ConfirmScreen(BooleanConsumer pCallback, Component pTitle, Component pMessage, Component pYesButton, Component pNoButton) {
        super(pTitle);
        this.callback = pCallback;
        this.message = pMessage;
        this.yesButtonComponent = pYesButton;
        this.noButtonComponent = pNoButton;
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), this.message);
    }

    @Override
    protected void init() {
        super.init();
        this.layout.defaultCellSetting().alignHorizontallyCenter();
        this.layout.addChild(new StringWidget(this.title, this.font));
        this.layout.addChild(new MultiLineTextWidget(this.message, this.font).setMaxWidth(this.width - 50).setMaxRows(15).setCentered(true));
        this.addAdditionalText();
        LinearLayout linearlayout = this.layout.addChild(LinearLayout.horizontal().spacing(4));
        linearlayout.defaultCellSetting().paddingTop(16);
        this.addButtons(linearlayout);
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    protected void addAdditionalText() {
    }

    protected void addButtons(LinearLayout pLayout) {
        this.yesButton = pLayout.addChild(Button.builder(this.yesButtonComponent, p_169259_ -> this.callback.accept(true)).build());
        this.noButton = pLayout.addChild(Button.builder(this.noButtonComponent, p_169257_ -> this.callback.accept(false)).build());
    }

    public void setDelay(int pTicksUntilEnable) {
        this.delayTicker = pTicksUntilEnable;
        this.yesButton.active = false;
        this.noButton.active = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (--this.delayTicker == 0) {
            this.yesButton.active = true;
            this.noButton.active = true;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) {
            this.callback.accept(false);
            return true;
        } else {
            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }
    }
}