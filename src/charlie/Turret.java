package charlie;

import battlecode.common.*;

public class Turret extends Robot {
    public Turret(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        Signal[] signals = rc.emptySignalQueue();

        if (!rc.isWeaponReady()) {
            return;
        }

        MapLocation attackLocation = checkBroadcastForEnemy(signals);

        if (attackLocation != null) {
            setIndicatorString(0, "health " + " " + attackLocation);
            rc.attackLocation(attackLocation);
        }
        else {
            RobotInfo[] zombies = senseNearbyZombies();
            RobotInfo[] enemies = senseNearbyEnemies();
            RobotInfo robotToAttack = findAttackableZombieOrRobot(zombies, enemies);
            if (robotToAttack != null
                && rc.canAttackLocation(robotToAttack.location)) {
                rc.attackLocation(robotToAttack.location);
            }
        }
    }

    private MapLocation checkBroadcastForEnemy(Signal[] signals) {
        MapLocation attackLocation = null;
        int lowestHealth = 1000000;
        for (int i = 0; i < signals.length; i++) {
            if (signals[i].getTeam() == team
                    && SignalUtil.getType(signals[i]) == SignalType.ENEMY) {
                RobotData robotData = SignalUtil.getRobotData(signals[i], currentLocation);
                if (robotData.health < lowestHealth
                        && rc.canAttackLocation(robotData.location)) {
                    attackLocation = robotData.location;
                    lowestHealth = robotData.health;
                }
            }
        }
        return attackLocation;
    }
}
