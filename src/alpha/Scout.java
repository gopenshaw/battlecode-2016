package alpha;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Scout extends Robot {
    public Scout(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn(RobotController rc) throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        Direction randomDirection = getRandomDirection();
        tryMove(randomDirection);
    }
}
