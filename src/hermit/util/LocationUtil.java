package hermit.util;

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
}
