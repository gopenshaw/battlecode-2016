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

        RobotInfo[] zombies = senseNearbyZombies();
        if (zombies.length > 0) {
            RobotInfo priorityZombie = getPriorityAttackableZombie(zombies);
            if (priorityZombie != null) {
                rc.attackLocation(priorityZombie.location);
                return;
            }
        }

        RobotInfo[] attackableEnemies = senseAttackableEnemies();
        if (attackableEnemies.length > 0) {
            RobotInfo robotToAttack = Util.getLowestHealthRobot(attackableEnemies);
            if (robotToAttack != null) {
                rc.attackLocation(robotToAttack.location);
                return;
            }
        }

        MapLocation attackLocation = checkBroadcastForEnemy(signals);

        if (attackLocation != null) {
            setIndicatorString(0, "health " + " " + attackLocation);
            rc.attackLocation(attackLocation);
        }
    }

    private RobotInfo[] senseAttackableEnemies() {
        return rc.senseNearbyRobots(attackRadius, enemy);
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

    private MapLocation checkBroadcastForEnemy(Signal[] signals) {
        MapLocation attackLocation = null;
        int lowestHealth = 1000000;
        for (int i = 0; i < signals.length; i++) {
            if (signals[i].getTeam() == team
                    && SignalUtil.getType(signals[i]) == SignalType.ENEMY
                    && SignalUtil.getRoundNumber(signals[i]) == roundNumber) {
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
