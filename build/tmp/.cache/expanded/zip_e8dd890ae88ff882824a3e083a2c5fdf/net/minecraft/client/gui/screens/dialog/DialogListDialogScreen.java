package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogListDialog;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DialogListDialogScreen extends ButtonListDialogScreen<DialogListDialog> {
    public DialogListDialogScreen(@Nullable Screen pPreviousScreen, DialogListDialog pDialog, DialogConnectionAccess pConnectionAccess) {
        super(pPreviousScreen, pDialog, pConnectionAccess);
    }

    protected Stream<ActionButton> createListActions(DialogListDialog p_409917_, DialogConnectionAccess p_409822_) {
        return p_409917_.dialogs().stream().map(p_408364_ -> createDialogClickAction(p_409917_, (Holder<Dialog>)p_408364_));
    }

    private static ActionButton createDialogClickAction(DialogListDialog pDialog, Holder<Dialog> pDialogToOpen) {
        return new ActionButton(
            new CommonButtonData(pDialogToOpen.value().common().computeExternalTitle(), pDialog.buttonWidth()),
            Optional.of(new StaticAction(new ClickEvent.ShowDialog(pDialogToOpen)))
        );
    }
}