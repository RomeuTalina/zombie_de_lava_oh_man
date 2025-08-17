package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;

public class DialogCommand {
    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pBuildContext) {
        pDispatcher.register(
            Commands.literal("dialog")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.literal("show")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.argument("dialog", ResourceOrIdArgument.dialog(pBuildContext))
                                        .executes(
                                            p_409348_ -> showDialog(
                                                (CommandSourceStack)p_409348_.getSource(),
                                                EntityArgument.getPlayers(p_409348_, "targets"),
                                                ResourceOrIdArgument.getDialog(p_409348_, "dialog")
                                            )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("clear")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .executes(p_408723_ -> clearDialog(p_408723_.getSource(), EntityArgument.getPlayers(p_408723_, "targets")))
                        )
                )
        );
    }

    private static int showDialog(CommandSourceStack pSource, Collection<ServerPlayer> pTargets, Holder<Dialog> pDialog) {
        for (ServerPlayer serverplayer : pTargets) {
            serverplayer.openDialog(pDialog);
        }

        if (pTargets.size() == 1) {
            pSource.sendSuccess(() -> Component.translatable("commands.dialog.show.single", pTargets.iterator().next().getDisplayName()), true);
        } else {
            pSource.sendSuccess(() -> Component.translatable("commands.dialog.show.multiple", pTargets.size()), true);
        }

        return pTargets.size();
    }

    private static int clearDialog(CommandSourceStack pSource, Collection<ServerPlayer> pTargets) {
        for (ServerPlayer serverplayer : pTargets) {
            serverplayer.connection.send(ClientboundClearDialogPacket.INSTANCE);
        }

        if (pTargets.size() == 1) {
            pSource.sendSuccess(() -> Component.translatable("commands.dialog.clear.single", pTargets.iterator().next().getDisplayName()), true);
        } else {
            pSource.sendSuccess(() -> Component.translatable("commands.dialog.clear.multiple", pTargets.size()), true);
        }

        return pTargets.size();
    }
}