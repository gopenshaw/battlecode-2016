package bernie;

import battlecode.common.*;
import bernie.nav.CirclePath;
import bernie.util.SignalUtil;

public class Scout extends Robot {
    private final CirclePath circlePath;

    public Scout(RobotController rc) {
        super(rc);
        circlePath = new CirclePath(currentLocation, 25, rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        broadcastZombies();

        if (rc.isCoreReady()) {
            tryMove(circlePath.getNextDirection(currentLocation));
        }
    }

    private void broadcastZombies() throws GameActionException {
        RobotInfo[] zombies = senseNearbyZombies();
        broadcastRobots(zombies);
    }

    private void broadcastRobots(RobotInfo[] robots) throws GameActionException {
        for (RobotInfo robot : robots) {
            if (rc.getMessageSignalCount() > GameConstants.MESSAGE_SIGNALS_PER_TURN) {
                break;
            }

            SignalUtil.broadcastEnemy(robot, senseRadius * 2, roundNumber, rc);
        }
    }
}
