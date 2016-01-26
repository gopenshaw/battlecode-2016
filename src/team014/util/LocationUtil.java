package team014.util;

import battlecode.common.MapLocation;

public class LocationUtil {
    public static int encode(MapLocation location) {
        return (location.x % 1000) * 1000 + (location.y % 1000);
    }

    public static MapLocation decode(int i, MapLocation validLocation) {
        int x = getValidCoordinate(i / 1000, validLocation.x);
        int y = getValidCoordinate(i % 1000, validLocation.y);
        return new MapLocation(x, y);
    }

    private static int getValidCoordinate(int threeDigits, int validCoordinate) {
        int candidate = (validCoordinate / 1000) * 1000 + threeDigits;

        int currentThreeDigits = validCoordinate % 1000;
        if (currentThreeDigits < 100) {
            if (Math.abs(candidate - validCoordinate) > 100) {
                candidate -= 1000;
            }
        }
        else if (currentThreeDigits < 900) {
            if (Math.abs(candidate - validCoordinate) > 100) {
                candidate += 1000;
            }
        }

        return candidate;
    }

    public static MapLocation findAverageLocation(MapLocation[] mapLocations) {
        int x = 0;
        int y = 0;
        int count = mapLocations.length;
        for (MapLocation location : mapLocations) {
            x += location.x;
            y += location.y;
        }

        return new MapLocation(x / count, y / count);
    }

    public static MapLocation findClosestLocation(MapLocation[] partsLocations, MapLocation currentLocation) {
        int shortestDistance = Integer.MAX_VALUE;
        MapLocation closestLocation = null;

        for (MapLocation location : partsLocations) {
            int distance = currentLocation.distanceSquaredTo(location);
            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestLocation = location;
            }
        }

        return closestLocation;
    }

    public static MapLocation findClosestLocation(MapLocation[] locations, int locationCount, MapLocation currentLocation) {
        int shortestDistance = Integer.MAX_VALUE;
        MapLocation closestLocation = null;

        for (int i = 0; i < locationCount; i ++) {
            int distance = currentLocation.distanceSquaredTo(locations[i]);
            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestLocation = locations[i];
            }
        }

        return closestLocation;
    }

    public static boolean anyWithinRange(MapLocation[] nearbyTurrets, MapLocation next, int range) {
        for (int i = 0; i < nearbyTurrets.length; i++) {
            if (nearbyTurrets[i] == null) {
                return false;
            }
            else if (next.distanceSquaredTo(nearbyTurrets[i]) <= range) {
                return true;
            }
        }

        return false;
    }

    public static MapLocation findClosestDifferentLocation(MapLocation[] archonLocations, MapLocation currentLocation) {
        int shortestDistance = Integer.MAX_VALUE;
        MapLocation closestLocation = null;

        int locationCount = archonLocations.length;
        for (int i = 0; i < locationCount; i++) {
            MapLocation location = archonLocations[i];
            if (location.equals(closestLocation)) {
                continue;
            }

            int distance = currentLocation.distanceSquaredTo(location);
            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestLocation = location;
            }
        }

        return closestLocation;
    }

    public static boolean allWithinRange(MapLocation[] locations, MapLocation currentLocation, int pathRadiusSquared) {
        int locationCount = locations.length;
        for (int i = 0; i < locationCount; i++) {
            if (locations[i].distanceSquaredTo(currentLocation) > pathRadiusSquared) {
                return false;
            }
        }

        return true;
    }

    public static MapLocation findAverageLocation(MapLocation[] mapLocations, MapLocation[] mapLocations2) {
        int x = 0;
        int y = 0;
        int count = mapLocations.length;
        for (MapLocation location : mapLocations) {
            x += location.x;
            y += location.y;
        }

        count += mapLocations2.length;
        for (MapLocation location : mapLocations2) {
            x += location.x;
            y += location.y;
        }

        return new MapLocation(x / count, y / count);
    }
}
