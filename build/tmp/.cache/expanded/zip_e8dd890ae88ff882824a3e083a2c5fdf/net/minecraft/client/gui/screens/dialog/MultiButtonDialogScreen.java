package net.minecraft.client.gui.screens.dialog;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiButtonDialogScreen extends ButtonListDialogScreen<MultiActionDialog> {
    public MultiButtonDialogScreen(@Nullable Screen pPreviousScreen, MultiActionDialog pDialog, DialogConnectionAccess pConnectionAccess) {
        super(pPreviousScreen, pDialog, pConnectionAccess);
    }

    protected Stream<ActionButton> createListActions(MultiActionDialog p_408087_, DialogConnectionAccess p_406419_) {
        return p_408087_.actions().stream();
    }
}