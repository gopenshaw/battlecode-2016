package charlie2;

import battlecode.common.*;

public class Turret extends Robot {
    public Turret(RobotController rc) {
        super(rc);
    }

    MapLocation broadcastedLocationToAttack = null;
    MapLocation nextLocationToAttack = null;
    boolean packed = false;
    int attackRadius = RobotType.TURRET.attackRadiusSquared;

    @Override
    protected void doTurn() throws GameActionException {
        Signal[] signals = rc.emptySignalQueue();
        for (Signal signal : signals) {
            if (signal.getTeam() == team && SignalUtil.getType(signal) == SignalType.ENEMY) {
                RobotData robotData = SignalUtil.getRobotData(signal, currentLocation);
                nextLocationToAttack = robotData.location;
            }
        }

        RobotInfo enemyInSight = null;
        RobotInfo[] nearbyPeople = rc.senseNearbyRobots();
        for (RobotInfo person : nearbyPeople) {
            if ((person.team == enemy || person.team == Team.ZOMBIE)
                    && currentLocation.distanceSquaredTo(person.location) <= attackRadius) {
                enemyInSight = person;
            }
        }

        if (enemyInSight != null) {
            if (packed) {
                rc.unpack();
                packed = false;
            }
            if (rc.isWeaponReady() && rc.isCoreReady()) {
                if (rc.canAttackLocation(enemyInSight.location)) {
                    rc.attackLocation(enemyInSight.location);
                }
            }
        } else if (broadcastedLocationToAttack != null) {
            if (currentLocation.distanceSquaredTo(broadcastedLocationToAttack) >= RobotType.TURRET.sensorRadiusSquared) {
                if (!packed) {
                    rc.pack();
                    packed = true;
                }
                if (rc.isCoreReady()) {
                    tryMoveToward(broadcastedLocationToAttack);
                }
            } else {
                broadcastedLocationToAttack = null;
            }
        } else {
            broadcastedLocationToAttack = nextLocationToAttack;
        }
    }
}
