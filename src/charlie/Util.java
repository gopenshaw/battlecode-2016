package charlie;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

import java.util.ArrayList;

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

    public static RobotInfo getLowestHealthAttackableRobot(RobotInfo[] robots, RobotController rc) {
        double minHealth = Double.MAX_VALUE;
        int minIndex = -1;

        for (int i = 0; i < robots.length; i++) {
            if (!rc.canAttackLocation(robots[i].location)) continue;

            if (robots[i].health < minHealth) {
                minIndex = i;
                minHealth = robots[i].health;
            }
        }

        return minIndex < 0 ? null : robots[minIndex];
    }

    public static RobotInfo getLowestHealthRobot(ArrayList<RobotInfo> robots) {
        double minHealth = Double.MAX_VALUE;
        int minIndex = -1;

        for (int i = 0; i < robots.size(); i++) {
            if (robots.get(i).health < minHealth) {
                minIndex = i;
                minHealth = robots.get(i).health;
            }
        }

        return minIndex < 0 ? null : robots.get(minIndex);
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
