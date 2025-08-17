package net.minecraft.world.waypoints;

public interface WaypointManager<T extends Waypoint> {
    void trackWaypoint(T pWaypoint);

    void updateWaypoint(T pWaypoint);

    void untrackWaypoint(T pWaypoint);
}