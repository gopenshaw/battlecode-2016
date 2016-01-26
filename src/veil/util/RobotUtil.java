package veil.util;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import veil.RobotData;

public class RobotUtil {
    private static final RobotType[] ENEMY_PRIORITY = {
            RobotType.VIPER, RobotType.SOLDIER, RobotType.GUARD,
            RobotType.TTM, RobotType.TURRET, RobotType.ARCHON
    };

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

    public static RobotInfo[] getRobotsCanAttack(RobotInfo[] robots, MapLocation location) {
        int canAttackCount = 0;
        int robotCount = robots.length;
        for (int i = 0; i < robotCount; i++) {
            RobotInfo robot = robots[i];
            if (robot != null
                    && robot.type.canAttack()
                    && robot.location.distanceSquaredTo(location) <= robot.type.attackRadiusSquared) {
                canAttackCount++;
            }
        }

        RobotInfo[] robotsCanAttack = new RobotInfo[canAttackCount];
        int index = 0;
        for (int i = 0; i < robotCount; i++) {
            RobotInfo robot = robots[i];
            if (robot != null
                    && robot.type.canAttack()
                    && robot.location.distanceSquaredTo(location) <= robot.type.attackRadiusSquared) {
                robotsCanAttack[index++] = robot;
            }
        }

        return robotsCanAttack;
    }

    public static boolean anyCanAttack(RobotInfo[] robots, MapLocation location) {
        for (RobotInfo robot : robots) {
            if (robot != null
                    && robot.type.canAttack()
                    && robot.location.distanceSquaredTo(location) <= robot.type.attackRadiusSquared) {
                return true;
            }
        }

        return false;
    }

