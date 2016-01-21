package nels;

import battlecode.common.*;
import nels.message.MessageParser;
import nels.message.consensus.ZombiesDeadConsensus;
import nels.nav.Bug;
import nels.util.*;

public class Archon extends Robot {

    private static final int SCOUT_ALIVE_ROUNDS = 200;
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
    private final ZombiesDeadConsensus zombiesDead;
    private int[] scoutAliveRound = new int[32001];
    private RobotQueueNoDuplicates aliveScouts = new RobotQueueNoDuplicates(30);
    private int scoutCountEstimate;

    public Archon(RobotController rc) {
        super(rc);
        Bug.init(rc);
        zombiesDead = new ZombiesDeadConsensus(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        getTurretBroadcasts(roundSignals);
        senseRobots();
        estimateScoutCount();
        
        zombiesDead.updateZombieCount(nearbyZombies.length, roundNumber);
        zombiesDead.participate(roundSignals, roundNumber);

        moveAwayFromZombiesAndEnemies();
        moveIfUnderAttack();
        buildRobots();
        convertAdjacentNeutrals();
        getParts();
        moveWithArmy();
        repairRobots();
        lastRoundHealth = rc.getHealth();
    }

    private void estimateScoutCount() {
        RobotInfo[] nearbyScouts = RobotUtil.getRobotsOfType(nearbyFriendlies, RobotType.SCOUT);
        if (nearbyScouts == null) {
            return;
        }

        for (int i = 0; i < nearbyScouts.length; i++) {
            RobotInfo scout = nearbyScouts[i];
            scoutAliveRound[scout.ID] = roundNumber;
            aliveScouts.add(new RobotData(scout.ID, null, (int)scout.health, scout.type));
        }

        if (!aliveScouts.isEmpty()) {
            RobotData oldScout = aliveScouts.peek();
            if (scoutAliveRound[oldScout.id] + SCOUT_ALIVE_ROUNDS > roundNumber) {
                aliveScouts.add(oldScout);
            }
            else {
                aliveScouts.remove();
            }
        }

        scoutCountEstimate = aliveScouts.getSize();
        setIndicatorString(2, "scout alive estimate: " + scoutCountEstimate);
    }

    private void moveWithArmy() throws GameActionException {
        if (nearbyEnemies.length > 0
                || nearbyZombies.length > 0
                || nearbyFriendlies.length < 2
                || !rc.isCoreReady()) {
            return;
        }

        RobotInfo[] nonArchonUnits = RobotUtil.removeRobotsOfType(nearbyFriendlies, RobotType.ARCHON);
        if (nonArchonUnits.length == 0) {
            return;
        }

        MapLocation armyCenter = RobotUtil.findAverageLocation(nonArchonUnits);
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

        if (rc.getRobotCount() > Config.HIGH_UNIT_COUNT
                || (roundNumber > 600 && rc.getTeamParts() > 350)) {
            RobotType robotType = highUnitCountBuildQueue[highUnitQueuePosition % highUnitCountBuildQueue.length];
            if (robotType == RobotType.SCOUT
                    && scoutCountEstimate > 8) {
                highUnitQueuePosition++;
                robotType = highUnitCountBuildQueue[highUnitQueuePosition % highUnitCountBuildQueue.length];
            }

            if (tryBuild(robotType)) {
                highUnitQueuePosition++;
            }
        }
        else {
            RobotType robotType = lowUnitCountBuildQueue[lowUnitQueuePosition % lowUnitCountBuildQueue.length];
            if (robotType == RobotType.SCOUT
                    && scoutCountEstimate > 8) {
                lowUnitQueuePosition++;
                robotType = lowUnitCountBuildQueue[lowUnitQueuePosition % lowUnitCountBuildQueue.length];
            }
            if (tryBuild(robotType)) {
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
            trySafeMoveToward(closest, enemyTurrets);
            rc.broadcastSignal(31);
            return;
        }

        if (previousPartLocation == null) {
            for (Signal s : roundSignals) {
                if (s.getTeam() == team) {
                    int[] message = s.getMessage();
                    if (message == null) continue;
                    if (MessageParser.matchesType(message, MessageType.PARTS)) {
                        previousPartLocation = MessageParser.getPartsData(message[0], message[1]).location;
                    }
                }
            }
        }

        if (previousPartLocation != null) {
            setIndicatorString(2, "moving toward memory parts");
            trySafeMoveToward(previousPartLocation, enemyTurrets);
            rc.broadcastSignal(31);
        }
    }
}
