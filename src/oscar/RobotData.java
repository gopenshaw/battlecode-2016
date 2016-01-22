package oscar;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class RobotData {
    public final int health;
    public final int id;
    public final MapLocation location;
    public final RobotType type;

    public RobotData(int id, MapLocation location, int health, RobotType type) {
        this.health = health;
        this.location = location;
        this.type = type;
        this.id = id;
    }

    @Override
    public String toString() {
        return type + ":" + location + " id:" + id + " health:" + health + ";";
    }
}
