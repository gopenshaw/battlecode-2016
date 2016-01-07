package alpha;

import battlecode.common.*;

public class Turret extends Robot {
    public Turret(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn(RobotController rc) throws GameActionException {
        updateTypeParams(rc);

        if (rc.getType() == RobotType.TTM) {
            doTTMTurn(rc);
            return;
        }

        if (!rc.isWeaponReady()) {
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        if (nearbyZombies.length > 0) {
            RobotInfo zombieToAttack = getBestAttackableZombie(nearbyZombies, rc);
            if (zombieToAttack != null) {
                rc.attackLocation(zombieToAttack.location);
                return;
            }
        }

        RobotInfo[] attackableEnemies = senseAttackableEnemies();
        if (attackableEnemies.length > 0) {
            RobotInfo robotToAttack = getLowestHealthAttackableRobot(attackableEnemies, rc);
            if (robotToAttack != null) {
                rc.attackLocation(robotToAttack.location);
                return;
            }
        }
    }

    private RobotInfo getLowestHealthAttackableRobot(RobotInfo[] robots, RobotController rc) {
        double lowestHealth = 1000000;
        RobotInfo lowestAttackable = null;
        for (RobotInfo robot : robots) {
            if (rc.canAttackLocation(robot.location)) {
                double health = robot.health;
                if (health < lowestHealth) {
                    lowestHealth = health;
                    lowestAttackable = robot;
                }
            }
        }

        return lowestAttackable;
    }

    private RobotInfo getBestAttackableZombie(RobotInfo[] zombies, RobotController rc) {
        //--If a ranged zombie exists, return the ranged zombie with lowest health
        //--Otherwise return the zombie with lowest health
        RobotInfo regularZombie = null;
        RobotInfo priorityZombie = null;
        double regularHealth = 10000;
        double priorityHealth = 10000;

        for (RobotInfo zombie : zombies) {
            if (rc.canAttackLocation(zombie.location)) {
                if (zombie.type != RobotType.STANDARDZOMBIE) {
                    if (zombie.health < priorityHealth) {
                        priorityHealth = zombie.health;
                        priorityZombie = zombie;
                    }
                }
                else if (priorityZombie != null) {
                    continue;
                }
                else if (zombie.health < regularHealth){
                    regularZombie = zombie;
                    regularHealth = zombie.health;
                }
            }
        }

        return priorityZombie == null ? regularZombie : priorityZombie;
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
