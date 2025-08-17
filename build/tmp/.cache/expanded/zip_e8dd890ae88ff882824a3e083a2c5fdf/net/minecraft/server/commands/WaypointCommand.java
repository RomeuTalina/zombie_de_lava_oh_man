package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.HexColorArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.WaypointArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import net.minecraft.world.waypoints.WaypointStyleAssets;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class WaypointCommand {
    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pBuildContext) {
        pDispatcher.register(
            Commands.literal("waypoint")
                .requires(Commands.hasPermission(2))
                .then(Commands.literal("list").executes(p_409851_ -> listWaypoints(p_409851_.getSource())))
                .then(
                    Commands.literal("modify")
                        .then(
                            Commands.argument("waypoint", EntityArgument.entity())
                                .then(
                                    Commands.literal("color")
                                        .then(
                                            Commands.argument("color", ColorArgument.color())
                                                .executes(
                                                    p_407116_ -> setWaypointColor(
                                                        p_407116_.getSource(),
                                                        WaypointArgument.getWaypoint(p_407116_, "waypoint"),
                                                        ColorArgument.getColor(p_407116_, "color")
                                                    )
                                                )
                                        )
                                        .then(
                                            Commands.literal("hex")
                                                .then(
                                                    Commands.argument("color", HexColorArgument.hexColor())
                                                        .executes(
                                                            p_409442_ -> setWaypointColor(
                                                                p_409442_.getSource(),
                                                                WaypointArgument.getWaypoint(p_409442_, "waypoint"),
                                                                HexColorArgument.getHexColor(p_409442_, "color")
                                                            )
                                                        )
                                                )
                                        )
                                        .then(
                                            Commands.literal("reset")
                                                .executes(p_407074_ -> resetWaypointColor(p_407074_.getSource(), WaypointArgument.getWaypoint(p_407074_, "waypoint")))
                                        )
                                )
                                .then(
                                    Commands.literal("style")
                                        .then(
                                            Commands.literal("reset")
                                                .executes(
                                                    p_408059_ -> setWaypointStyle(
                                                        p_408059_.getSource(), WaypointArgument.getWaypoint(p_408059_, "waypoint"), WaypointStyleAssets.DEFAULT
                                                    )
                                                )
                                        )
                                        .then(
                                            Commands.literal("set")
                                                .then(
                                                    Commands.argument("style", ResourceLocationArgument.id())
                                                        .executes(
                                                            p_406034_ -> setWaypointStyle(
                                                                p_406034_.getSource(),
                                                                WaypointArgument.getWaypoint(p_406034_, "waypoint"),
                                                                ResourceKey.create(
                                                                    WaypointStyleAssets.ROOT_ID, ResourceLocationArgument.getId(p_406034_, "style")
                                                                )
                                                            )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int setWaypointStyle(CommandSourceStack pSource, WaypointTransmitter pWaypoint, ResourceKey<WaypointStyleAsset> pStyle) {
        mutateIcon(pSource, pWaypoint, p_407629_ -> p_407629_.style = pStyle);
        pSource.sendSuccess(() -> Component.translatable("commands.waypoint.modify.style"), false);
        return 0;
    }

    private static int setWaypointColor(CommandSourceStack pSource, WaypointTransmitter pWaypoint, ChatFormatting pColor) {
        mutateIcon(pSource, pWaypoint, p_409923_ -> p_409923_.color = Optional.of(pColor.getColor()));
        pSource.sendSuccess(() -> Component.translatable("commands.waypoint.modify.color", Component.literal(pColor.getName()).withStyle(pColor)), false);
        return 0;
    }

    private static int setWaypointColor(CommandSourceStack pSource, WaypointTransmitter pWaypoint, Integer pColor) {
        mutateIcon(pSource, pWaypoint, p_409255_ -> p_409255_.color = Optional.of(pColor));
        pSource.sendSuccess(
            () -> Component.translatable(
                "commands.waypoint.modify.color", Component.literal(String.format("%06X", ARGB.color(0, pColor))).withColor(pColor)
            ),
            false
        );
        return 0;
    }

    private static int resetWaypointColor(CommandSourceStack pSource, WaypointTransmitter pWaypoint) {
        mutateIcon(pSource, pWaypoint, p_410469_ -> p_410469_.color = Optional.empty());
        pSource.sendSuccess(() -> Component.translatable("commands.waypoint.modify.color.reset"), false);
        return 0;
    }

    private static int listWaypoints(CommandSourceStack pSource) {
        ServerLevel serverlevel = pSource.getLevel();
        Set<WaypointTransmitter> set = serverlevel.getWaypointManager().transmitters();
        String s = serverlevel.dimension().location().toString();
        if (set.isEmpty()) {
            pSource.sendSuccess(() -> Component.translatable("commands.waypoint.list.empty", s), false);
            return 0;
        } else {
            Component component = ComponentUtils.formatList(
                set.stream()
                    .map(
                        p_410734_ -> {
                            if (p_410734_ instanceof LivingEntity livingentity) {
                                BlockPos blockpos = livingentity.blockPosition();
                                return livingentity.getFeedbackDisplayName()
                                    .copy()
                                    .withStyle(
                                        p_406446_ -> p_406446_.withClickEvent(
                                                new ClickEvent.SuggestCommand(
                                                    "/execute in "
                                                        + s
                                                        + " run tp @s "
                                                        + blockpos.getX()
                                                        + " "
                                                        + blockpos.getY()
                                                        + " "
                                                        + blockpos.getZ()
                                                )
                                            )
                                            .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
                                            .withColor(p_410734_.waypointIcon().color.orElse(-1))
                                    );
                            } else {
                                return Component.literal(p_410734_.toString());
                            }
                        }
                    )
                    .toList(),
                Function.identity()
            );
            pSource.sendSuccess(() -> Component.translatable("commands.waypoint.list.success", set.size(), s, component), false);
            return set.size();
        }
    }

    private static void mutateIcon(CommandSourceStack pSource, WaypointTransmitter pWaypoint, Consumer<Waypoint.Icon> pMutator) {
        ServerLevel serverlevel = pSource.getLevel();
        serverlevel.getWaypointManager().untrackWaypoint(pWaypoint);
        pMutator.accept(pWaypoint.waypointIcon());
        serverlevel.getWaypointManager().trackWaypoint(pWaypoint);
    }
}