package francis.util;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class RobotUtil {
    public static RobotInfo getLowestHealthRobot(RobotInfo[] robots) {
        double minHealth = Double.MAX_VALUE;
        int minIndex = -1;

        for (int i = 0; i < robots.length; i++) {
            if (robots[i].health < minHealth) {
                minIndex = i;
                minHealth = robots[i].health;
            }
        }

        return minIndex < 0 ? null : robots[minIndex];
    }

    public static boolean anyCanAttack(RobotInfo[] robots, MapLocation location) {
        for (RobotInfo robot : robots) {
            if (robot.type.canAttack()
                    && robot.location.distanceSquaredTo(location) <= robot.type.attackRadiusSquared) {
                return true;
            }
        }

        return false;
    }

    public static boolean anyCanAttack(RobotInfo[] robots) {
        for (RobotInfo robot : robots) {
            if (robot.type.canAttack()) {
                return true;
            }
        }

        return false;
    }

    public static int getCountOfType(RobotInfo[] robots, RobotType type) {
        int count = 0;
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].type == type) {
                count++;
            }
        }

        return count;
    }

    public static RobotInfo getRobotOfType(RobotInfo[] robots, RobotType type) {
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].type == type) {
                return robots[i];
            }
        }

        return null;
    }

    public static RobotInfo getLowestHealthNonInfectedRobot(RobotInfo[] attackableEnemies) {
        double lowestHealth = 1000000;
        RobotInfo robotToAttack = null;
        for (int i = 0; i < attackableEnemies.length; i++) {
            RobotInfo currentRobot = attackableEnemies[i];
            if (currentRobot.viperInfectedTurns > 0
                    || currentRobot.zombieInfectedTurns > 0) {
                continue;
            }

            if (currentRobot.health < lowestHealth) {
                lowestHealth = currentRobot.health;
                robotToAttack = currentRobot;
            }
        }

        return robotToAttack;
    }

    public static boolean robotCanAttackZombie(RobotInfo robot, RobotInfo zombie) {
        if (!robot.type.canAttack()) {
            return false;
        }

        if (robot.type == RobotType.TURRET) {
            return (robot.location.distanceSquaredTo(zombie.location) > 5);
        }

        return true;
    }
}
