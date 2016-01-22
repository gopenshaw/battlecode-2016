package vicky.util;

import battlecode.common.RobotType;

public class ZombieUtil {
    private static RobotType[] zombiePriority = {
            RobotType.STANDARDZOMBIE,
            RobotType.FASTZOMBIE,
            RobotType.BIGZOMBIE,
            RobotType.RANGEDZOMBIE,
    };

    public static int getAttackPriority(RobotType zombieType) {
        for (int i = 0 ; i < zombiePriority.length; i++) {
            if (zombiePriority[i] == zombieType)  {
                return i;
            }
        }

        return -1;
    }
}
