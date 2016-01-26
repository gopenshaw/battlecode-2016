package uber.util;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import uber.RobotData;

import java.util.ArrayList;

public class DirectionUtil {
    public static Direction getDirectionAwayFrom(RobotInfo[] robots, MapLocation currentLocation) {
        return getDirectionAwayFrom(robots, new RobotInfo[0], currentLocation);
    }

    public static Direction getDirectionAwayFrom(RobotInfo[] robots1, RobotInfo[] robots2, MapLocation currentLocation) {
        Direction[] directions = new Direction[robots1.length + robots2.length];
        for (int i = 0; i < robots1.length; i++) {
            MapLocation enemyLocation = robots1[i].location;
            directions[i] = enemyLocation.directionTo(currentLocation);
        }

        int offset = robots1.length;
        for (int i = 0; i < robots2.length; i++) {
            MapLocation enemyLocation = robots2[i].location;
            directions[offset + i] = enemyLocation.directionTo(currentLocation);
        }

        return getAverageDirection(directions);
    }

    public static Direction getDirectionAwayFrom(ArrayList<MapLocation> locations, MapLocation currentLocation) {
        Direction[] directions = new Direction[locations.size()];
        for (int i = 0; i < locations.size(); i++) {
            directions[i] = locations.get(i).directionTo(currentLocation);
        }

        return getAverageDirection(directions);
    }

    private static Direction getAverageDirection(Direction[] directions) {
        int x = 0;
        int y = 0;
        for (Direction d : directions) {
            x += d.dx;
            y += d.dy;
        }

        return getDirection(x, y);
    }

    public static Direction getDirection(int x, int y) {
        if (x == 0
                && y == 0) {
            return Direction.NONE;
        }

        if (x < 0) {
            if (y < 0) {
                return Direction.NORTH_WEST;
            }
            else if (y > 0) {
                return Direction.SOUTH_WEST;
            }
            else {
                return Direction.WEST;
            }
        }
        else if (x > 0) {
            if (y < 0) {
                return Direction.NORTH_EAST;
            }
            else if (y > 0) {
                return Direction.SOUTH_EAST;
            }
            else {
                return Direction.EAST;
            }
        }
        else {
            if (y < 0) {
                return Direction.NORTH;
            }
            else {
                return Direction.SOUTH;
            }
        }
    }

    public static Direction getDirectionToward(RobotInfo[] robots, MapLocation currentLocation) {
        return getDirectionAwayFrom(robots, currentLocation).opposite();
    }

    public static Direction getDirectionAwayAwayToward(
            RobotInfo[] away1, RobotInfo[] away2, RobotInfo[] toward, MapLocation currentLocation) {
        Direction[] directions = new Direction[away1.length + away2.length + toward.length];
        for (int i = 0; i < away1.length; i++) {
            MapLocation enemyLocation = away1[i].location;
            directions[i] = enemyLocation.directionTo(currentLocation);
        }

        int offset = away1.length;
        for (int i = 0; i < away2.length; i++) {
            MapLocation enemyLocation = away2[i].location;
            directions[offset + i] = enemyLocation.directionTo(currentLocation);
        }

        offset += away2.length;
        for (int i = 0; i < toward.length; i++) {
            MapLocation towardLocation = toward[i].location;
            directions[offset + i] = currentLocation.directionTo(towardLocation);
        }

        return getAverageDirection(directions);
    }

    public static Direction getDirectionAwayFrom(RobotData[] enemyTurrets, MapLocation currentLocation) {
        int size = 0;
        for (int i = 0; i < enemyTurrets.length; i++) {
            if (enemyTurrets[i] == null) {
                break;
            }
            else {
                size++;
            }
        }

        Direction[] directions = new Direction[size];
        for (int i = 0; i < size; i++) {
            MapLocation enemyLocation = enemyTurrets[i].location;
            directions[i] = enemyLocation.directionTo(currentLocation);
        }

        return getAverageDirection(directions);
    }

    public static Direction getDirectionToward(RobotData[] enemyTurrets, int size, MapLocation currentLocation) {
        Direction[] directions = new Direction[size];
        for (int i = 0; i < size; i++) {
            directions[i] = currentLocation.directionTo(enemyTurrets[i].location);
        }

        return getAverageDirection(directions);
    }
}
