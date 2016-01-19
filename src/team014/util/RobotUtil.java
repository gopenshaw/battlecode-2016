package team014.util;

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

    public static RobotInfo[] getRobotsOfType(RobotInfo[] robots, RobotType type) {
        int count = 0;
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].type == type) {
                count++;
            }
        }

        if (count == 0) {
            return null;
        }

        int index = 0;
        RobotInfo[] robotsOfType = new RobotInfo[count];
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].type == type) {
                robotsOfType[index++] = robots[i];
            }
        }

        return robotsOfType;
    }

    public static RobotInfo getRobotCanAttack(RobotInfo[] robots) {
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].type.canAttack()) {
                return robots[i];
            }
        }

        return null;
    }

    public static RobotInfo getClosestRobotToLocation(RobotInfo[] robots, MapLocation location) {
        int closestLocationDistance = Integer.MAX_VALUE;
        RobotInfo closestRobot = null;

        for (RobotInfo robot : robots) {
            int currentDistance = robot.location.distanceSquaredTo(location);
            if (currentDistance < closestLocationDistance) {
                closestLocationDistance = currentDistance;
                closestRobot = robot;
            }
        }
        return closestRobot;
    }

    public static RobotInfo getLowestHealthNonInfectedRobot(RobotInfo[] robots) {
        double minHealth = Double.MAX_VALUE;
        int minIndex = -1;

        for (int i = 0; i < robots.length; i++) {
            if (robots[i].viperInfectedTurns == 0
                    && robots[i].zombieInfectedTurns == 0
                    && robots[i].health < minHealth) {
                minIndex = i;
                minHealth = robots[i].health;
            }
        }

        return minIndex < 0 ? null : robots[minIndex];
    }

    public static RobotInfo[] getRobotsOfType(RobotInfo[] robots, RobotType type1, RobotType type2) {
        int count = 0;
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].type == type1
                    || robots[i].type == type2) {
                count++;
            }
        }

        if (count == 0) {
            return null;
        }

        int index = 0;
        RobotInfo[] robotsOfType = new RobotInfo[count];
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].type == type1
                    || robots[i].type == type2) {
                robotsOfType[index++] = robots[i];
            }
        }

        return robotsOfType;
    }

    public static boolean anyCanAttack(RobotInfo[] robots1, RobotInfo[] robots2, MapLocation currentLocation) {
        return anyCanAttack(robots1, currentLocation)
                || anyCanAttack(robots2, currentLocation);
    }

    public static boolean anyWithinRange(RobotInfo[] nearbyZombies, int range, MapLocation location) {
        int count = nearbyZombies.length;
        for (int i = 0; i < count; i++) {
            if (location.distanceSquaredTo(nearbyZombies[i].location) <= range) {
                return true;
            }
        }

        return false;
    }

    public static RobotInfo[] getRobotsOfType(RobotInfo[] robots, RobotType type1, RobotType type2, RobotType type3) {
        int count = 0;
        for (int i = 0; i < robots.length; i++) {
            RobotType robotType = robots[i].type;
            if (robotType == type1
                    || robotType == type2
                    || robotType == type3) {
                count++;
            }
        }

        if (count == 0) {
            return null;
        }

        int index = 0;
        RobotInfo[] robotsOfType = new RobotInfo[count];
        for (int i = 0; i < robots.length; i++) {
            RobotType robotType = robots[i].type;
            if (robotType == type1
                    || robotType == type2
                    || robotType == type3) {
                robotsOfType[index++] = robots[i];
            }
        }

        return robotsOfType;
    }
}
