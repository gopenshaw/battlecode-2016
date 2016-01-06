package alpha;

import battlecode.common.*;

public class Archon extends Robot {
    private final int SENSE_WIDTH =
            (int) (Math.floor(Math.sqrt(RobotType.ARCHON.sensorRadiusSquared)) * 2 + 1);

    private MapLocation[][] surroundings = new MapLocation[SENSE_WIDTH][SENSE_WIDTH];
    private boolean[][] locationValid = new boolean[SENSE_WIDTH][SENSE_WIDTH];

    public Archon(RobotController rc) {
        super(rc);
    }

    private RobotType[] buildQueue = {RobotType.SCOUT, RobotType.GUARD, RobotType.VIPER,
            RobotType.GUARD, RobotType.GUARD, RobotType.GUARD};

    private int queuePosition = 0;
    private MapLocation base = null;

    private final int SQR_DIST_TO_BASE = 20;

    @Override
    public void doTurn(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() == 0) {
            rc.broadcastSignal(2000);
            return;
        }

        if (rc.getRoundNum() == 1) {
            Signal[] otherArchons = rc.emptySignalQueue();
            base = findAverageMapLocation(rc.getLocation(), otherArchons);
        }

        if (!rc.isCoreReady()) {
            scanSurroundings(rc);
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        RobotInfo[] nearbyEnemies = senseNearbyEnemies();
        if (nearbyZombies.length > 0
                || nearbyEnemies.length > 0) {
            Direction away = DirectionUtil.getDirectionAwayFrom(nearbyEnemies, nearbyZombies, rc);
            tryMove(away);
            return;
        }

        MapLocation currentLocation = rc.getLocation();
        if (base != null
                && currentLocation.distanceSquaredTo(base) > SQR_DIST_TO_BASE) {
            tryMoveToward(base);
            return;
        }

        MapLocation mostParts = findMostParts(rc);
        if (mostParts != null) {
            tryMoveToward(mostParts);
            return;
        }

        if (queuePosition < buildQueue.length) {
            if (tryBuild(buildQueue[queuePosition])) {
                queuePosition++;
            }
        }
    }

    private MapLocation findAverageMapLocation(MapLocation location, Signal[] locations) {
        int x = location.x;
        int y = location.y;
        int count = 1;
        for (Signal s : locations) {
            if (s.getTeam() != team) {
                continue;
            }

            MapLocation loc = s.getLocation();
            x += loc.x;
            y += loc.y;
            count++;
        }

        return new MapLocation(x / count, y / count);
    }

    private MapLocation findMostParts(RobotController rc) {
        double max = 0;
        MapLocation maxLocation = null;
        int width = surroundings.length;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                if (locationValid[i][j]) {
                    double parts = rc.senseParts(surroundings[i][j]);
                    if (parts > max) {
                        maxLocation = surroundings[i][j];
                        max = parts;
                    }
                }
            }
        }

        return maxLocation;
    }

    private void scanSurroundings(RobotController rc) throws GameActionException {
        MapLocation center = rc.getLocation();
        int x = center.x;
        int y = center.y;
        int width = surroundings.length;
        int offset = - (width / 2);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                MapLocation location = new MapLocation(x + i + offset, y + j + offset);
                if (rc.canSenseLocation(location)
                        && rc.onTheMap(location)) {
                    locationValid[i][j] = true;
                    surroundings[i][j] = location;
                }
                else {
                    locationValid[i][j] = false;
                }
            }
        }
    }
}
