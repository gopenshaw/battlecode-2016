package scout;

import battlecode.common.*;

public class RobotPlayer {

    private static RobotController rc;
    private static MapLocation currentLocation;
    static CirclePath circlePath;

    public static void run(RobotController rcIn) {
        rc = rcIn;
        circlePath = new CirclePath(rc.getLocation(), 4, rc);

        while (true) {
            try {
                doRound();
                Clock.yield();
            } catch (GameActionException e) {
                e.printStackTrace();
            }
        }
    }

    private static void doRound() throws GameActionException {
        currentLocation = rc.getLocation();
        moveInCircle();
    }

    private static void moveInCircle() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        rc.setIndicatorString(0, "rotations completed " + circlePath.getRotationsCompleted());
        tryMove(circlePath.getNextDirection(currentLocation));
    }

    static protected void tryMove(Direction direction) throws GameActionException {
        if (rc.canMove(direction)) {
            rc.move(direction);
            return;
        }

        Direction left = direction.rotateLeft();
        if (rc.canMove(left)) {
            rc.move(left);
            return;
        }

        Direction right = direction.rotateRight();
        if (rc.canMove(right)) {
            rc.move(right);
            return;
        }

        for (int i = 0; i < 2; i++) {
            left = left.rotateLeft();
            if (rc.canMove(left)) {
                rc.move(left);
                return;
            }

            right = right.rotateRight();
            if (rc.canMove(right)) {
                rc.move(right);
                return;
            }
        }
    }
}
