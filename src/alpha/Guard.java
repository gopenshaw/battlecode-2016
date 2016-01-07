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
        RobotInfo[] nearbyEnemies = senseNearbyEnemies();

        RobotInfo robotToAttack = findAttackableZombieOrRobot(nearbyZombies, nearbyEnemies);
        if (robotToAttack != null
                && rc.isWeaponReady()) {
            rc.attackLocation(robotToAttack.location);
            return;
        }

        if (nearbyZombies.length > 0) {
            rc.broadcastSignal(SIGNAL_RADIUS);
        }

        if (!rc.isCoreReady()) {
            return;
        }

        MapLocation helpLocation = getHelpLocation(rc);
        if (helpLocation != null) {
            tryMoveToward(helpLocation);
        }
    }

    private MapLocation getHelpLocation(RobotController rc) {
        Signal[] signals = rc.emptySignalQueue();
        MapLocation helpLocation = null;
        for (Signal s : signals) {
            if (s.getTeam() == team
                    && s.getMessage() == null) {
                helpLocation = s.getLocation();
            }
        }
        return helpLocation;
    }
}
