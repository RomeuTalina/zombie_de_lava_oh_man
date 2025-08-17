package net.minecraft.client.gui.screens.dialog;

import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ConfirmationDialog;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogListDialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.NoticeDialog;
import net.minecraft.server.dialog.ServerLinksDialog;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DialogScreens {
    private static final Map<MapCodec<? extends Dialog>, DialogScreens.Factory<?>> FACTORIES = new HashMap<>();

    private static <T extends Dialog> void register(MapCodec<T> pCodec, DialogScreens.Factory<? super T> pFactory) {
        FACTORIES.put(pCodec, pFactory);
    }

    @Nullable
    public static <T extends Dialog> DialogScreen<T> createFromData(T pDialog, @Nullable Screen pPreviousScreen, DialogConnectionAccess pConnectionAccess) {
        DialogScreens.Factory<T> factory = (DialogScreens.Factory<T>)FACTORIES.get(pDialog.codec());
        return factory != null ? factory.create(pPreviousScreen, pDialog, pConnectionAccess) : null;
    }

    public static void bootstrap() {
        register(ConfirmationDialog.MAP_CODEC, SimpleDialogScreen::new);
        register(NoticeDialog.MAP_CODEC, SimpleDialogScreen::new);
        register(DialogListDialog.MAP_CODEC, DialogListDialogScreen::new);
        register(MultiActionDialog.MAP_CODEC, MultiButtonDialogScreen::new);
        register(ServerLinksDialog.MAP_CODEC, ServerLinksDialogScreen::new);
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface Factory<T extends Dialog> {
        DialogScreen<T> create(@Nullable Screen pPreviousScreen, T pDialog, DialogConnectionAccess pConnectionAccess);
    }
}