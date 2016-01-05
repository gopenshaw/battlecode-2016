package alpha;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class DirectionUtil {
    public static Direction getDirectionAwayFrom(RobotInfo[] robots, RobotController rc) {
        int x = 0;
        int y = 0;
        MapLocation currentLocation = rc.getLocation();
        for (RobotInfo ri : robots) {
            MapLocation enemyLocation = ri.location;
            x += enemyLocation.directionTo(currentLocation).dx;
            y += enemyLocation.directionTo(currentLocation).dy;
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
