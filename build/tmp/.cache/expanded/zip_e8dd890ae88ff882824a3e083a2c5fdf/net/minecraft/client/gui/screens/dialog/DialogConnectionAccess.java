package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.Dialog;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface DialogConnectionAccess {
    void disconnect(Component pMessage);

    void runCommand(String pCommand, @Nullable Screen pPreviousScreen);

    void openDialog(Holder<Dialog> pDialog, @Nullable Screen pPreviousScreen);

    void sendCustomAction(ResourceLocation pId, Optional<Tag> pPayload);

    ServerLinks serverLinks();
}