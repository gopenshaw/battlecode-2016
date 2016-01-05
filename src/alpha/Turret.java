package alpha;

import battlecode.common.*;

public class Turret extends Robot {
    public Turret(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn(RobotController rc) throws GameActionException {
        updateType(rc);

        if (rc.getType() == RobotType.TTM) {
            doTTMTurn(rc);
            return;
        }

        if (!rc.isCoreReady()) {
            return;
        }

        if (!rc.isWeaponReady()) {
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        if (nearbyZombies.length > 1) {
            convertToTTM(rc);
            return;
        }

        if (nearbyZombies.length > 0) {
            MapLocation zombieLocation = nearbyZombies[0].location;
            if (rc.canAttackLocation(zombieLocation)) {
                rc.attackLocation(zombieLocation);
            }
        }
    }

    private void doTTMTurn(RobotController rc) throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        if (nearbyZombies.length > 0) {
            Direction away = DirectionUtil.getDirectionAwayFrom(nearbyZombies, rc);
            tryMove(away);
        }
    }

    private void convertToTTM(RobotController rc) throws GameActionException {
        rc.pack();
    }
}
