package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.ServerLinksDialog;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ServerLinksDialogScreen extends ButtonListDialogScreen<ServerLinksDialog> {
    public ServerLinksDialogScreen(@Nullable Screen pPreviousScreen, ServerLinksDialog pDialog, DialogConnectionAccess pConnectionAccess) {
        super(pPreviousScreen, pDialog, pConnectionAccess);
    }

    protected Stream<ActionButton> createListActions(ServerLinksDialog p_410724_, DialogConnectionAccess p_406702_) {
        return p_406702_.serverLinks().entries().stream().map(p_409170_ -> createDialogClickAction(p_410724_, p_409170_));
    }

    private static ActionButton createDialogClickAction(ServerLinksDialog pDialog, ServerLinks.Entry pEntry) {
        return new ActionButton(
            new CommonButtonData(pEntry.displayName(), pDialog.buttonWidth()), Optional.of(new StaticAction(new ClickEvent.OpenUrl(pEntry.link())))
        );
    }
}