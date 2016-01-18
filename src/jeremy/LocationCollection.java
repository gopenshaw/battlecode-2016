package jeremy;

import battlecode.common.MapLocation;

public class LocationCollection {
    private MapLocation[] memory;
    private int size;
    private boolean[][] inSet;

    public LocationCollection(int capacity) {
        memory = new MapLocation[capacity];
        size = 0;
        inSet = new boolean[100][100];
    }

    public void add(MapLocation location) {
        if (inSet[location.x % 100][location.y % 100]) {
            return;
        }

        addToMemory(location);
        for (int i = 0; i < memory.length; i++) {
            if (memory[i] == null) {
                memory[i] = location;
                size++;
                break;
            }
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public MapLocation removeClosestTo(MapLocation destination) {
        if (size == 0) {
            return null;
        }

        MapLocation closestLocation = null;
        int shortestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < memory.length; i++) {
            MapLocation currentLocation = memory[i];
            if (currentLocation == null
                    || !inSet(currentLocation)) {
                continue;
            }

            int currentDistance = currentLocation.distanceSquaredTo(destination);
            if (currentDistance < shortestDistance) {
                shortestDistance = currentDistance;
                closestLocation = currentLocation;
            }
        }

        if (closestLocation == null) {
            return null;
        }

        removeFromMemory(closestLocation);
        size--;
        return closestLocation;
    }

    public boolean inSet(MapLocation location) {
        return inSet[location.x % 100][location.y % 100];
    }

    private void addToMemory(MapLocation location) {
        inSet[location.x % 100][location.y % 100] = true;
    }

    private void removeFromMemory(MapLocation location) {
        inSet[location.x % 100][location.y % 100] = false;
    }

    public int getSize() {
        return size;
    }
}
