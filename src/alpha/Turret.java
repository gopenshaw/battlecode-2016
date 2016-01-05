package alpha;

import battlecode.common.*;

public class Turret extends Robot {
    public Turret(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn(RobotController rc) throws GameActionException {
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

    private void convertToTTM(RobotController rc) throws GameActionException {
        rc.pack();
    }
}
