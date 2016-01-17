package jeremy;

import battlecode.common.*;
import jeremy.message.MessageParser;
import jeremy.util.AverageMapLocation;
import jeremy.util.DirectionUtil;
import jeremy.util.LocationUtil;

public class Archon extends Robot {

    private RobotType[] buildQueue = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
        RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
        RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER };

    private int buildQueuePosition = 0;
    private RobotInfo[] nearbyZombies;
    private AverageMapLocation previousZombieLocation = new AverageMapLocation(5);
    private Signal[] roundSignals;
    private MapLocation previousPartLocation;

    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        senseZombies();
        moveAwayFromZombies();
        buildRobots();
        getParts();
        moveIfSafe();
        repairRobots();
    }

    private void senseZombies() {
        nearbyZombies = senseNearbyZombies();
        for (RobotInfo zombie : nearbyZombies) {
            previousZombieLocation.add(zombie.location);
        }
    }

    private void moveIfSafe() throws GameActionException {
        if (nearbyZombies.length > 0
                || !rc.isCoreReady()) {
            return;
        }

        MapLocation towardZombies = previousZombieLocation.getAverage();
        if (towardZombies == null
                || towardZombies.equals(currentLocation)) {
            tryMove(getRandomDirection());
        }
        else {
            setIndicatorString(0, "move toward zombies: " + towardZombies);
            tryMoveToward(towardZombies);
        }
    }

    private void buildRobots() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (tryBuild(buildQueue[buildQueuePosition % buildQueue.length])) {
            buildQueuePosition++;
        }
    }

    private void moveAwayFromZombies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (nearbyZombies.length > 0) {
            tryMove(DirectionUtil.getDirectionAwayFrom(nearbyZombies, currentLocation));
        }
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

    public void getParts() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (previousPartLocation != null
                && currentLocation.equals(previousPartLocation)) {
            previousPartLocation = null;
        }

        MapLocation[] partsLocations = rc.sensePartLocations(senseRadius);
        if (partsLocations.length > 0) {
            setIndicatorString(0, "i sense parts");
            MapLocation closest = LocationUtil.findClosestLocation(partsLocations, currentLocation);
            tryMoveOnto(closest);
            return;
        }

        if (previousPartLocation == null) {
            for (Signal s : roundSignals) {
                if (s.getTeam() == team) {
                    int[] message = s.getMessage();
                    if (message == null) continue;
                    MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                    if (parser.getMessageType() == MessageType.PARTS) {
                        previousPartLocation = parser.getPartsData().location;
                        setIndicatorString(2, "received part signal: " + previousPartLocation);
                    }
                }
            }
        }

        if (previousPartLocation != null) {
            setIndicatorString(1, "moving toward " + previousPartLocation);
            tryMoveOnto(previousPartLocation);
        }
    }
}
