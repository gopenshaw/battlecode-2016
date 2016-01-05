package alpha;

import battlecode.common.*;

public class Archon extends Robot {
    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    public void doTurn(RobotController rc) throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        if (nearbyZombies.length > 0) {
            Direction away = DirectionUtil.getDirectionAwayFrom(nearbyZombies, rc);
            tryMove(away);
            return;
        }

        if (rc.getTeamParts() > RobotType.GUARD.partCost) {
            tryBuild(RobotType.GUARD);
        }
    }
}
