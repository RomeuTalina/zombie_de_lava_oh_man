package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public class DialogTypes {
    public static MapCodec<? extends Dialog> bootstrap(Registry<MapCodec<? extends Dialog>> pRegistry) {
        Registry.register(pRegistry, "notice", NoticeDialog.MAP_CODEC);
        Registry.register(pRegistry, "server_links", ServerLinksDialog.MAP_CODEC);
        Registry.register(pRegistry, "dialog_list", DialogListDialog.MAP_CODEC);
        Registry.register(pRegistry, "multi_action", MultiActionDialog.MAP_CODEC);
        return Registry.register(pRegistry, "confirmation", ConfirmationDialog.MAP_CODEC);
    }
}