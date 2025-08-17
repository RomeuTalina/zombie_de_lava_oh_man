package net.minecraft.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class WaypointArgument {
    public static final SimpleCommandExceptionType ERROR_NOT_A_WAYPOINT = new SimpleCommandExceptionType(Component.translatable("argument.waypoint.invalid"));

    public static WaypointTransmitter getWaypoint(CommandContext<CommandSourceStack> pContext, String pArgument) throws CommandSyntaxException {
        if (pContext.getArgument(pArgument, EntitySelector.class).findSingleEntity(pContext.getSource()) instanceof WaypointTransmitter waypointtransmitter) {
            return waypointtransmitter;
        } else {
            throw ERROR_NOT_A_WAYPOINT.create();
        }
    }
}