package francis.util;

import battlecode.common.RobotInfo;
import francis.RobotData;

public class EnemyStatus {
    public static final int MAX_ROBOT_ID = 32000;

    private int[] ignoreEnemyUntilRound = new int[MAX_ROBOT_ID];

    public void ignoreRobot(RobotInfo robot, int currentRound, int numberOfRounds) {
        ignoreEnemyUntilRound[robot.ID] = currentRound + numberOfRounds;
    }

    public boolean ignoring(RobotData robot, int currentRound) {
        return ignoreEnemyUntilRound[robot.id] >= currentRound;
    }
}
