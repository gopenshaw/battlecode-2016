package ella;

import battlecode.common.*;
import ella.util.ZombieUtil;

public class Turret extends Robot{
    private Signal[] roundSignals;
    private MapLocation requestLocation;

    public Turret(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        attackEnemiesAndZombies();
        requestSpace();
        spreadOut();
    }

    private void requestSpace() throws GameActionException {
        if (rc.getType() == RobotType.TTM) {
            return;
        }

        RobotInfo[] neighbors = rc.senseNearbyRobots(2, team);
        if (neighbors.length >= 6) {
            rc.broadcastSignal(2);
        }
    }

    private void spreadOut() throws GameActionException {
        if (requestLocation == null) {
            for (Signal s : roundSignals) {
                if (s.getTeam() == team
                        && s.getMessage() == null) {
                    requestLocation = s.getLocation();
                    rc.pack();
                    return;
                }
            }
        }

        if (requestLocation == null
                || !rc.isCoreReady()) {
            return;
        }

        if (rc.getType() == RobotType.TTM) {
            tryMove(requestLocation.directionTo(currentLocation));
            rc.unpack();
            requestLocation = null;
        }
    }

    private void attackEnemiesAndZombies() throws GameActionException {
        if (rc.getType() == RobotType.TTM) {
            return;
        }

        if (!rc.isWeaponReady()) {
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        RobotInfo zombieToAttack = getPriorityAttackableZombie(nearbyZombies);
        if (zombieToAttack != null) {
            rc.attackLocation(zombieToAttack.location);
            return;
        }

        RobotInfo[] nearbyEnemies = senseNearbyEnemies();
        if (nearbyEnemies.length > 0) {
            if (rc.canAttackLocation(nearbyEnemies[0].location)) {
                rc.attackLocation(nearbyEnemies[0].location);
                return;
            }
        }
    }

    private RobotInfo getPriorityAttackableZombie(RobotInfo[] zombies) {
        RobotInfo zombieToAttack = null;
        int highestPriority = -1;
        for (RobotInfo r : zombies) {
            int priority = ZombieUtil.getAttackPriority(r.type);
            if (priority > highestPriority
                    && rc.canAttackLocation(r.location)) {
                zombieToAttack = r;
                highestPriority = priority;
            }
        }

        return zombieToAttack;
    }
}
