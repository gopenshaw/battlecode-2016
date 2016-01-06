package alpha;

import battlecode.common.*;

public class Guard extends Robot{
    private final int SIGNAL_RADIUS = RobotType.GUARD.sensorRadiusSquared * 2;

    public Guard(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn(RobotController rc) throws GameActionException {
        RobotInfo[] nearbyZombies = senseNearbyZombies();
        if (nearbyZombies.length > 0) {
            RobotInfo attackableZombie = findAttackableRobot(nearbyZombies);
            if (attackableZombie == null) {
                rc.broadcastSignal(SIGNAL_RADIUS);

                if (rc.isCoreReady()) {
                    tryMoveToward(nearbyZombies[0].location);
                }
            }
            else {
                if (rc.isWeaponReady()) {
                    rc.attackLocation(attackableZombie.location);
                }
            }

            return;
        }

        if (!rc.isCoreReady()) {
            return;
        }

        Signal[] signals = rc.emptySignalQueue();
        MapLocation teamLocation = null;
        for (Signal s : signals) {
            if (s.getTeam() == team
                    && s.getMessage() == null) {
                teamLocation = s.getLocation();
            }
        }

        if (teamLocation != null) {
            tryMoveToward(teamLocation);
        }
    }
}
