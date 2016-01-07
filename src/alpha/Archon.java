package alpha;

import battlecode.common.*;

import java.util.*;

public class Archon extends Robot {
    private final int SENSE_WIDTH =
            (int) (Math.floor(Math.sqrt(RobotType.ARCHON.sensorRadiusSquared)) * 2 + 1);

    private MapLocation[][] surroundings = new MapLocation[SENSE_WIDTH][SENSE_WIDTH];
    private boolean[][] locationValid = new boolean[SENSE_WIDTH][SENSE_WIDTH];

    public Archon(RobotController rc) {
        super(rc);
    }

    private int queuePosition = 0;
    private RobotType[] buildQueue = {RobotType.GUARD, RobotType.GUARD,
            RobotType.TURRET, RobotType.TURRET, RobotType.TURRET};

    private MapLocation base = null;
    private RobotData neutralRobot = null;
    private int archonId = 1; //--unique number in [1,6]
    private boolean scoutBuilt = false;
    private Map<Integer, RobotData> neutralRobots = new HashMap<Integer, RobotData>();

    private final int SQR_DIST_TO_BASE = 20;

    @Override
    public void doTurn(RobotController rc) throws GameActionException {
        if (doEarlyGameActions(rc)) return;

        readSignals(rc);

        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        RobotInfo[] nearbyEnemies = senseNearbyEnemies();
        if (Util.anyCanAttack(nearbyEnemies)
                || Util.anyCanAttack(nearbyZombies)) {
            Direction away = DirectionUtil.getDirectionAwayFrom(nearbyEnemies, nearbyZombies, rc);
            tryMove(away);
            return;
        }

        MapLocation currentLocation = rc.getLocation();
        if (base != null
                && archonId != 2
                && currentLocation.distanceSquaredTo(base) > SQR_DIST_TO_BASE) {
            tryMoveToward(base);
            return;
        }

        if (archonId == 2) {
            if (tryToActivateNeutrals(rc, currentLocation)) return;
            if (!rc.isCoreReady()) return;
        }

        if (queuePosition < buildQueue.length) {
            if (tryBuild(buildQueue[queuePosition])) {
                queuePosition++;
            }
        }
        else {
            tryBuild(RobotType.SOLDIER);
        }
    }

    private boolean tryToActivateNeutrals(RobotController rc, MapLocation currentLocation) throws GameActionException {
        if (neutralRobot != null) {
            if (currentLocation.isAdjacentTo(neutralRobot.location)) {
                rc.activate(neutralRobot.location);
                setIndicatorString(0, "activating " + neutralRobot);
                neutralRobots.remove(neutralRobot.robotId);
                neutralRobot = null;
                return true;
            }
            else {
                tryMoveToward(neutralRobot.location);
            }
        }
        else if (!neutralRobots.isEmpty()) {
            neutralRobot = findClosestNeutralRobot(neutralRobots.values(), currentLocation);
            tryMoveToward(neutralRobot.location);
            return true;
        }

        return false;
    }

    private RobotData findClosestNeutralRobot(Collection<RobotData> values, MapLocation currentLocation) {
        RobotData closestRobot = null;
        int shortestDistance = Integer.MAX_VALUE;
        for (RobotData robotData : values) {
            int distance = robotData.location.distanceSquaredTo(currentLocation);
            if (distance < shortestDistance) {
                closestRobot = robotData;
                shortestDistance = distance;
            }
        }

        return closestRobot;
    }

    private void readSignals(RobotController rc) {
        Signal[] signals = rc.emptySignalQueue();
        for (Signal s : signals) {
            if (s.getTeam() == enemy
                    || s.getMessage() == null) {
                continue;
            }

            RobotData robotData = SignalUtil.readSignal(s, rc.getLocation());
            if (robotData.team == Team.NEUTRAL) {
                if (!neutralRobots.containsKey(robotData.robotId)) {
                    setIndicatorString(0, "adding neutral robot to dictionary " + robotData.location);
                    neutralRobots.put(robotData.robotId, robotData);
                }
            }
        }
    }

    private boolean doEarlyGameActions(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() == 0) {
            rc.broadcastSignal(SignalUtil.ENTIRE_MAP_RADIUS);
            return true;
        }

        if (rc.getRoundNum() == 1) {
            Signal[] otherArchons = rc.emptySignalQueue();
            setArchonId(otherArchons, rc);
            setIndicatorString(1, "my id is " + archonId);

            base = findAverageMapLocation(rc.getLocation(), otherArchons);
        }

        if (archonId == 1 && !scoutBuilt) {
            if (tryBuild(RobotType.SCOUT)) {
                scoutBuilt = true;
                return true;
            }
        }
        return false;
    }

    private void setArchonId(Signal[] otherArchons, RobotController rc) {
        int myId = rc.getID();
        for (Signal s : otherArchons) {
            if (s.getTeam() == enemy) {
                continue;
            }

            if (s.getID() < myId) {
                archonId++;
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
