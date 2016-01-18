package ella2.util;

import battlecode.common.MapLocation;
import ella2.MapBounds;

public class MapUtil {
    public static MapBounds getBoundsThatEncloseLocations(MapLocation[] locations, MapLocation[] locations2) {
        int north = 1000000;
        int east = 1000000;
        int south = -1000000;
        int west = -1000000;

        for (MapLocation location : locations) {
            int x = location.x;
            int y = location.y;
            if (x < east) {
                east = x;
            } else if (x > west) {
                west = x;
            }

            if (y < north) {
                north = y;
            } else if (y > south) {
                south = y;
            }
        }

        for (MapLocation location : locations2) {
            int x = location.x;
            int y = location.y;
            if (x < east) {
                east = x;
            } else if (x > west) {
                west = x;
            }

            if (y < north) {
                north = y;
            } else if (y > south) {
                south = y;
            }
        }

        return new MapBounds(north, east, south, west);
    }

    public static MapLocation getClosestToBoundary(MapLocation[] locations, MapBounds bounds) {
        int shortest = 1000000;
        MapLocation bestLocation = null;
        for (MapLocation location : locations) {
            int distance = bounds.getDistanceToBoundary(location);
            if (distance < shortest) {
                shortest = distance;
                bestLocation = location;
            }
        }

        return bestLocation;
    }
}
