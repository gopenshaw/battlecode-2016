package selfie;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
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

    public RobotData(RobotInfo robot) {
        this.health = (int) robot.health;
        this.location = robot.location;
        this.type = robot.type;
        this.id = robot.ID;
    }

    @Override
    public String toString() {
        return type + ":" + location + " id:" + id + " health:" + health + ";";
    }
}
