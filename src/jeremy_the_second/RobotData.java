package jeremy_the_second;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class RobotData {
    int health;
    int id;
    MapLocation location;
    RobotType type;

    public RobotData(int id, MapLocation location, int health, RobotType type) {
        this.health = health;
        this.location = location;
        this.type = type;
        this.id = id;
    }

    @Override
    public String toString() {
        return location + " " + health + " " + type + "; ";
    }
}
