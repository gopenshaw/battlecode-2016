package ella.util;

import battlecode.common.RobotType;
import battlecode.common.ZombieCount;
import battlecode.common.ZombieSpawnSchedule;

public class ZombieUtil {
    private static final double[] ZOMBIE_MULTIPLIER = {1, 1.1, 1.2, 1.3, 1.5, 1.7, 2, 2.3, 2.6, 3};

    private static RobotType[] zombiePriority = {
            RobotType.STANDARDZOMBIE,
            RobotType.FASTZOMBIE,
            RobotType.BIGZOMBIE,
            RobotType.RANGEDZOMBIE,
    };

    public static int[] getSpawnRounds(ZombieSpawnSchedule schedule) {
        return schedule.getRounds();
    }

    public static int getAttackPriority(RobotType zombieType) {
        for (int i = 0 ; i < zombiePriority.length; i++) {
            if (zombiePriority[i] == zombieType)  {
                return i;
            }
        }

        return -1;
    }

    public static String formatZombieSchedule(ZombieSpawnSchedule schedule) {
        StringBuilder sb = new StringBuilder();
        int[] rounds = schedule.getRounds();
        for (int i = 0; i < rounds.length; i++) {
            sb.append(String.format("Round %d: ", rounds[i]));
            ZombieCount[] counts = schedule.getScheduleForRound(rounds[i]);
            for (int j = 0; j < counts.length; j++) {
                sb.append(String.format("%d %s: ", counts[j].getCount(), counts[j].getType()));
            }
        }

        return sb.toString();
    }

    public static int getTotalZombiesToSpawn(ZombieSpawnSchedule schedule) {
        int count = 0;
        int[] rounds = schedule.getRounds();
        for (int i = 0; i < rounds.length; i++) {
            ZombieCount[] counts = schedule.getScheduleForRound(rounds[i]);
            for (int j = 0; j < counts.length; j++) {
                count += counts[j].getCount();
            }
        }

        return count;
    }

    public static int getNumberOfRounds(ZombieSpawnSchedule schedule) {
        return schedule.getRounds().length;
    }

    public static int getStrengthEstimate(ZombieSpawnSchedule schedule) {
        double strength = 0;
        int[] spawnRounds = schedule.getRounds();
        for (int spawn = 0; spawn < spawnRounds.length; spawn++) {
            ZombieCount[] counts = schedule.getScheduleForRound(spawnRounds[spawn]);
            for (int i = 0; i < counts.length; i++) {
                strength += counts[i].getCount() * getMultiplier(spawn);
            }
        }

        return (int)strength;
    }

    private static double getMultiplier(int spawn) {
        if (spawn < ZOMBIE_MULTIPLIER.length) {
            return ZOMBIE_MULTIPLIER[spawn];
        }
        else {
            return spawn - 6;
        }
    }
}
