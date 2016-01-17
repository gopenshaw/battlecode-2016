package jeremy_the_second.nav;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class SquarePath {
    private final MapLocation center;
    private final RobotController rc;

    private int radius;
    private int top;
    private int bottom;
    private int left;
    private int right;

    private Direction previousDirection = null;
    private Direction rotationCountDirection = null;
    private int rotationCount;

    public SquarePath(MapLocation center, int radius, RobotController rc) {
        this.center = center;
        this.radius = radius;
        this.rc = rc;
        this.radius = radius;
        updateBoundaries();

        previousDirection = Direction.NORTH;
        rotationCountDirection = Direction.NORTH;
    }
    public Direction getNextDirection(MapLocation currentLocation) throws GameActionException {
        countRotations(currentLocation);
        Direction pathDirection = getNextPathDirection(currentLocation);

        //--make sure direction is on map
        if (!rc.onTheMap(currentLocation.add(pathDirection))) {
            pathDirection = pathDirection.rotateRight().rotateRight();
        }

        previousDirection = pathDirection;
        return pathDirection;
    }

    public int getRotationsCompleted() {
        return rotationCount;
    }

    public void updateRadius(int newRadius) {
        this.radius = newRadius;
        updateBoundaries();
    }

    public int getRadius() {
        return radius;
    }

    private void updateBoundaries() {
        top = center.y - radius;
        bottom = center.y + radius;
        right = center.x + radius;
        left = center.x - radius;
    }


    private void countRotations(MapLocation currentLocation) {
        if (currentLocation.equals(center)) {
            return;
        }

        if (currentLocation.x == center.x) {
            if (currentLocation.y > center.y) {
                rotationCountDirection = Direction.SOUTH;
            }
            else  {
                if (rotationCountDirection != Direction.NORTH) {
                    rotationCount++;
                }

                rotationCountDirection = Direction.NORTH;
            }
        }
    }

    private Direction getNextPathDirection(MapLocation currentLocation) {
        if (currentLocation.equals(center)) {
            return Direction.NORTH;
        }

        if (previousDirection == Direction.NORTH) {
            if (currentLocation.y <= top) {
                return Direction.EAST;
            }
        }
        else if (previousDirection == Direction.EAST) {
            if (currentLocation.x >= right) {
                return Direction.SOUTH;
            }
        }
        else if (previousDirection == Direction.SOUTH) {
            if (currentLocation.y >= bottom) {
                return Direction.WEST;
            }
        }
        else {
            if (currentLocation.x <= left) {
                return Direction.NORTH;
            }
        }

        return previousDirection;
    }

    public MapLocation getCenter() {
        return center;
    }
}
