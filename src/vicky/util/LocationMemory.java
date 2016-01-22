package vicky.util;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class LocationMemory {
    private int[] roundRecorded = new int[32000];
    private MapLocation[] location = new MapLocation[32000];

    private static final int ROUND_TOLERANCE = 3;

    public void saveLocation(RobotInfo robot, int currentRound) {
        roundRecorded[robot.ID] = currentRound;
        location[robot.ID] = robot.location;
    }

    public MapLocation getPreviousLocation(RobotInfo robot, int currentRound) {
        if (roundRecorded[robot.ID] + ROUND_TOLERANCE < currentRound) {
            return null;
        }

        return location[robot.ID];
    }
}
