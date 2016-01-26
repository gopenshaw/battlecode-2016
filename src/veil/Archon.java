package veil;

import battlecode.common.*;
import veil.message.*;
import veil.message.consensus.ZombiesDeadConsensus;
import veil.nav.Bug;
import veil.util.*;

public class Archon extends Robot {

    private static final int SCOUT_ALIVE_ROUNDS = 200;
    private RobotType[] lowUnitCountBuildQueue = {
            RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.SOLDIER, RobotType.VIPER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER
    };

    private RobotType[] highUnitCountBuildQueue = {
            RobotType.SCOUT, RobotType.TURRET,
            RobotType.SOLDIER, RobotType.SOLDIER,
    };

    private int lowUnitQueuePosition = 0;
    private int highUnitQueuePosition = 0;

    private RobotInfo[] nearbyZombies;
    private RobotInfo[] nearbyEnemies;
    private RobotInfo[] nearbyFriendlies;
    private RobotInfo[] nearbyNeutrals;

    private AverageMapLocation previousZombieLocation = new AverageMapLocation(5);
    private Signal[] roundSignals;
    private MapLocation previousPartLocation;
    private double lastRoundHealth;
    private final ZombiesDeadConsensus zombiesDead;
    private int[] scoutAliveRound = new int[32001];
    private RobotQueueNoDuplicates aliveScouts = new RobotQueueNoDuplicates(60);
    private int scoutCountEstimate;
    private RobotData enemyToApproach;

    public Archon(RobotController rc) {
        super(rc);
        Bug.init(rc);
        zombiesDead = new ZombiesDeadConsensus(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        processAllBroadcasts();
        broadcastEnemyToApproach();
        broadcastZombiesDead();
        senseRobots();
        estimateScoutCount();

        zombiesDead.updateZombieCount(nearbyZombies.length, roundNumber);
        zombiesDead.participate(roundSignals, roundNumber);

        moveAwayFromZombiesAndEnemies();
        moveIfUnderAttack();
        moveIfNearEnemyTurrets();
        moveWithArmy();
        buildRobots();
        convertAdjacentNeutrals();
        getParts();
        repairRobots();
        goTowardNeutral();
        requestHelpIfUnderAttack();

        lastRoundHealth = rc.getHealth();
    }

    private void broadcastZombiesDead() throws GameActionException {
        if (enemyToApproach == null
                || roundNumber % 10 != 8) {
            return;
        }

        //--broadcast so the information goes to robots we are spawning
        setIndicatorString(1, "broadcast zombies dead");
        Message message = MessageBuilder.buildAnnouncement(Subject.ZOMBIES_DEAD, AnnouncementMode.AFFIRM);
        rc.broadcastMessageSignal(message.getFirst(), message.getSecond(), 2);
    }

    private void goTowardNeutral() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (nearbyNeutrals.length > 0) {
            RobotInfo closest = RobotUtil.getClosestRobotToLocation(nearbyNeutrals, currentLocation);
            trySafeMoveToward(closest.location, nearbyEnemies, nearbyZombies);
        }
    }

    private void broadcastEnemyToApproach() throws GameActionException {
        if (enemyToApproach == null
                || roundNumber % 10 != 7) {
            return;
        }

        //--broadcast so the information goes to robots we are spawning
        setIndicatorString(1, "broadcast enemy to approach " + enemyToApproach.location);
        Message message = MessageBuilder.buildEnemyMessage(enemyToApproach);
        rc.broadcastMessageSignal(message.getFirst(), message.getSecond(), 2);
    }

    private void processAllBroadcasts() {
        enemyTurretCount = 0;

        roundSignals = rc.emptySignalQueue();
        int signalCount = roundSignals.length;
        for (int i = 0; i < signalCount; i++) {
            if (roundSignals[i].getTeam() != team) {
                continue;
            }

            int[] message = roundSignals[i].getMessage();
            if (message != null) {
                processBroadcastWithMessage(message);
            }
        }
    }

    private void processBroadcastWithMessage(int[] message) {
        MessageType messageType = MessageParser.getMessageType(message[0], message[1]);
        if (messageType == MessageType.ENEMY) {
            enemyToApproach = MessageParser.getRobotData(message[0], message[1]);
        }
        else if (enemyTurretCount < Config.MAX_ENEMY_TURRETS
                        && messageType == MessageType.ENEMY_TURRET) {
            enemyTurrets[enemyTurretCount++] = MessageParser.getRobotData(message[0], message[1]);
        }
    }

    private void moveIfNearEnemyTurrets() throws GameActionException {
        //--so that our spawn area is safe
        if (!rc.isCoreReady()) {
            return;
        }

        if (enemyTurretCount == 0) {
            return;
        }

        Direction towardTurrets = DirectionUtil.getDirectionToward(enemyTurrets, enemyTurretCount, currentLocation);
        MapLocation towardTurret = currentLocation.add(towardTurrets);
        if (RobotUtil.anyCanAttack(enemyTurrets, enemyTurretCount, towardTurret)) {
            setIndicatorString(1, "MOVE AWAY FOR SPAWN");
            trySafeMove(towardTurrets.opposite(), enemyTurrets);
        }
    }

