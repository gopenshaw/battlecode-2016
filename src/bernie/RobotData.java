package bernie;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class RobotData {
    int health;
    MapLocation location;
    RobotType type;

    public RobotData(MapLocation location, int health, RobotType type) {
        this.health = health;
        this.location = location;
        this.type = type;
    }

    @Override
    public String toString() {
        return location + " " + health + " " + type + "; ";
    }
}
