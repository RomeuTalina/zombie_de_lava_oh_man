package net.minecraft.client.gui.screens.dialog;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.dialog.input.InputControlHandlers;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.action.Action;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DialogControlSet {
    public static final Supplier<Optional<ClickEvent>> EMPTY_ACTION = Optional::empty;
    private final DialogScreen<?> screen;
    private final Map<String, Action.ValueGetter> valueGetters = new HashMap<>();

    public DialogControlSet(DialogScreen<?> pScreen) {
        this.screen = pScreen;
    }

    public void addInput(Input pInput, Consumer<LayoutElement> pAdder) {
        String s = pInput.key();
        InputControlHandlers.createHandler(pInput.control(), this.screen, (p_410319_, p_406391_) -> {
            this.valueGetters.put(s, p_406391_);
            pAdder.accept(p_410319_);
        });
    }

    private static Button.Builder createDialogButton(CommonButtonData pButtonData, Button.OnPress pOnPress) {
        Button.Builder button$builder = Button.builder(pButtonData.label(), pOnPress);
        button$builder.width(pButtonData.width());
        if (pButtonData.tooltip().isPresent()) {
            button$builder = button$builder.tooltip(Tooltip.create(pButtonData.tooltip().get()));
        }

        return button$builder;
    }

    public Supplier<Optional<ClickEvent>> bindAction(Optional<Action> pAction) {
        if (pAction.isPresent()) {
            Action action = pAction.get();
            return () -> action.createAction(this.valueGetters);
        } else {
            return EMPTY_ACTION;
        }
    }

    public Button.Builder createActionButton(ActionButton pButton) {
        Supplier<Optional<ClickEvent>> supplier = this.bindAction(pButton.action());
        return createDialogButton(pButton.button(), p_406838_ -> this.screen.runAction(supplier.get()));
    }
}