    public static boolean anyCanAttackConsideringTTMsTurrets(RobotInfo[] robots, MapLocation location) {
        for (RobotInfo robot : robots) {
            if (robot == null) {
                continue;
            }

            RobotType type = robot.type;
            if (type == RobotType.TTM) {
                if (robot.location.distanceSquaredTo(location) <= RobotType.TURRET.attackRadiusSquared) {
                    return true;
                }
            }
            else {
                if (type.canAttack()
                    && robot.location.distanceSquaredTo(location) <= type.attackRadiusSquared) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean anyCanAttack(RobotData[] robots, MapLocation location) {
        int count = robots.length;
        for (int i = 0; i < count; i++) {
            RobotData robot = robots[i];
            if (robot != null
                    && robot.location.distanceSquaredTo(location) <= robot.type.attackRadiusSquared) {
                return true;
            }
        }

        return false;
    }

    public static boolean anyCanAttack(RobotData[] robots, int count, MapLocation location) {
        for (int i = 0; i < count; i++) {
            RobotData robot = robots[i];
            if (robot != null
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

    public static RobotInfo getRobotOfType(RobotInfo[] robots, RobotType type1, RobotType type2) {
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].type == type1
                    || robots[i].type == type2) {
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
            return new RobotInfo[0];
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

    public static BoundedQueue<RobotInfo> getEnemiesThatCanAttack(RobotInfo[] robots, MapLocation currentLocation, int maxEnemies) {
        BoundedQueue<RobotInfo> canAttack = new BoundedQueue<RobotInfo>(maxEnemies);
        int count = robots.length;
        for (int i = 0; i < count; i++) {
            RobotInfo enemy = robots[i];
            if (enemy.type.canAttack()
                    && enemy.location.distanceSquaredTo(currentLocation) <= enemy.type.attackRadiusSquared) {
                canAttack.add(robots[i]);
                if (canAttack.isFull()) {
                    return canAttack;
                }
            }
        }

        return canAttack;
    }

    public static int countCanAttack(RobotInfo[] nearbyFriendlies, BoundedQueue<RobotInfo> enemiesCanAttackMe) {
        int canAttack = 0;
        for (int i = 0; i < nearbyFriendlies.length; i++) {
            if (RobotUtil.canAttackAny(nearbyFriendlies[i], enemiesCanAttackMe)) {
                canAttack++;
            }
        }

        return canAttack;
    }

    private static boolean canAttackAny(RobotInfo attacker, BoundedQueue<RobotInfo> targets) {
        int size = targets.getSize();
        for (int i = 0; i < size; i++) {
            RobotInfo target = targets.remove();
            targets.add(target);
            if (attacker.type.canAttack()
                    && attacker.location.distanceSquaredTo(target.location) <= attacker.type.attackRadiusSquared) {
                return true;
            }
        }

        return false;
    }

    public static boolean canAttackInOneMove(RobotInfo[] nearbyZombies, MapLocation currentLocation) {
        int count = nearbyZombies.length;
        for (int i = 0; i < count; i++) {
            RobotType robotType = nearbyZombies[i].type;
            if (!robotType.canAttack()) {
                continue;
            }

            MapLocation enemyPosition = nearbyZombies[i].location;
            MapLocation nextPosition = enemyPosition.add(enemyPosition.directionTo(currentLocation));
            if (nextPosition.distanceSquaredTo(currentLocation) <= robotType.attackRadiusSquared) {
                return true;
            }
        }

        return false;
    }

    public static MapLocation findAverageLocation(RobotInfo[] nearbyFriendlies) {
        int count = nearbyFriendlies.length;
        if (count == 0) {
            return null;
        }

        int x = 0;
        int y = 0;
        for (int i = 0; i < count; i++) {
            MapLocation location = nearbyFriendlies[i].location;
            x += location.x;
            y += location.y;
        }

        return new MapLocation(x / count, y / count);
    }

    public static RobotInfo[] removeRobots(RobotInfo[] robots, RobotData[] robotsToRemove) {
        int removeCount = 0;
        int robotCount = robots.length;
        for (int i = 0; i < robotCount; i++) {
            if (RobotUtil.robotInCollection(robots[i], robotsToRemove)) {
                removeCount++;
            }
        }

        int newCount = robotCount - removeCount;
        RobotInfo[] trimmed = new RobotInfo[newCount];
        int index = 0;
        for (int i = 0; i < robotCount; i++) {
            if (!RobotUtil.robotInCollection(robots[i], robotsToRemove)) {
                trimmed[index++] = robots[i];
            }
        }

        return trimmed;
    }

    private static boolean robotInCollection(RobotInfo robot, RobotData[] robotCollection) {
        for (int i = 0; i < robotCollection.length; i++) {
            if (robotCollection[i] == null) {
                return false;
            }
            else {
                if (robot.ID == robotCollection[i].id) {
                    return true;
                }
            }
        }

        return false;
    }

    public static RobotInfo[] removeRobotsOfType(RobotInfo[] robots, RobotType typeToRemove) {
        int countToRemove = 0;
        int robotCount = robots.length;
        for (int i = 0; i < robotCount; i++) {
            if (robots[i].type == typeToRemove) countToRemove++;
        }

        RobotInfo[] trimmed = new RobotInfo[robotCount - countToRemove];
        int index = 0;
        for (int i = 0; i < robotCount; i++) {
            if (robots[i].type != typeToRemove) {
                trimmed[index++] = robots[i];
            }
        }

        return trimmed;
    }

    public static RobotInfo[] removeRobotsOfType(RobotInfo[] robots, RobotType typeToRemove1, RobotType typeToRemove2) {
        int countToRemove = 0;
        int robotCount = robots.length;
        for (int i = 0; i < robotCount; i++) {
            if (robots[i].type == typeToRemove1
                    || robots[i].type == typeToRemove2) countToRemove++;
        }

        RobotInfo[] trimmed = new RobotInfo[robotCount - countToRemove];
        int index = 0;
        for (int i = 0; i < robotCount; i++) {
            if (robots[i].type != typeToRemove1
                    && robots[i].type != typeToRemove2) {
                trimmed[index++] = robots[i];
            }
        }

        return trimmed;
    }

    public static RobotInfo getHighestPriorityEnemyUnit(RobotInfo[] enemies) {
        int highestPriority = -1;
        int enemyCount = enemies.length;
        RobotInfo highestRobot = null;
        for (int i = 0; i < enemyCount; i++) {
            int currentPriority = RobotUtil.getPriority(enemies[i].type);
            if (currentPriority > highestPriority) {
                highestPriority = currentPriority;
                highestRobot = enemies[i];
            }
        }

        return highestRobot;
    }

    public static int getPriority(RobotType type) {
        for (int i = 0; i < ENEMY_PRIORITY.length; i++) {
            if (type == ENEMY_PRIORITY[i]) {
                return i;
            }
        }

        return -1;
    }

    public static RobotData getClosestRobotToLocation(RobotData[] robots, int robotCount, MapLocation currentLocation) {
        RobotData closest = null;
        int closestDistance = 1000000;
        for (int i = 0; i < robotCount; i++) {
            int currentDistance = currentLocation.distanceSquaredTo(robots[i].location);
            if (currentDistance < closestDistance) {
                closestDistance = currentDistance;
                closest = robots[i];
            }
        }

        return closest;
    }

    public static int countMoveReady(RobotInfo[] adjacentTeammates) {
        int count = 0;
        int total = adjacentTeammates.length;
        for (int i = 0; i < total; i++) {
            if (adjacentTeammates[i].coreDelay < 1) {
                count++;
            }
        }

        return count;
    }

    public static RobotInfo getClosestRobotToLocation(RobotInfo[] robots, RobotInfo[] robots2, MapLocation currentLocation) {
        RobotInfo closest = null;
        int closestDistance = 1000000;
        int robotCount = robots.length;
        for (int i = 0; i < robotCount; i++) {
            int currentDistance = currentLocation.distanceSquaredTo(robots[i].location);
            if (currentDistance < closestDistance) {
                closestDistance = currentDistance;
                closest = robots[i];
            }
        }

        robotCount = robots2.length;
        for (int i = 0; i < robotCount; i++) {
            int currentDistance = currentLocation.distanceSquaredTo(robots2[i].location);
            if (currentDistance < closestDistance) {
                closestDistance = currentDistance;
                closest = robots2[i];
            }
        }

        return closest;
    }

    public static RobotInfo[] getRobotsCloserToUs(RobotInfo[] robots, RobotInfo[] nearbyFriendlies, RobotInfo[] nearbyEnemies) {
        int length = robots.length;
        int index[] = new int[length];
        for (int i = 0; i < length; i++) {
            index[i] = -1;
        }

        int count = 0;
        for (int i = 0; i < length; i++) {
            int ourDistance = RobotUtil.getShortestDistance(robots[i], nearbyFriendlies);
            int theirDistance = RobotUtil.getShortestDistance(robots[i], nearbyEnemies);
            if (ourDistance <= theirDistance) {
                index[i] = count++;
            }
        }

        RobotInfo[] closerToUs = new RobotInfo[count];
        for (int i = 0; i < length; i++) {
            if (index[i] == -1) {
                continue;
            }

            closerToUs[index[i]] = robots[i];
        }

        return closerToUs;
    }

    private static int getShortestDistance(RobotInfo robot, RobotInfo[] robots) {
        int shortest = 1000000;
        int length = robots.length;
        MapLocation loc = robot.location;
        for (int i = 0; i < length; i++) {
            int distance = loc.distanceSquaredTo(robots[i].location);
            if (distance < shortest) {
                shortest = distance;
            }
        }

        return shortest;
    }

    public static RobotInfo[] removeRobotsOfType(RobotInfo[] robots,
                                                 RobotType typeToRemove1,
                                                 RobotType typeToRemove2,
                                                 RobotType typeToRemove3) {
        int countToRemove = 0;
        int robotCount = robots.length;
        for (int i = 0; i < robotCount; i++) {
            RobotType type = robots[i].type;
            if (type == typeToRemove1
                    || type == typeToRemove2
                    || type == typeToRemove3) countToRemove++;
        }

        RobotInfo[] trimmed = new RobotInfo[robotCount - countToRemove];
        int index = 0;
        for (int i = 0; i < robotCount; i++) {
            RobotType type = robots[i].type;
            if (type != typeToRemove1
                    && type != typeToRemove2
                    && type != typeToRemove3) {
                trimmed[index++] = robots[i];
            }
        }

        return trimmed;
    }

    public static boolean anyInfected(RobotInfo[] robots) {
        int count = robots.length;
        for (int i = 0; i < count; i++) {
            if (robots[i].viperInfectedTurns > 0
                    || robots[i].zombieInfectedTurns > 0) {
                return true;
            }
        }

        return false;
    }

    public static RobotInfo[] getRobotsAreInfected(RobotInfo[] robots) {
        int count = 0;
        int length = robots.length;
        for (int i = 0; i < length; i++) {
            if (robots[i].viperInfectedTurns > 0
                    || robots[i].zombieInfectedTurns > 0) {
                count++;
            }
        }

        RobotInfo[] infected = new RobotInfo[count];
        int index = 0;
        for (int i = 0; i < length; i++) {
            if (robots[i].viperInfectedTurns > 0
                    || robots[i].zombieInfectedTurns > 0) {
                infected[index++] = robots[i];
            }
        }

        return infected;
    }

    public static RobotInfo getLowestHealthRobotNotOfType(RobotInfo[] robots, RobotType type) {
        double minHealth = Double.MAX_VALUE;
        int minIndex = -1;

        for (int i = 0; i < robots.length; i++) {
            RobotInfo robot = robots[i];
            if (robot.type == type) {
                continue;
            }

            if (robot.viperInfectedTurns == 0
                    && robot.zombieInfectedTurns == 0
                    && robot.health < minHealth) {
                minIndex = i;
                minHealth = robot.health;
            }
        }

        return minIndex < 0 ? null : robots[minIndex];
    }

    public static int getCountOfType(RobotInfo[] robots, RobotType type1, RobotType type2) {
        int count = 0;
        for (int i = 0; i < robots.length; i++) {
            RobotType type = robots[i].type;
            if (type == type1
                    || type == type2) {
                count++;
            }
        }

        return count;
    }
}
