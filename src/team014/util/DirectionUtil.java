package team014.util;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class DirectionUtil {
    public static Direction getDirectionAwayFrom(RobotInfo[] robots, RobotController rc) {
        return getDirectionAwayFrom(robots, new RobotInfo[0], rc);
    }

    public static Direction getDirectionAwayFrom(RobotInfo[] robots1, RobotInfo[] robots2, RobotController rc) {
        Direction[] directions = new Direction[robots1.length + robots2.length];
        MapLocation currentLocation = rc.getLocation();
        for (int i = 0; i < robots1.length; i++) {
            MapLocation enemyLocation = robots1[i].location;
            directions[i] = enemyLocation.directionTo(currentLocation);
        }

        int offset = robots1.length;
        for (int i = 0; i < robots2.length; i++) {
            MapLocation enemyLocation = robots2[i].location;
            directions[offset + i] = enemyLocation.directionTo(currentLocation);
        }

        return getAverageDirection(directions);
    }

    private static Direction getAverageDirection(Direction[] directions) {
        int x = 0;
        int y = 0;
        for (Direction d : directions) {
            x += d.dx;
            y += d.dy;
        }

        if (x < 0) {
            if (y < 0) {
                return Direction.NORTH_WEST;
            }
            else if (y > 0) {
                return Direction.SOUTH_WEST;
            }
            else {
                return Direction.WEST;
            }
        }
        else if (x > 0) {
            if (y < 0) {
                return Direction.NORTH_EAST;
            }
            else if (y > 0) {
                return Direction.SOUTH_EAST;
            }
            else {
                return Direction.EAST;
            }
        }
        else {
            if (y < 0) {
                return Direction.NORTH;
            }
            else {
                return Direction.SOUTH;
            }
        }
    }
}
