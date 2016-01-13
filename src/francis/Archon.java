package francis;

import battlecode.common.*;
import francis.message.MessageBuilder;
import francis.message.MessageParser;
import francis.nav.Bug;
import francis.util.AverageMapLocation;
import francis.util.DirectionUtil;
import francis.util.RobotUtil;

import java.util.ArrayList;

public class Archon extends Robot {
    private int id;
    private MapLocation baseLocation;
    private boolean builtScout;
    private int teamTurretCount;

    private RobotType[] buildQueue = {RobotType.GUARD, RobotType.TURRET, RobotType.TURRET};
    private int buildCounter = 0;
    private int closeTurretCount;
    private int minTurretCount;
    private Direction towardCenterEstimate;

    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        checkRelativeLocation();
        getIdAndBaseLocation();
        if (roundNumber < 2) return;

        countRobots();
        broadcastTurretCount();
        getMinTurretCount();
        repairRobots();
        moveAwayFromEnemiesAndZombies();
        moveTowardBase();
        requestSpace();
        buildRobot();
        clearRubble();
    }

    private void checkRelativeLocation() throws GameActionException {
        if (roundNumber != 0) {
            return;
        }

        MapLocation[] locations = MapLocation.getAllMapLocationsWithinRadiusSq(currentLocation, senseRadius);
        ArrayList<MapLocation> offMap = new ArrayList<MapLocation>();
        boolean xFound = false;
        boolean yFound = false;
        int x = currentLocation.x;
        int y = currentLocation.y;
        for (MapLocation loc : locations) {
            if (loc.x != x
                    && loc.y != y) {
                continue;
            }

            if (loc.x == x
                    && xFound) {
                continue;
            }

            if (loc.y == y
                    && yFound) {
                continue;
            }

            if (!rc.onTheMap(loc)) {
                offMap.add(loc);
                if (loc.x == x) {
                    xFound = true;
                }

                if (loc.y == y) {
                    yFound = true;
                }

                if (offMap.size() == 2) {
                    towardCenterEstimate = DirectionUtil.getDirectionAwayFrom(offMap, currentLocation);
                    setIndicatorString(2, "toward center: " + towardCenterEstimate);
                    setIndicatorString(0, "count: " + offMap.size());
                    return;
                }
            }
        }

        if (offMap.size() == 0) {
            towardCenterEstimate = Direction.NORTH;
            return;
        }

        towardCenterEstimate = DirectionUtil.getDirectionAwayFrom(offMap, currentLocation);
        setIndicatorString(2, "toward center: " + towardCenterEstimate);
        setIndicatorString(0, "count: " + offMap.size());
    }

    private void moveAwayFromEnemiesAndZombies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        if (RobotUtil.anyCanAttack(nearbyZombies, currentLocation)) {
            tryMove(DirectionUtil.getDirectionAwayFrom(nearbyZombies, currentLocation));
        }
    }

    private void countRobots() throws GameActionException {
        teamTurretCount = 0;
        RobotInfo[] nearbyFriendlies = rc.senseNearbyRobots(senseRadius, team);
        for (RobotInfo r : nearbyFriendlies) {
            if (r.type == RobotType.TURRET) {
                teamTurretCount++;
            }
        }
    }

    private void broadcastTurretCount() throws GameActionException {
        RobotInfo[] closeFriends = rc.senseNearbyRobots(5, team);
        closeTurretCount = RobotUtil.getCountOfType(closeFriends, RobotType.TURRET);

        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.buildCountMessage(closeTurretCount);
        rc.broadcastMessageSignal(messageBuilder.getFirst(), messageBuilder.getSecond(), senseRadius);
    }

    private void repairRobots() throws GameActionException {
        RobotInfo[] repairableRobots = rc.senseNearbyRobots(attackRadius, team);
        RobotInfo robotToRepair = null;
        double lowestHealth = 1000000;
        for (RobotInfo r : repairableRobots) {
            if (r.type == RobotType.ARCHON) {
                continue;
            }

            if (r.health < r.type.maxHealth
                    && r.health < lowestHealth) {
                lowestHealth = r.health;
                robotToRepair = r;
            }
        }

        if (robotToRepair == null) {
            return;
        }

        rc.repair(robotToRepair.location);
    }

    private void moveTowardBase() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (currentLocation.distanceSquaredTo(baseLocation) > 2) {
            Direction direction = Bug.getDirection(currentLocation);
            rc.setIndicatorString(0, "" + direction);
            tryMove(direction);
        }
    }

    private void getIdAndBaseLocation() throws GameActionException {
        int radiusSquared = 2000;
        if (roundNumber == 0) {
            rc.broadcastSignal(radiusSquared);

            Signal[] signals = rc.emptySignalQueue();
            for (Signal s : signals) {
                if (s.getTeam() == team) {
                    id++;
                }
            }

            setIndicatorString(1, "id " + id);
        }

        if (roundNumber == 1) {
            rc.broadcastSignal(radiusSquared);

            AverageMapLocation averageMapLocation = new AverageMapLocation(GameConstants.NUMBER_OF_ARCHONS_MAX);
            averageMapLocation.add(currentLocation);
            Signal[] signals = rc.emptySignalQueue();
            for (Signal s : signals) {
                if (s.getTeam() == team) {
                    averageMapLocation.add(s.getLocation());
                }
            }

            baseLocation = averageMapLocation.getAverage();
            Bug.init(rc);
            Bug.setDestination(baseLocation);
        }
    }

    private void requestSpace() throws GameActionException {
        if (rc.senseNearbyRobots(2, team).length == 8) {
            rc.broadcastSignal(2);
        }
    }

    private void buildRobot() throws GameActionException {
        if (!builtScout
                && teamTurretCount > 5 + id) {
            if (tryBuild(RobotType.SCOUT, towardCenterEstimate)) {
                builtScout = true;
            }
        }

        RobotType typeToBuild = null;
        if (roundNumber < 500) {
            typeToBuild = buildQueue[buildCounter % buildQueue.length];
        }
        else {
            typeToBuild = RobotType.TURRET;
        }

        setIndicatorString(0, "min: " + minTurretCount);
        setIndicatorString(1, "close: " + closeTurretCount);
        setIndicatorString(2, "total: " + teamTurretCount);

        if (typeToBuild == RobotType.TURRET
                && minTurretCount < closeTurretCount) {
            return;
        }

        if (tryBuild(typeToBuild, towardCenterEstimate)) {
            buildCounter++;
        }
    }

    private void getMinTurretCount() {
        Signal[] signals = rc.emptySignalQueue();
        minTurretCount = 1010101;
        for (Signal signal : signals) {
            if (signal.getTeam() == team
                    && signal.getMessage() != null) {
                int[] message = signal.getMessage();
                MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                if (parser.getMessageType() == MessageType.COUNT) {
                    int currentCount = parser.getCount();
                    if (currentCount < minTurretCount) {
                        minTurretCount = currentCount;
                    }
                }
            }
        }
    }

    private void clearRubble() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        for (int i = 0; i < 8; i++) {
            if (rc.senseRubble(currentLocation.add(directions[i])) >= 100) {
                rc.clearRubble(directions[i]);
                return;
            }
        }
    }
}
