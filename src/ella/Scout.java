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
        spreadOut();
    }

    private void broadcastZombies() throws GameActionException {
        RobotInfo[] nearbyZombies = senseNearbyZombies();
        for (RobotInfo zombie : nearbyZombies) {
            MessageBuilder builder = new MessageBuilder();
            builder.buildZombieMessage(zombie, roundNumber);
            rc.broadcastMessageSignal(builder.getFirst(), builder.getSecond(), senseRadius * 4);
            if (rc.getMessageSignalCount() >= GameConstants.MESSAGE_SIGNALS_PER_TURN) {
                return;
            }
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
