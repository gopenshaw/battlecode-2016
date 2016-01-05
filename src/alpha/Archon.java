package alpha;

import battlecode.common.*;

public class Archon extends Robot {
    private final int SENSE_WIDTH =
            (int) (Math.floor(Math.sqrt(RobotType.ARCHON.sensorRadiusSquared)) * 2 + 1);

    private MapLocation[][] surroundings = new MapLocation[SENSE_WIDTH][SENSE_WIDTH];
    private boolean[][] locationValid = new boolean[SENSE_WIDTH][SENSE_WIDTH];
    private boolean scoutBuilt = false;

    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    public void doTurn(RobotController rc) throws GameActionException {
        if (!rc.isCoreReady()) {
            scanSurroundings(rc);
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        if (nearbyZombies.length > 0) {
            Direction away = DirectionUtil.getDirectionAwayFrom(nearbyZombies, rc);
            tryMove(away);
            return;
        }

        MapLocation mostParts = findMostParts(rc);
        if (mostParts != null) {
            tryMoveToward(mostParts);
            return;
        }

        if (!scoutBuilt) {
            if (rc.getTeamParts() > RobotType.SCOUT.partCost) {
                scoutBuilt = tryBuild(RobotType.SCOUT);
            }
        }
        else if (rc.getTeamParts() > RobotType.GUARD.partCost) {
            tryBuild(RobotType.GUARD);
        }
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
