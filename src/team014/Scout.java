package team014;

import battlecode.common.*;
import team014.message.MessageBuilder;
import team014.message.MessageParser;
import team014.nav.SquarePath;
import team014.util.AverageMapLocation;

public class Scout extends Robot {
    private static final int INITIAL_RADIUS = 4;
    private static final int RADIUS_INCREMENT = 2;
    private final int BROADCAST_RADIUS = senseRadius * 4;

    private SquarePath path;

    private AverageMapLocation previousArchonLocations = new AverageMapLocation(6);
    private int enemiesSeen;
    private int radiusIncreased;

    private RobotInfo[] nearbyEnemies;
    private RobotInfo[] nearbyZombies;

    public Scout(RobotController rc) {
        super(rc);
        path = new SquarePath(currentLocation, INITIAL_RADIUS, rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        senseEnemiesAndZombies();
        broadcastZombies();
        broadcastParts();
        readSignals();
        explore();
        updatePath();
    }

    private void broadcastParts() throws GameActionException {
        boolean shouldCheck = roundNumber % 10 == id % 10;
        if (!shouldCheck) {
            return;
        }

        int x = currentLocation.x;
        int y = currentLocation.y;

        for (int i = -4; i <= 4; i++) {
            for (int j = -4; j <= 4; j++) {
                MapLocation location = new MapLocation(x + i, y + j);
                if (rc.onTheMap(location)) {
                    int parts = (int)rc.senseParts(location);
                    if (parts > 0) {
                        if (rc.getMessageSignalCount() >= GameConstants.MESSAGE_SIGNALS_PER_TURN) {
                            return;
                        }

                        MessageBuilder builder = new MessageBuilder();
                        builder.buildPartsMessage(location, parts);
                        rc.broadcastMessageSignal(builder.getFirst(), builder.getSecond(), senseRadius * 2);
                    }
                }
            }
        }
    }

    private void senseEnemiesAndZombies() {
        nearbyEnemies = senseNearbyEnemies();
        nearbyZombies = senseNearbyZombies();
    }

    private void updatePath() {
        MapLocation pathCenter = path.getCenter();
        MapLocation archonLocation = previousArchonLocations.getAverage();
        if (archonLocation != null
                && pathCenter.distanceSquaredTo(archonLocation) > 25) {
            path = new SquarePath(archonLocation, path.getRadius(), rc);
        }

        if (nearbyEnemies.length > 0
                || nearbyZombies.length > 0) {
            enemiesSeen = roundNumber;
            setIndicatorString(1, "saw enemy");
        }

        if (roundNumber - enemiesSeen > 200
                && roundNumber - radiusIncreased > 200) {
            path.updateRadius(path.getRadius() + RADIUS_INCREMENT);
            setIndicatorString(2, "growing path");
            radiusIncreased = roundNumber;
        }
    }

    private void explore() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        Direction circleDirection = path.getNextDirection(currentLocation);
        trySafeMove(circleDirection, nearbyEnemies, nearbyZombies);
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
            else {
                setIndicatorString(0, "received enemy signal");
            }
        }

//        setIndicatorString(0, "archon locs: " + sb.toString());
//        setIndicatorString(1, "average loc: " + previousArchonLocations.getAverage());
    }

    private void broadcastZombies() throws GameActionException {
        for (RobotInfo zombie : nearbyZombies) {
            MessageBuilder builder = new MessageBuilder();
            builder.buildZombieMessage(zombie);
            rc.broadcastMessageSignal(builder.getFirst(), builder.getSecond(), BROADCAST_RADIUS);

            if (rc.getMessageSignalCount() == GameConstants.MESSAGE_SIGNALS_PER_TURN) {
                break;
            }
        }
    }
}