    private void requestHelpIfUnderAttack() throws GameActionException {
        if (RobotUtil.anyCanAttack(nearbyZombies, currentLocation)) {
            rc.broadcastSignal(senseRadius * 2);
        }
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
                || roundNumber < 90
                || !rc.isCoreReady()) {
            return;
        }

        RobotInfo[] armyUnits = RobotUtil.removeRobotsOfType(nearbyFriendlies, RobotType.ARCHON, RobotType.SCOUT, RobotType.GUARD);
        if (armyUnits.length < 2) {
            return;
        }

        MapLocation armyCenter = RobotUtil.findAverageLocation(armyUnits);
        setIndicatorString(2, "army center is " + armyCenter);
        if (armyCenter.distanceSquaredTo(currentLocation) > 8) {
            setIndicatorString(2, "moving toward center");
            if (enemyTurretCount > 0) {
                trySafeMove(DirectionUtil.getDirectionToward(nearbyFriendlies, currentLocation), enemyTurretLocations);
            }
            else {
                tryMove(DirectionUtil.getDirectionToward(nearbyFriendlies, currentLocation));
            }
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
            Direction runDirection = safestDirectionTooRunTo(nearbyEnemies, nearbyZombies);
            //--TODO check if we are going into a corner or some other trap
            tryMove(runDirection);
            rc.broadcastSignal(senseRadius - 3);
        }
    }

    private void senseRobots() {
        nearbyZombies = senseNearbyZombies();
        nearbyEnemies = senseNearbyEnemies();
        nearbyFriendlies = senseNearbyFriendlies();
        nearbyNeutrals = senseNearbyNeutrals();
        setIndicatorString(0, "nearby enemies: " + nearbyEnemies.length);
        setIndicatorString(0, "nearby zom: " + nearbyZombies.length);
        for (RobotInfo zombie : nearbyZombies) {
            previousZombieLocation.add(zombie.location);
        }
    }

    private void buildRobots() throws GameActionException {
        if (!rc.isCoreReady()
                || (roundNumber > 1000
                    && nearbyZombies.length > 0)) {
            return;
        }

        boolean useHighQueue = rc.getRobotCount() > Config.HIGH_UNIT_COUNT
                || (roundNumber > 600 && rc.getTeamParts() > 500);

        RobotType typeToBuild;
        if (useHighQueue) {
            typeToBuild = highUnitCountBuildQueue[highUnitQueuePosition % highUnitCountBuildQueue.length];
        }
        else {
            typeToBuild = lowUnitCountBuildQueue[lowUnitQueuePosition % lowUnitCountBuildQueue.length];
        }

        //--try to get a free build by grabbing a neutral
        RobotInfo neutral = RobotUtil.getRobotOfType(nearbyNeutrals, typeToBuild, RobotType.ARCHON);
        if (neutral != null) {
            if (currentLocation.isAdjacentTo(neutral.location)) {
                rc.activate(neutral.location);
                if (useHighQueue) {
                    highUnitQueuePosition++;
                }
                else {
                    lowUnitQueuePosition++;
                }

                return;
            }
            else if (nearbyEnemies.length == 0) {
                Direction toward = currentLocation.directionTo(neutral.location);
                if (rc.canMove(toward)) {
                    rc.move(toward);
                    return;
                }
            }
        }

        //--randomize builds so they go evenly between archons
        if (id % 3 == roundNumber % 3) {
            return;
        }

        //--skip scouts if we have too many
        if (typeToBuild == RobotType.SCOUT
                && tooManyScouts()) {
            if (useHighQueue) {
                highUnitQueuePosition++;
                typeToBuild = highUnitCountBuildQueue[highUnitQueuePosition % highUnitCountBuildQueue.length];
            }
            else {
                lowUnitQueuePosition++;
                typeToBuild = lowUnitCountBuildQueue[lowUnitQueuePosition % lowUnitCountBuildQueue.length];
            }
        }

        if (tryBuild(typeToBuild)) {
            if (useHighQueue) {
                highUnitQueuePosition++;
            }
            else {
                lowUnitQueuePosition++;
            }
        }
    }

    private boolean tooManyScouts() {
        if (scoutCountEstimate == 0) {
            return false;
        }

        return rc.getRobotCount() / scoutCountEstimate < 4;
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
        if (nearbyEnemies.length > 0
                || !rc.isCoreReady()) {
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
            trySafeMoveDigOnto(closest, enemyTurrets, enemyTurretCount);
            return;
        }

        if (previousPartLocation != null) {
            setIndicatorString(2, "moving toward memory parts");
            trySafeMoveDigOnto(previousPartLocation, enemyTurrets, enemyTurretCount);
            rc.broadcastSignal(31);
        }
    }

    private boolean zombiesAreDangerous() {
        if (nearbyZombies.length == 0) {
            return false;
        }

        return nearbyZombies.length > nearbyFriendlies.length
                || RobotUtil.canAttackInOneMove(nearbyZombies, currentLocation);
    }
}