package francis;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class RobotData {
    public int health;
    public int id;
    public MapLocation location;
    public RobotType type;

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
