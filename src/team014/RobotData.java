package team014;

import battlecode.common.MapLocation;

public class RobotData {
    int health;
    MapLocation location;

    RobotData(MapLocation location, int health) {
        this.health = health;
        this.location = location;
    }

    @Override
    public String toString() {
        return location + " " + health + "; ";
    }
}
