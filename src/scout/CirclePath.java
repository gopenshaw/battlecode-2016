package scout;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class CirclePath {
    private final MapLocation center;
    private final int radiusSquared;
    private final RobotController rc;
    private final int TOLERANCE;

    private final Direction INITIAL_DIRECTION = Direction.NORTH;

    public CirclePath(MapLocation center, int radiusSquared, RobotController rc) {
        this.center = center;
        this.radiusSquared = radiusSquared;
        this.TOLERANCE = radiusSquared;
        this.rc = rc;
    }

    public Direction getNextDirection(MapLocation currentLocation) throws GameActionException {
        Direction pathDirection = getNextPathDirection(currentLocation);

        //--make sure direction is on map
        while (!rc.onTheMap(currentLocation.add(pathDirection))) {
            pathDirection = pathDirection.rotateRight();
        }

        return pathDirection;
    }

    private Direction getNextPathDirection(MapLocation currentLocation) {
        if (currentLocation.equals(center)) {
            return INITIAL_DIRECTION;
        }

        int distanceFromCenter = currentLocation.distanceSquaredTo(center);
        if (distanceFromCenter < radiusSquared) {
            return center.directionTo(currentLocation);
        }
        else if (distanceFromCenter > radiusSquared + TOLERANCE) {
            return currentLocation.directionTo(center);
        }
        else {
            Direction towardCenter = currentLocation.directionTo(center);
            Direction tangent = towardCenter.rotateLeft().rotateLeft();
            while (!rc.canMove(tangent)) {
                tangent = tangent.rotateRight();
            }

            return tangent;
        }
    }
}
