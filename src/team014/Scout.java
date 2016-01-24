package team014;

import battlecode.common.*;
import team014.message.Message;
import team014.message.MessageBuilder;
import team014.message.MessageParser;
import team014.message.consensus.ZombiesDeadConsensus;
import team014.nav.SquarePath;
import team014.util.*;

public class Scout extends Robot {
    private static final int ROUNDS_TO_REVERSE = 4;
    private static final int MIN_PAIRING_ROUND = 300;
    private final int ZOMBIE_BROADCAST_RADIUS = senseRadius * 3;
    private final SquarePath initialPath;
    private final MapBounds mapEstimate;
    private Direction exploreDirection;
    private final int LOOKAHEAD_LENGTH = 5;
    private RobotInfo[] nearbyZombies;
    private int ignoreEnemiesRound;
    private RobotInfo[] nearbyEnemies;
    private Signal[] roundSignals;

    private RobotInfo lastEnemy;

    private RobotInfo lastZombieAddedToMessageStore = null;
    private RobotInfo[] nearbyFriendlies;
    private RobotInfo myPair;
    private final ZombiesDeadConsensus zombiesDead;

    private RobotQueueNoDuplicates zombieDens;
    private BoundedQueue<Integer> destroyedDens;

    private boolean[] denDestroyed = new boolean[32001];
    private boolean initialPathCompleted;
    private boolean amFirstScout;

