package oscar;

import battlecode.common.MapLocation;

public class DenPath {
    MapLocation denLocation;
    MapLocation firstWaypoint;
    MapLocation secondWaypoint;

    public DenPath(MapLocation denLocation, MapLocation firstWaypoint, MapLocation secondWaypoint) {
        this.denLocation = denLocation;
        this.firstWaypoint = firstWaypoint;
        this.secondWaypoint = secondWaypoint;
    }

    public int getWaypointCount() {
        int count = 0;
        if (firstWaypoint != null) {
            count++;
        }

        if (secondWaypoint != null) {
            count++;
        }

        return count;
    }
}
