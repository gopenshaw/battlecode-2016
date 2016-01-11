package bernie;

import battlecode.common.*;
import bernie.message.MessageBuilder;
import bernie.message.MessageParser;
import bernie.nav.SquarePath;
import bernie.util.AverageMapLocation;

public class Scout extends Robot {
    private static final int INITIAL_RADIUS = 4;
    private static final int RADIUS_INCREMENT = 1;
    private final int BROADCAST_RADIUS = senseRadius * 4;

    private SquarePath squarePath;
    private int previousRotations;

    private AverageMapLocation previousArchonLocations = new AverageMapLocation(6);

    public Scout(RobotController rc) {
        super(rc);
        squarePath = new SquarePath(currentLocation, INITIAL_RADIUS, rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        broadcastZombies();
        readSignals();
        explore();
    }

    private void explore() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        MapLocation pathCenter = squarePath.getCenter();
        if (previousArchonLocations.getAverage() != null
                && pathCenter.distanceSquaredTo(previousArchonLocations.getAverage()) > 25) {
            squarePath = new SquarePath(previousArchonLocations.getAverage(), INITIAL_RADIUS, rc);
        }

        Direction circleDirection = squarePath.getNextDirection(currentLocation);
        tryMove(circleDirection);
    }

    private void readSignals() {
        Signal[] signals = rc.emptySignalQueue();
        StringBuilder sb = new StringBuilder();
        for (Signal s : signals) {
            if (s.getTeam() == team) {
                int[] message = s.getMessage();
                if (message == null) continue;
                MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                if (parser.getMessageType() == MessageType.ID) {
                    RobotData robotData = parser.getRobotData();
                    if (robotData.type == RobotType.ARCHON) {
                        previousArchonLocations.add(robotData.location);
                        sb.append(robotData.location + "; ");
                    }
                }
            }
        }

        setIndicatorString(0, "archon locs: " + sb.toString());
        setIndicatorString(1, "average loc: " + previousArchonLocations.getAverage());
    }

    private void broadcastZombies() throws GameActionException {
        RobotInfo[] zombies = senseNearbyZombies();
        for (RobotInfo zombie : zombies) {
            MessageBuilder builder = new MessageBuilder();
            builder.buildZombieMessage(zombie);
            rc.broadcastMessageSignal(builder.getFirst(), builder.getSecond(), BROADCAST_RADIUS);

            if (rc.getMessageSignalCount() == GameConstants.MESSAGE_SIGNALS_PER_TURN) {
                break;
            }
        }
    }
}
