package francis;

import battlecode.common.*;
import francis.message.MessageBuilder;

public class Scout extends Robot {
    public Scout(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        broadcastZombies();
        broadcastEnemies();
    }

    private void broadcastZombies() throws GameActionException {
        RobotInfo[] nearbyZombies = senseNearbyZombies();
        for (RobotInfo zombie : nearbyZombies) {
            if (rc.getMessageSignalCount() >= GameConstants.MESSAGE_SIGNALS_PER_TURN) {
                return;
            }

            MessageBuilder builder = new MessageBuilder();
            builder.buildZombieMessage(zombie, roundNumber);
            rc.broadcastMessageSignal(builder.getFirst(), builder.getSecond(), senseRadius * 4);
        }
    }

    private void broadcastEnemies() throws GameActionException {
        RobotInfo[] nearbyEnemies = senseNearbyEnemies();
        for (RobotInfo zombie : nearbyEnemies) {
            if (rc.getMessageSignalCount() >= GameConstants.MESSAGE_SIGNALS_PER_TURN) {
                return;
            }

            MessageBuilder builder = new MessageBuilder();
            builder.buildEnemyMessage(zombie, roundNumber);
            rc.broadcastMessageSignal(builder.getFirst(), builder.getSecond(), senseRadius * 4);
        }
    }
}
