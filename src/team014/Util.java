package team014;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class Util {
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
}
