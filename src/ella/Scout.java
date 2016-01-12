package ella;

import battlecode.common.*;
import ella.message.MessageBuilder;

public class Scout extends Robot {
    private MapLocation requestLocation;
    private Signal[] roundSignals;

    public Scout(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        broadcastZombies();
        broadcastEnemies();
        spreadOut();
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

    private void spreadOut() throws GameActionException {
        if (requestLocation == null) {
            for (Signal s : roundSignals) {
                if (s.getTeam() == team
                        && s.getMessage() == null) {
                    requestLocation = s.getLocation();
                }
            }
        }

        if (requestLocation == null
                || !rc.isCoreReady()) {
            return;
        }

        tryMove(requestLocation.directionTo(currentLocation));
        requestLocation = null;
    }
}
