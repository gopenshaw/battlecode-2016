package bernie.util;

import battlecode.common.MapLocation;

public class AverageMapLocation {
    private final BoundedQueue<MapLocation> queue;

    private int sumX;
    private int sumY;

    public AverageMapLocation(int capacity) {
        this.queue = new BoundedQueue<MapLocation>(capacity);
    }

    public MapLocation getAverage() {
        if (queue.isEmpty()) {
            return null;
        }

        int currentSize = queue.getSize();
        return new MapLocation(sumX / currentSize, sumY / currentSize);
    }

    public void add(MapLocation location) {
        if (queue.isFull()) {
            MapLocation old = queue.remove();
            sumX -= old.x;
            sumY -= old.y;
        }

        queue.add(location);
        sumX += location.x;
        sumY += location.y;
    }
}
