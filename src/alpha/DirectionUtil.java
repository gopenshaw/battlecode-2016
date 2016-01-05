package alpha;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class DirectionUtil {
    public static Direction getDirectionAwayFrom(RobotInfo[] robots, RobotController rc) {
        Direction[] directions = new Direction[robots.length];
        MapLocation currentLocation = rc.getLocation();
        for (int i = 0; i < robots.length; i++) {
            MapLocation enemyLocation = robots[i].location;
            directions[i] = enemyLocation.directionTo(currentLocation);
        }

        return getAverageDirection(directions);
    }

    public static Direction getAverageDirection(Direction[] directions) {
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
