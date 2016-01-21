package nels.util;

import nels.RobotData;

public class RobotQueueNoDuplicates {
    private BoundedQueue<RobotData> robots;
    private boolean[] idInSet = new boolean[32001];

    public RobotQueueNoDuplicates(int capacity) {
        robots = new BoundedQueue<RobotData>(capacity);
    }

    public void add(RobotData robot) {
        if (idInSet[robot.id]) {
            return;
        }

        idInSet[robot.id] = true;
        robots.add(robot);
    }

    public RobotData remove() {
        if (robots.isEmpty()) {
            return null;
        }

        RobotData robot = robots.remove();
        idInSet[robot.id] = false;
        return robot;
    }

    public RobotData peek() {
        if (robots.isEmpty()) {
            return null;
        }

        return robots.peek();
    }

    public boolean isEmpty() {
        return robots.isEmpty();
    }

    public int getSize() {
        return robots.getSize();
    }
}
