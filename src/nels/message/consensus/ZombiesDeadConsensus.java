package nels.message.consensus;

import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import nels.message.Subject;

public class ZombiesDeadConsensus extends ConsensusManager {
    private int[] zombieMemory = new int[GameConstants.GAME_DEFAULT_ROUNDS + 1];
    private final int ZOMBIE_EXISTS_ROUNDS = 80;

    public ZombiesDeadConsensus(RobotController rc) {
        super(rc);
    }

    @Override
    protected int getRetryDelay() {
        return 100;
    }

    @Override
    protected Subject getSubject() {
        return Subject.ZOMBIES_DEAD;
    }

    @Override
    protected int getMinimumAgeToPropose() {
        return 250;
    }

    @Override
    protected int getMinimumAgeToDeny() {
        return 5;
    }

    @Override
    protected boolean shouldPropose(int currentRound) {
        int beginRound = currentRound - ZOMBIE_EXISTS_ROUNDS;
        if (beginRound < 0) beginRound = 0;
        for (int i = beginRound; i <= currentRound; i++) {
            if (zombieMemory[i] > 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected boolean shouldDeny(int currentRound) {
        int beginRound = currentRound - ZOMBIE_EXISTS_ROUNDS;
        if (beginRound < 0) beginRound = 0;
        for (int i = beginRound; i <= currentRound; i++) {
            if (zombieMemory[i] > 0) {
                return true;
            }
        }

        return false;
    }

    public void updateZombieCount(int count, int currentRound) {
        zombieMemory[currentRound] = count;
    }
}
