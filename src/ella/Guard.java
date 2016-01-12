package ella;

import battlecode.common.*;
import ella.util.DirectionUtil;
import ella.util.RobotUtil;

public class Guard extends Robot {
    private RobotInfo[] attackableZombies;
    private RobotInfo[] nearbyZombies;
    private RobotInfo[] adjacentTeammates;
    private RobotInfo[] nearbyTeammates;

    public Guard(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        senseRobots();
        attackZombies();
        moveTowardZombies();
        moveHome();
        clearRubble();
        moveRandom();
    }

    private void senseRobots() {
        attackableZombies = senseAttackableZombies();
        nearbyZombies = senseNearbyZombies();
        nearbyTeammates = rc.senseNearbyRobots(senseRadius, team);
        adjacentTeammates = rc.senseNearbyRobots(2, team);
    }

    private void attackZombies() throws GameActionException {
        if (!rc.isWeaponReady()) {
            return;
        }

        if (attackableZombies.length > 0) {
            rc.attackLocation(attackableZombies[0].location);
        }
    }

    private void moveTowardZombies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        int adjacentTurrets = RobotUtil.getCountOfType(adjacentTeammates, RobotType.TURRET);
        if (adjacentTurrets < 1) {
            return;
        }

        if (attackableZombies.length == 0
                && nearbyZombies.length > 0) {
            tryMoveToward(nearbyZombies[0].location);
        }
    }

    private void clearRubble() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        for (int i = 0; i < 8; i++) {
            MapLocation adjacent = currentLocation.add(directions[i]);
            if (rc.senseRubble(adjacent) >= 100) {
                rc.clearRubble(directions[i]);
                return;
            }
        }
    }

    private void moveHome() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        int adjacentTurrets = RobotUtil.getCountOfType(adjacentTeammates, RobotType.TURRET);
        if (adjacentTurrets == 0) {
            RobotInfo nearbyTurret = RobotUtil.getRobotOfType(nearbyTeammates, RobotType.TURRET);
            if (nearbyTurret != null) {
                tryMoveToward(nearbyTurret.location);
            }
        }
    }

    private void moveRandom() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        tryMove(getRandomDirection());
    }
}
