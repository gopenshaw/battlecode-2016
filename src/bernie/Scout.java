package bernie;

import battlecode.common.*;
import bernie.nav.SquarePath;
import bernie.util.SignalUtil;

public class Scout extends Robot {
    private static final int INITIAL_RADIUS = 4;
    private static final int RADIUS_INCREMENT = 0;
    private final int BROADCAST_RADIUS = senseRadius * 4;

    private final SquarePath squarePath;
    private int previousRotations;

    public Scout(RobotController rc) {
        super(rc);
        squarePath = new SquarePath(currentLocation, INITIAL_RADIUS, rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        broadcastZombies();

        explore();
    }

    private void explore() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (squarePath.getRotationsCompleted() > previousRotations) {
            previousRotations = squarePath.getRotationsCompleted();
            squarePath.updateRadius(squarePath.getRadius() + RADIUS_INCREMENT);
        }

        Direction circleDirection = squarePath.getNextDirection(currentLocation);
        tryMove(circleDirection);
    }

    private void broadcastZombies() throws GameActionException {
        RobotInfo[] zombies = senseNearbyZombies();
        broadcastRobots(zombies);
    }

    private void broadcastRobots(RobotInfo[] robots) throws GameActionException {
        for (RobotInfo robot : robots) {
            SignalUtil.broadcastEnemy(robot, BROADCAST_RADIUS, roundNumber, rc);

            if (rc.getMessageSignalCount() == GameConstants.MESSAGE_SIGNALS_PER_TURN) {
                break;
            }
        }
    }
}
