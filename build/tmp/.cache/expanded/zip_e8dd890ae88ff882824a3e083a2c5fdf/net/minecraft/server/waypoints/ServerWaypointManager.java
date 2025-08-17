package net.minecraft.server.waypoints;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.collect.Sets.SetView;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.waypoints.WaypointManager;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class ServerWaypointManager implements WaypointManager<WaypointTransmitter> {
    private final Set<WaypointTransmitter> waypoints = new HashSet<>();
    private final Set<ServerPlayer> players = new HashSet<>();
    private final Table<ServerPlayer, WaypointTransmitter, WaypointTransmitter.Connection> connections = HashBasedTable.create();

    public void trackWaypoint(WaypointTransmitter p_408241_) {
        this.waypoints.add(p_408241_);

        for (ServerPlayer serverplayer : this.players) {
            this.createConnection(serverplayer, p_408241_);
        }
    }

    public void updateWaypoint(WaypointTransmitter p_409897_) {
        if (this.waypoints.contains(p_409897_)) {
            Map<ServerPlayer, WaypointTransmitter.Connection> map = Tables.transpose(this.connections).row(p_409897_);
            SetView<ServerPlayer> setview = Sets.difference(this.players, map.keySet());

            for (Entry<ServerPlayer, WaypointTransmitter.Connection> entry : ImmutableSet.copyOf(map.entrySet())) {
                this.updateConnection(entry.getKey(), p_409897_, entry.getValue());
            }

            for (ServerPlayer serverplayer : setview) {
                this.createConnection(serverplayer, p_409897_);
            }
        }
    }

    public void untrackWaypoint(WaypointTransmitter p_406555_) {
        this.connections.column(p_406555_).forEach((p_408654_, p_407919_) -> p_407919_.disconnect());
        Tables.transpose(this.connections).row(p_406555_).clear();
        this.waypoints.remove(p_406555_);
    }

    public void addPlayer(ServerPlayer pPlayer) {
        this.players.add(pPlayer);

        for (WaypointTransmitter waypointtransmitter : this.waypoints) {
            this.createConnection(pPlayer, waypointtransmitter);
        }

        if (pPlayer.isTransmittingWaypoint()) {
            this.trackWaypoint((WaypointTransmitter)pPlayer);
        }
    }

    public void updatePlayer(ServerPlayer pPlayer) {
        Map<WaypointTransmitter, WaypointTransmitter.Connection> map = this.connections.row(pPlayer);
        SetView<WaypointTransmitter> setview = Sets.difference(this.waypoints, map.keySet());

        for (Entry<WaypointTransmitter, WaypointTransmitter.Connection> entry : ImmutableSet.copyOf(map.entrySet())) {
            this.updateConnection(pPlayer, entry.getKey(), entry.getValue());
        }

        for (WaypointTransmitter waypointtransmitter : setview) {
            this.createConnection(pPlayer, waypointtransmitter);
        }
    }

    public void removePlayer(ServerPlayer pPlayer) {
        this.connections.row(pPlayer).values().removeIf(p_409511_ -> {
            p_409511_.disconnect();
            return true;
        });
        this.untrackWaypoint((WaypointTransmitter)pPlayer);
        this.players.remove(pPlayer);
    }

    public void breakAllConnections() {
        this.connections.values().forEach(WaypointTransmitter.Connection::disconnect);
        this.connections.clear();
    }

    public void remakeConnections(WaypointTransmitter pWaypoint) {
        for (ServerPlayer serverplayer : this.players) {
            this.createConnection(serverplayer, pWaypoint);
        }
    }

    public Set<WaypointTransmitter> transmitters() {
        return this.waypoints;
    }

    private static boolean isLocatorBarEnabledFor(ServerPlayer pPlayer) {
        return pPlayer.level().getServer().getGameRules().getBoolean(GameRules.RULE_LOCATOR_BAR);
    }

    private void createConnection(ServerPlayer pPlayer, WaypointTransmitter pWaypoint) {
        if (pPlayer != pWaypoint) {
            if (isLocatorBarEnabledFor(pPlayer)) {
                pWaypoint.makeWaypointConnectionWith(pPlayer).ifPresentOrElse(p_407837_ -> {
                    this.connections.put(pPlayer, pWaypoint, p_407837_);
                    p_407837_.connect();
                }, () -> {
                    WaypointTransmitter.Connection waypointtransmitter$connection = this.connections.remove(pPlayer, pWaypoint);
                    if (waypointtransmitter$connection != null) {
                        waypointtransmitter$connection.disconnect();
                    }
                });
            }
        }
    }

    private void updateConnection(ServerPlayer pPlayer, WaypointTransmitter pWaypoint, WaypointTransmitter.Connection pConnection) {
        if (pPlayer != pWaypoint) {
            if (isLocatorBarEnabledFor(pPlayer)) {
                if (!pConnection.isBroken()) {
                    pConnection.update();
                } else {
                    pWaypoint.makeWaypointConnectionWith(pPlayer).ifPresentOrElse(p_408633_ -> {
                        p_408633_.connect();
                        this.connections.put(pPlayer, pWaypoint, p_408633_);
                    }, () -> {
                        pConnection.disconnect();
                        this.connections.remove(pPlayer, pWaypoint);
                    });
                }
            }
        }
    }
}