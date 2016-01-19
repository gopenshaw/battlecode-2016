package team014;

import battlecode.common.MapLocation;

public class LocationCollection {
    private RobotData[] memory;
    private int size;
    private boolean[] inSet;

    public LocationCollection(int capacity) {
        memory = new RobotData[capacity];
        size = 0;
        inSet = new boolean[32001];
    }

    public void add(RobotData robot) {
        if (inSet[robot.id]) {
            return;
        }

        for (int i = 0; i < memory.length; i++) {
            if (memory[i] == null) {
                memory[i] = robot;
                addToMemory(robot);
                size++;
                break;
            }
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public RobotData removeClosestTo(MapLocation destination) {
        if (size == 0) {
            return null;
        }

        RobotData closestRobot = null;
        int shortestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < memory.length; i++) {
            RobotData currentRobot = memory[i];
            if (currentRobot == null
                    || !inSet(currentRobot)) {
                continue;
            }

            MapLocation currentLocation = currentRobot.location;
            int currentDistance = currentLocation.distanceSquaredTo(destination);
            if (currentDistance < shortestDistance) {
                shortestDistance = currentDistance;
                closestRobot = currentRobot;
            }
        }

        if (closestRobot == null) {
            return null;
        }

        removeFromMemory(closestRobot);
        size--;
        return closestRobot;
    }

    public boolean inSet(RobotData robot) {
        return inSet[robot.id];
    }

    private void addToMemory(RobotData robot) {
        inSet[robot.id] = true;
    }

    private void removeFromMemory(RobotData robot) {
        inSet[robot.id] = false;
    }

    public int getSize() {
        return size;
    }

    public void remove(int robotId) {
        if (inSet[robotId]) {
            inSet[robotId] = false;
            size--;
        }
    }
}
