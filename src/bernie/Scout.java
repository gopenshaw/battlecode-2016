package bernie;

import battlecode.common.*;
import bernie.nav.SquarePath;
import bernie.util.SignalUtil;

public class Scout extends Robot {
    private final SquarePath squarePath;
    private final int BROADCAST_RADIUS = senseRadius * 4;
    StringBuilder sb = new StringBuilder();

    public Scout(RobotController rc) {
        super(rc);
        squarePath = new SquarePath(currentLocation, 1000, rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        broadcastZombies();

        setIndicatorString(0, sb.toString());
        sb = new StringBuilder();

        if (rc.isCoreReady()) {
            Direction circleDirection = squarePath.getNextDirection(currentLocation);
            setIndicatorString(2, "circle direction: " + circleDirection);
            tryMoveClockwise(circleDirection);
        }
    }

    private void broadcastZombies() throws GameActionException {
        RobotInfo[] zombies = senseNearbyZombies();
        broadcastRobots(zombies);
    }

    private void broadcastRobots(RobotInfo[] robots) throws GameActionException {
        for (RobotInfo robot : robots) {
            SignalUtil.broadcastEnemy(robot, BROADCAST_RADIUS, roundNumber, rc);
            sb.append(robot.location + "; ");

            if (rc.getMessageSignalCount() == GameConstants.MESSAGE_SIGNALS_PER_TURN) {
                break;
            }
        }
    }
}
