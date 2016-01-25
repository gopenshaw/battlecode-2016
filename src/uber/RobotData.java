package uber;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class RobotData {
    public final int id;
    public final MapLocation location;
    public final RobotType type;
    public final int roundNumber;

    public RobotData(int id, MapLocation location, RobotType type, int roundNumber) {
        this.location = location;
        this.type = type;
        this.id = id;
        this.roundNumber = roundNumber;
    }

    public RobotData(RobotInfo robot, int roundNumber) {
        this.location = robot.location;
        this.type = robot.type;
        this.id = robot.ID;
        this.roundNumber = roundNumber;
    }

    @Override
    public String toString() {
        return type + ":" + location + " id:" + id + ";";
    }
}
