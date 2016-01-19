package team014;

import battlecode.common.*;
import team014.message.MessageParser;
import team014.nav.Bug;
import team014.util.AverageMapLocation;
import team014.util.DirectionUtil;
import team014.util.LocationUtil;
import team014.util.RobotUtil;

public class Archon extends Robot {

    private RobotType[] lowUnitCountBuildQueue = {
            RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
    };

    private RobotType[] highUnitCountBuildQueue = {
            RobotType.SCOUT, RobotType.TURRET,
            RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER
    };

    private int lowUnitQueuePosition = 0;
    private int highUnitQueuePosition = 0;

    private RobotInfo[] nearbyZombies;
    private AverageMapLocation previousZombieLocation = new AverageMapLocation(5);
    private Signal[] roundSignals;
    private MapLocation previousPartLocation;
    private RobotInfo[] nearbyEnemies;
    private double lastRoundHealth;
    private RobotInfo[] nearbyFriendlies;

    public Archon(RobotController rc) {
        super(rc);
        Bug.init(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        senseRobots();
        moveAwayFromZombiesAndEnemies();
        moveIfUnderAttack();
        buildRobots();
        convertAdjacentNeutrals();
        getParts();
        moveWithArmy();
        repairRobots();
        lastRoundHealth = rc.getHealth();
    }

    private void moveWithArmy() throws GameActionException {
        if (nearbyEnemies.length > 0
                || nearbyZombies.length > 0
                || nearbyFriendlies.length < 2
                || !rc.isCoreReady()) {
            return;
        }

        MapLocation armyCenter = RobotUtil.findAverageLocation(nearbyFriendlies);
        setIndicatorString(2, "army center is " + armyCenter);
        if (armyCenter.distanceSquaredTo(currentLocation) > 8) {
            setIndicatorString(2, "moving toward center");
            tryMove(DirectionUtil.getDirectionToward(nearbyFriendlies, currentLocation));
        }
    }

    private void moveIfUnderAttack() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (rc.getHealth() < lastRoundHealth) {
            tryMove(DirectionUtil.getDirectionToward(nearbyFriendlies, currentLocation));
        }
    }

    private void convertAdjacentNeutrals() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] adjacentNeutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
        if (adjacentNeutrals.length > 0) {
            rc.activate(adjacentNeutrals[0].location);
        }
    }

    private void moveAwayFromZombiesAndEnemies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (zombiesAreDangerous()
                || RobotUtil.anyCanAttack(nearbyEnemies, currentLocation)) {
            rc.broadcastSignal(32);
            Direction runDirection = DirectionUtil.getDirectionAwayFrom(nearbyEnemies, nearbyZombies, currentLocation);

            //--TODO check if we are going into a corner or some other trap
            tryMove(runDirection);
        }
    }

    private boolean zombiesAreDangerous() {
        if (nearbyZombies.length == 0) {
            return false;
        }

        return nearbyZombies.length > nearbyFriendlies.length
                || RobotUtil.canAttackInOneMove(nearbyZombies, currentLocation);
    }

    private void senseRobots() {
        nearbyZombies = senseNearbyZombies();
        nearbyEnemies = senseNearbyEnemies();
        nearbyFriendlies = senseNearbyFriendlies();
        setIndicatorString(0, "nearby enemies: " + nearbyEnemies.length);
        setIndicatorString(0, "nearby zom: " + nearbyZombies.length);
        for (RobotInfo zombie : nearbyZombies) {
            previousZombieLocation.add(zombie.location);
        }
    }

    private void buildRobots() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (rc.getRobotCount() > Config.HIGH_UNIT_COUNT) {
            if (tryBuild(highUnitCountBuildQueue[highUnitQueuePosition % highUnitCountBuildQueue.length])) {
                highUnitQueuePosition++;
            }
        }
        else {
            if (tryBuild(lowUnitCountBuildQueue[lowUnitQueuePosition % lowUnitCountBuildQueue.length])) {
                lowUnitQueuePosition++;
            }
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
            MapLocation closest = LocationUtil.findClosestLocation(partsLocations, currentLocation);
            setIndicatorString(2, "moving toward closest parts");
            tryMoveOnto(closest);
            rc.broadcastSignal(31);
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
                    }
                }
            }
        }

        if (previousPartLocation != null) {
            setIndicatorString(2, "moving toward memory parts");
            tryMoveOnto(previousPartLocation);
            rc.broadcastSignal(31);
        }
    }
}
