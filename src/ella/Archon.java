package ella;

import battlecode.common.*;
import ella.message.MessageBuilder;
import ella.message.MessageParser;
import ella.nav.Bug;
import ella.util.*;

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
    private boolean baseValidated;

    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        if (roundNumber == 0) {
            checkRelativeLocation();
            getIdAndBaseLocation();
            printZombieDetails();
        }

        validateBaseLocation();

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

    private void printZombieDetails() {
        ZombieSpawnSchedule schedule = rc.getZombieSpawnSchedule();
        setIndicatorString(0, "spawn rounds" + PrintUtil.toString(schedule.getRounds()));
        setIndicatorString(1, "total zombies: " + ZombieUtil.getTotalZombiesToSpawn(schedule));
        setIndicatorString(1, "# of rounds: " + ZombieUtil.getNumberOfRounds(schedule));
        setIndicatorString(1, "strength estimate: " + ZombieUtil.getStrengthEstimate(schedule));
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
                    return;
                }
            }
        }

        if (offMap.size() == 0) {
            towardCenterEstimate = Direction.NORTH;
            return;
        }

        towardCenterEstimate = DirectionUtil.getDirectionAwayFrom(offMap, currentLocation);
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
            tryDigMove(direction);
        }
    }

    private MapLocation getBaseLocation(MapLocation[] teamArchons, MapLocation[] enemyArchons) {
        MapBounds boundaryEstimate = MapUtil.getBoundsThatEncloseLocations(teamArchons, enemyArchons);
        return MapUtil.getClosestToBoundary(teamArchons, boundaryEstimate);
    }

    private void getIdAndBaseLocation() throws GameActionException {
        MapLocation[] teamArchonLocations = rc.getInitialArchonLocations(team);
        MapLocation[] enemyArchonLocations = rc.getInitialArchonLocations(enemy);

        setId(teamArchonLocations);
        baseLocation = getBaseLocation(teamArchonLocations, enemyArchonLocations);

        Bug.init(rc);
        Bug.setDestination(baseLocation);
    }

    private void validateBaseLocation() throws GameActionException {
        if (baseValidated
                || !canSenseSurroundings(baseLocation, Config.REQUIRED_RADIUS_AROUND_BASE)) {
            return;
        }

        if (closeToAnEdge(baseLocation, Config.REQUIRED_RADIUS_AROUND_BASE)) {
            baseLocation = moveAwayFromEdges(baseLocation, Config.REQUIRED_RADIUS_AROUND_BASE);
        }

        Bug.setDestination(baseLocation);
        baseValidated = true;
    }

    private MapLocation moveAwayFromEdges(MapLocation location, int radius) throws GameActionException {
        MapLocation newLocation = new MapLocation(location.x, location.y);
        for (int i = 0; i < 8; i += 2) {
            if (!rc.onTheMap(location.add(directions[i], radius))) {
                newLocation = newLocation.add(directions[i].opposite(), radius);
            }
        }

        return newLocation;
    }

    private boolean canSenseSurroundings(MapLocation location, int radius) {
        for (int i = 0; i < 8; i += 2) {
            if (!rc.canSense(location.add(directions[i], radius))) {
                return false;
            }
        }

        return true;
    }

    private boolean closeToAnEdge(MapLocation location, int radius) throws GameActionException {
        for (int i = 0; i < 8; i += 2) {
            if (!rc.onTheMap(location.add(directions[i], radius))) {
                return true;
            }
        }

        return false;
    }

    private void setId(MapLocation[] teamArchonLocations) {
        //--All robots will have the same ordering of map locations
        //  so they will each have a unique ID
        id = 0;
        for (int i = 0; i < teamArchonLocations.length; i++) {
            if (currentLocation.equals(teamArchonLocations[i])) {
                id = i;
            }
        }
    }

    private void requestSpace() throws GameActionException {
        if (needSpace()) {
            rc.broadcastSignal(2);
        }
    }

    private boolean needSpace() throws GameActionException {
        RobotInfo[] adjacentTeammates = rc.senseNearbyRobots(2, team);
        int adjacentOnMapSquares = getAdjacentOnMapSquares(currentLocation);
        return adjacentTeammates.length == adjacentOnMapSquares;
    }

    private int getAdjacentOnMapSquares(MapLocation currentLocation) throws GameActionException {
        int edges = 0;
        for (int i = 0; i < 8; i += 2) {
            MapLocation adjacent = currentLocation.add(directions[i]);
            if (!rc.onTheMap(adjacent)) {
                edges++;
                if (edges > 1) {
                    break;
                }
            }
        }

        if (edges == 2) {
            return 3;
        }
        else if (edges == 1) {
            return 5;
        }

        return 8;
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
