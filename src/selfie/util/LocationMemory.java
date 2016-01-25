package selfie.util;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class LocationMemory {
    private int[] previousLocationRound = new int[32000];
    private MapLocation[] location = new MapLocation[32000];
    private MapLocation[] previousLocation = new MapLocation[32000];

    private static final int ROUND_TOLERANCE = 6;

    public void saveLocation(RobotInfo robot, int currentRound) {
        int robotId = robot.ID;
        MapLocation currentLocation = robot.location;
        if (!currentLocation.equals(this.location[robotId]))  {
            previousLocation[robotId] = this.location[robotId];
            previousLocationRound[robotId] = currentRound;
        }

        this.location[robotId] = currentLocation;
    }

    public MapLocation getPreviousLocation(RobotInfo robot, int currentRound) {
        if (previousLocationRound[robot.ID] + ROUND_TOLERANCE < currentRound) {
            return null;
        }

        return previousLocation[robot.ID];
    }
}
