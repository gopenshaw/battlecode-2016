package alpha;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Guard extends Robot{
    public Guard(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn(RobotController rc) throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        if (nearbyZombies.length > 0) {
            MapLocation zombieLocation = nearbyZombies[0].location;
            if (rc.canAttackLocation(zombieLocation)) {
                rc.attackLocation(zombieLocation);
            }
            else {
                tryMoveToward(zombieLocation);
            }
        }
    }
}
