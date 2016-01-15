package ella;

import battlecode.common.MapLocation;

public class MapBounds {
    private int north;
    private int east;
    private int south;
    private int west;

    public MapBounds(int north, int east, int south, int west) {
        this.north = north;
        this.east = east;
        this.south = south;
        this.west = west;
    }

    public int getDistanceToBoundary(MapLocation location) {
        int shortest = 1000000;
        shortest = Math.min(shortest, location.y - north);
        shortest = Math.min(shortest, south - location.y);
        shortest = Math.min(shortest, location.x - east);
        shortest = Math.min(shortest, west - location.x);
        return shortest;
    }

    @Override
    public String toString() {
        return "north: " + north + " east: " + east + " south: " + south + " west: " + west;
    }
}