    public Scout(RobotController rc) {
        super(rc);
        zombiesDead = new ZombiesDeadConsensus(rc);
        zombieDens = new RobotQueueNoDuplicates(Config.MAX_DENS);
        destroyedDens = new BoundedQueue<Integer>(Config.MAX_DENS);
        amFirstScout = rc.getRoundNum() < 40;
        mapEstimate = MapUtil.getBoundsThatEncloseLocations(rc.getInitialArchonLocations(rc.getTeam()),
                rc.getInitialArchonLocations(rc.getTeam().opponent()));
        int pathRadius = Math.min(mapEstimate.getHeight(), mapEstimate.getWidth()) / 4;
        System.out.printf("height %d width %d radius\n", mapEstimate.getHeight(), mapEstimate.getWidth(), pathRadius);
        initialPath = new SquarePath(rc.getLocation(), pathRadius, rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        getTurretBroadcasts(roundSignals);
        senseRobots();
        zombiesDead.updateZombieCount(nearbyZombies.length, roundNumber);

        updateConnectionWithPair();
        getPairIfUnpaired();
        if (myPair == null) {
            zombiesDead.participate(roundSignals, roundNumber);
            if (!zombiesDead.isConsensusReached()) {
                discoverDestroyedDens();
                readDenMessages();
            }

            addNearbyDensToDenQueue();
            broadcastZombies();
            if (!zombiesDead.isConsensusReached()) {
                broadcastDensAndDestroyedDens();
            }

            broadcastEnemy();
            explore();
            moveAwayFromZombies();
        } else if (myPair.team == team) {
            zombiesDead.observe(roundSignals, roundNumber);
            moveTowardMyPair();
            broadcastTargets();
            int nearbyTTMs = RobotUtil.getCountOfType(nearbyFriendlies, RobotType.TTM);
            int nearbyTurrets = RobotUtil.getCountOfType(nearbyFriendlies, RobotType.TURRET);
            int desiredFriendlies = (nearbyTTMs + nearbyTurrets) * 6;
            if (nearbyFriendlies.length < desiredFriendlies) {
                rc.broadcastSignal(senseRadius * 2);
            }
        } else {
            // we are watching enemy turrets
            zombiesDead.observe(roundSignals, roundNumber);
            moveToSafety();
            moveCloser();
            broadcastAllTurrets();
            broadcastZombies();
            unpairIfZombiesAreClose();
        }
    }

    private void unpairIfZombiesAreClose() {
        if (nearbyZombies.length > 0) {
            myPair = null;
        }
    }

    private void moveCloser() throws GameActionException {
        if (myPair == null
                || !rc.isCoreReady()) {
            return;
        }

        Direction towardEnemy = DirectionUtil.getDirectionToward(nearbyEnemies, currentLocation);
        setIndicatorString(2, "move closer");
        Direction safeTowardEnemy = getSafeMoveDirectionConsideringTTMsTurrets(towardEnemy, nearbyEnemies);
        if (safeTowardEnemy == null) {
            return;
        }

        if (currentLocation.add(safeTowardEnemy).distanceSquaredTo(myPair.location) > type.sensorRadiusSquared) {
            //--don't move if I would no longer be able to see my pair
            return;
        }

        rc.move(safeTowardEnemy);
    }


    private void moveToSafety() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] robotsCanAttackMe = RobotUtil.getRobotsCanAttack(nearbyEnemies, currentLocation);
        if (robotsCanAttackMe != null
                && robotsCanAttackMe.length > 0) {
            setIndicatorString(2, "move to safety");
            tryMove(DirectionUtil.getDirectionAwayFrom(robotsCanAttackMe, currentLocation));
        }
    }

    private void broadcastAllTurrets() throws GameActionException {
        RobotInfo[] enemyTurrets = RobotUtil.getRobotsOfType(nearbyEnemies, RobotType.TURRET);
        if (enemyTurrets == null) {
            return;
        }

        for (RobotInfo robot : enemyTurrets) {
            Message message = MessageBuilder.buildTurretMessage(robot);
            rc.broadcastMessageSignal(message.getFirst(), message.getSecond(), senseRadius * 2);
        }
    }

    private void readDenMessages() {
        int[][] messages = getMessagesOfType(roundSignals, MessageType.ZOMBIE);
        int messageCount = messages.length;
        for (int i = 0; i < messageCount; i++) {
            if (messages[i] == null) {
                break;
            }

            RobotData zombie = MessageParser.getRobotData(messages[i]);
            if (zombie.type == RobotType.ZOMBIEDEN
                    && !denDestroyed[zombie.id]) {
                zombieDens.add(zombie);
            }
        }
    }

    private void discoverDestroyedDens() throws GameActionException {
        checkLocationWeCanSense();
        checkBroadcastsForDestroyedDens();
    }

    private void checkBroadcastsForDestroyedDens() {
        int[][] messages = getMessagesOfType(roundSignals, MessageType.DESTROYED_DENS);
        int messageCount = messages.length;
        for (int i = 0; i < messageCount; i++) {
            if (messages[i] == null) {
                break;
            }

            DestroyedDenData densFromBroadcast = MessageParser.getDestroyedDens(messages[i][0], messages[i][1]);
            for (int j = 0; j < densFromBroadcast.numberOfDens; j++) {
                int denId = densFromBroadcast.denId[j];
                if (!denDestroyed[denId]) {
                    denDestroyed[denId] = true;
                    destroyedDens.add(denId);
                }
            }
        }
    }

    private void checkLocationWeCanSense() throws GameActionException {
        int count = zombieDens.getSize();
        for (int i = 0; i < count; i++) {
            RobotData den = zombieDens.remove();
            if (rc.canSenseLocation(den.location)) {
                RobotInfo robotAtLocation = rc.senseRobotAtLocation(den.location);
                if (robotAtLocation == null
                        || robotAtLocation.ID != den.id) {
                    denDestroyed[den.id] = true;
                    destroyedDens.add(den.id);
                }
            }
            else {
                zombieDens.add(den);
            }
        }
    }

    private void broadcastDensAndDestroyedDens() throws GameActionException {
        int mod = roundNumber % 3;
        if (mod == 0) {
            broadcastNextDen();
        }
        else if (mod == 1) {
            broadcastDestroyedDens();
        }
    }

    private void broadcastDestroyedDens() throws GameActionException {
        int size = destroyedDens.getSize();
        if (size == 0) {
            return;
        }
        else if (size > 4) {
            size = 4;
        }

        DestroyedDenData denData = new DestroyedDenData(size);
        for (int i = 0; i < size; i++) {
            int id = destroyedDens.remove();
            denData.denId[i] = id;
            destroyedDens.add(id);
        }

        Message message = MessageBuilder.buildDestroyedDenMessage(denData);
        rc.broadcastMessageSignal(message.getFirst(), message.getSecond(), getDestroyedDenBroadcastRadius());
    }

    private void broadcastNextDen() throws GameActionException {
        if (zombieDens.isEmpty()) {
            return;
        }

        RobotData den = zombieDens.remove();
        while (denDestroyed[den.id]) {
            if (zombieDens.isEmpty()) {
                return;
            }

            den = zombieDens.remove();
        }

        zombieDens.add(den);
        Message message = MessageBuilder.buildZombieMessage(den);
        rc.broadcastMessageSignal(message.getFirst(), message.getSecond(), senseRadius * 6);
    }

    private void broadcastTargets() throws GameActionException {
        if (nearbyEnemies.length == 0) {
            return;
        }

        RobotInfo[] highValueTargets = RobotUtil.getRobotsOfType(nearbyEnemies, RobotType.SCOUT);
        if (highValueTargets != null
                && highValueTargets.length != 0) {
            RobotInfo closest = RobotUtil.getClosestRobotToLocation(highValueTargets, currentLocation);
            Message target = MessageBuilder.buildTargetMessage(closest);
            rc.broadcastMessageSignal(target.getFirst(), target.getSecond(), 2);
            return;
        }

        highValueTargets = RobotUtil.getRobotsOfType(nearbyEnemies, RobotType.TURRET, RobotType.ARCHON);
        if (highValueTargets != null
                && highValueTargets.length != 0) {
            RobotInfo closest = RobotUtil.getClosestRobotToLocation(highValueTargets, currentLocation);
            Message target = MessageBuilder.buildTargetMessage(closest);
            rc.broadcastMessageSignal(target.getFirst(), target.getSecond(), 2);
            return;
        }

        if (enemyTurretCount > 0) {
            RobotData closest = RobotUtil.getClosestRobotToLocation(enemyTurrets, enemyTurretCount, currentLocation);
            Message target = MessageBuilder.buildTargetMessage(closest);
            rc.broadcastMessageSignal(target.getFirst(), target.getSecond(), 2);
            return;
        }


        RobotInfo closest = RobotUtil.getClosestRobotToLocation(nearbyEnemies, currentLocation);
        if (closest == null) {
            return;
        }

        Message target = MessageBuilder.buildTargetMessage(closest);
        rc.broadcastMessageSignal(target.getFirst(), target.getSecond(), 2);
    }

    private void moveTowardMyPair() throws GameActionException {
        if (myPair == null
                || !rc.isCoreReady()) {
            return;
        }

        if (!currentLocation.isAdjacentTo(myPair.location)) {
            setIndicatorString(2, "move toward my pair");
            tryMove(currentLocation.directionTo(myPair.location));
        }
    }

    private void updateConnectionWithPair() throws GameActionException {
        if (myPair == null) {
            return;
        }

        if (!rc.canSenseRobot(myPair.ID)) {
            setIndicatorString(0, "can't sense pair " + myPair.ID);
            myPair = null;
            return;
        }

        myPair = rc.senseRobot(myPair.ID);
        setIndicatorString(0, "update connection with pair " + myPair.ID);
        broadcastPairMessage(myPair);
    }

    private void broadcastPairMessage(RobotInfo myPair) throws GameActionException {
        Message pairMessage = MessageBuilder.buildPairingMessage(myPair);
        rc.broadcastMessageSignal(pairMessage.getFirst(), pairMessage.getSecond(), senseRadius * 2);
    }

    private void getPairIfUnpaired() throws GameActionException {
        if (myPair != null
                || nearbyZombies.length > 0
                || roundNumber < MIN_PAIRING_ROUND) {
            return;
        }

        RobotInfo[] turrets = RobotUtil.getRobotsOfType(nearbyFriendlies, RobotType.TURRET, RobotType.TTM);
        if (tryPairWithOneRobot(turrets)) {
            return;
        }

        RobotInfo[] sensedEnemyTurrets = RobotUtil.getRobotsOfType(nearbyEnemies, RobotType.TURRET);
        if (sensedEnemyTurrets == null) {
            return;
        }

        for (int i = 0; i < enemyTurrets.length; i++) {
            if (enemyTurrets[i] == null) {
                break;
            }
            else {
//                setIndicatorString(1, " " + enemyTurrets[i].location);
            }
        }

        RobotInfo[] turretsNotBeingBroadcast = RobotUtil.removeRobots(sensedEnemyTurrets, enemyTurrets);
        if (turretsNotBeingBroadcast.length > 0) {
            tryPairWithOneRobot(turretsNotBeingBroadcast);
        }
    }

    private boolean tryPairWithOneRobot(RobotInfo[] turrets) throws GameActionException {
        if (turrets == null
                || turrets.length == 0) {
            return false;
        }

        RobotInfo unpairedTurret = getUnpairedTurret(turrets, roundSignals);
        if (unpairedTurret == null) {
            return false;
        }

        myPair = unpairedTurret;
        setIndicatorString(0, "pairing with " + myPair.ID);
        broadcastPairMessage(myPair);
        return true;
    }

    private RobotInfo getUnpairedTurret(RobotInfo[] turrets, Signal[] roundSignals) {
        for (RobotInfo turret : turrets) {
            if (!signalsContainPairingMessage(turret, roundSignals)) {
                return turret;
            }
        }

        return null;
    }

    private boolean signalsContainPairingMessage(RobotInfo robot, Signal[] roundSignals) {
        for (Signal s : roundSignals) {
            if (s.getTeam() == team) {
                int[] message = s.getMessage();
                if (message == null) continue;
                if (MessageParser.getMessageType(message[0], message[1]) == MessageType.PAIR
                        && MessageParser.pairs(message[0], message[1], robot)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void broadcastEnemy() throws GameActionException {
        if (!zombiesDead.isConsensusReached()
                || lastEnemy == null) {
            return;
        }

        Message enemyMessage = MessageBuilder.buildEnemyMessage(lastEnemy);
        rc.broadcastMessageSignal(enemyMessage.getFirst(), enemyMessage.getSecond(), senseRadius * 4);
    }


    private void addNearbyDensToDenQueue() {
        for (RobotInfo zombie : nearbyZombies) {
            if (zombie.type == RobotType.ZOMBIEDEN
                    && zombie != lastZombieAddedToMessageStore) {
                zombieDens.add(new RobotData(zombie.ID, zombie.location, (int) zombie.health, zombie.type));
            }
        }
    }

    private void broadcastZombies() throws GameActionException {
        if (nearbyZombies.length == 0) {
            return;
        }

        RobotInfo closestZombie = RobotUtil.getClosestRobotToLocation(nearbyZombies, currentLocation);
        Message zombieMessage = MessageBuilder.buildZombieMessage(closestZombie);
        rc.broadcastMessageSignal(zombieMessage.getFirst(), zombieMessage.getSecond(), ZOMBIE_BROADCAST_RADIUS);
    }

    private void senseRobots() {
        nearbyZombies = senseNearbyZombies();
        nearbyEnemies = senseNearbyEnemies();
        nearbyFriendlies = senseNearbyFriendlies();

        if (nearbyEnemies.length > 0) {
            RobotInfo highPriority = RobotUtil.getHighestPriorityEnemyUnit(nearbyEnemies);
            if (lastEnemy == null
                    || RobotUtil.getPriority(highPriority.type) >= RobotUtil.getPriority(lastEnemy.type)) {
                lastEnemy = highPriority;
            }
        }
    }

    private void moveAwayFromZombies() throws GameActionException {
        if (nearbyZombies.length == 0
                || !rc.isCoreReady()) {
            return;
        }

        tryMove(DirectionUtil.getDirectionAwayFrom(nearbyZombies, nearbyEnemies, currentLocation));
    }

    private void explore() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (amFirstScout
                && roundNumber < 300
                && !initialPathCompleted) {
            Direction pathDirection = initialPath.getNextDirection(currentLocation);
            if (initialPath.getRotationsCompleted() > 0) {
                initialPathCompleted = true;
            }
            else {
                trySafeMove(pathDirection, nearbyEnemies, nearbyZombies);
                return;
            }
        }

        if (exploreDirection == null) {
            exploreDirection = getExploreDirection(null);
        }

        if (RobotUtil.anyCanAttack(nearbyEnemies, currentLocation)
                && roundNumber > ignoreEnemiesRound + ROUNDS_TO_REVERSE) {
            exploreDirection = exploreDirection.opposite();
            ignoreEnemiesRound = roundNumber;
        }
        else {
            //--check ahead
            MapLocation lookaheadLocation = currentLocation.add(exploreDirection, LOOKAHEAD_LENGTH);
            Direction newDirection = null;
            while (!rc.onTheMap(lookaheadLocation)) {
                newDirection = getExploreDirection(exploreDirection);
                lookaheadLocation = currentLocation.add(newDirection, LOOKAHEAD_LENGTH);
            }

            if (newDirection != null) {
                exploreDirection = newDirection;
            }
        }

        if (RobotUtil.anyCanAttack(nearbyZombies, currentLocation.add(exploreDirection, 2))) {
            return;
        }

        if (rc.isCoreReady()) {
            tryMove(exploreDirection);
        }
    }


    private Direction getExploreDirection(Direction previousDirection) {
        if (previousDirection == null) {
            return getRandomDirection();
        }

        Direction newDirection = getRandomDirection();
        while (newDirection.equals(exploreDirection)
                || newDirection.opposite() == exploreDirection) {
            newDirection = getRandomDirection();
        }

        return newDirection;
    }

    public int getDestroyedDenBroadcastRadius() {
        if (roundNumber % 10 == id % 10
                && nearbyEnemies.length == 0) {
            return 1000; // core delay 8.02
        }
        else {
            return senseRadius * 2; // core delay 0.05
        }
    }
}
