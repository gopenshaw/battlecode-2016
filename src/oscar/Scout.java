package oscar;

import battlecode.common.*;
import oscar.message.Message;
import oscar.message.MessageBuilder;
import oscar.message.MessageParser;
import oscar.message.consensus.ZombiesDeadConsensus;
import oscar.nav.SquarePath;
import oscar.util.*;

public class Scout extends Robot {
    private static final int ROUNDS_TO_REVERSE = 4;
    private static final int MIN_PAIRING_ROUND = 300;
    private static final int MAX_WAYPOINTS = 3;
    private final int ZOMBIE_BROADCAST_RADIUS = senseRadius * 3;
    private final SquarePath initialPath;
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

    private RobotQueueNoDuplicates zombieDenQueue;
    private BoundedQueue<Integer> destroyedDens;

    private boolean[] denDestroyed = new boolean[32001];
    private boolean initialPathCompleted;
    private boolean amFirstScout;

    private boolean[][] recordedLocation = new boolean[100][100];
    private int[][] rubble = new int[100][100];
    private MapLocation startLocation;

    private boolean[] recordedZombieDen = new boolean[32001];
    private boolean[] denHasPath = new boolean[Config.MAX_DENS];
    private MapLocation[][] zombieDenPath = new MapLocation[Config.MAX_DENS][MAX_WAYPOINTS];
    private MapLocation[] zombieDen = new MapLocation[Config.MAX_DENS];
    private int zombieDenCount;

    public Scout(RobotController rc) {
        super(rc);
        zombiesDead = new ZombiesDeadConsensus(rc);
        zombieDenQueue = new RobotQueueNoDuplicates(Config.MAX_DENS);
        destroyedDens = new BoundedQueue<Integer>(Config.MAX_DENS);
        initialPath = new SquarePath(rc.getLocation(), 8, rc);
        amFirstScout = rc.getRoundNum() < 40;
    }

    @Override
    protected void doTurn() throws GameActionException {
        firstRound();
        roundSignals = rc.emptySignalQueue();
        getTurretBroadcasts(roundSignals);
        senseRobots();
        zombiesDead.updateZombieCount(nearbyZombies.length, roundNumber);

        readRubble();
        updateConnectionWithPair();
        getPairIfUnpaired();
        if (myPair == null) {
            zombiesDead.participate(roundSignals, roundNumber);
            setIndicatorString(2, "zombies dead " + zombiesDead.isConsensusReached());
            if (!zombiesDead.isConsensusReached()) {
                discoverDestroyedDens();
                readDenMessages();
            }

            checkNearbyDensForPath();
            addNearbyDensToDenQueue();
            broadcastZombies();
            if (!zombiesDead.isConsensusReached()) {
                broadcastDensAndDestroyedDens();
            }

            broadcastEnemy();
            moveAwayFromZombies();
            explore();
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
            setIndicatorString(2, "pair with " + myPair.ID);
            moveToSafety();
            moveCloser();
            broadcastAllTurrets();
            broadcastZombies();
            unpairIfZombiesAreClose();
        }
    }

    private void firstRound() {
        if (startLocation == null) {
            startLocation = currentLocation;
        }
    }

    private void readRubble() throws GameActionException {
        setBytecodeIndicator(1, "before rubble");
        MapLocation[] nearbyLocations = MapLocation.getAllMapLocationsWithinRadiusSq(currentLocation, senseRadius);
        int locationCount = nearbyLocations.length;
        for (int i = 0; i < locationCount; i++) {
            MapLocation location = nearbyLocations[i];
            int x = location.x % 100;
            int y = location.y % 100;
            if (!recordedLocation[x][y]) {
                if (rc.onTheMap(location)) {
                    rubble[x][y] = (int) rc.senseRubble(location);
                }

                recordedLocation[x][y] = true;
            }
        }
        setBytecodeIndicator(1, "after rubble");
    }

    private void unpairIfZombiesAreClose() {
        if (nearbyZombies.length > 0) {
            myPair = null;
        }
    }

    private void moveCloser() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        Direction towardEnemy = DirectionUtil.getDirectionToward(nearbyEnemies, currentLocation);
        trySafeMove(towardEnemy, nearbyEnemies);
    }


    private void moveToSafety() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (RobotUtil.anyCanAttack(nearbyEnemies, currentLocation)) {
            tryMove(DirectionUtil.getDirectionAwayFrom(nearbyEnemies, currentLocation));
        }
    }

    private void broadcastAllTurrets() throws GameActionException {
        setIndicatorString(0, "turrets: ");
        RobotInfo[] enemyTurrets = RobotUtil.getRobotsOfType(nearbyEnemies, RobotType.TURRET);
        if (enemyTurrets == null) {
            return;
        }

        for (RobotInfo robot : enemyTurrets) {
            setIndicatorString(0, " " + robot.location);
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
                zombieDenQueue.add(zombie);
            }
        }
    }

    private void discoverDestroyedDens() {
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

    private void checkLocationWeCanSense() {
        int count = zombieDenQueue.getSize();
        for (int i = 0; i < count; i++) {
            RobotData den = zombieDenQueue.remove();
            if (rc.canSenseLocation(den.location)
                    && !rc.canSenseRobot(den.id)) {
                denDestroyed[den.id] = true;
                destroyedDens.add(den.id);
            }
            else {
                zombieDenQueue.add(den);
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

        setIndicatorString(1, "broadcast destroyed dens");
        Message message = MessageBuilder.buildDestroyedDenMessage(denData);
        rc.broadcastMessageSignal(message.getFirst(), message.getSecond(), getDestroyedDenBroadcastRadius());
    }

    private void broadcastNextDen() throws GameActionException {
        if (zombieDenQueue.isEmpty()) {
            return;
        }

        RobotData den = zombieDenQueue.remove();
        while (denDestroyed[den.id]) {
            if (zombieDenQueue.isEmpty()) {
                return;
            }

            den = zombieDenQueue.remove();
        }

        zombieDenQueue.add(den);
        Message message = MessageBuilder.buildZombieMessage(den);
        setIndicatorString(1, "broadcast den " + den.location);
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
            tryMove(currentLocation.directionTo(myPair.location));
        }
    }

    private void updateConnectionWithPair() throws GameActionException {
        if (myPair == null) {
            return;
        }

        if (!rc.canSenseRobot(myPair.ID)) {
            myPair = null;
            return;
        }

        myPair = rc.senseRobot(myPair.ID);
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

        setIndicatorString(1, "reading turrets");
        for (int i = 0; i < enemyTurrets.length; i++) {
            if (enemyTurrets[i] == null) {
                break;
            }
            else {
                setIndicatorString(1, " " + enemyTurrets[i].location);
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

    private void checkNearbyDensForPath() {
        setIndicatorString(2, "check nearby dens for path");
        for (RobotInfo zombie : nearbyZombies) {
            if (zombie.type == RobotType.ZOMBIEDEN) {
                if (!recordedZombieDen[zombie.ID]) {
                    setIndicatorString(2, "recording den " + zombie.location);
                    zombieDen[zombieDenCount++] = zombie.location;
                }

                recordedZombieDen[zombie.ID] = true;
            }
        }

        for (int i = 0; i < zombieDenCount; i++) {
            if (!denHasPath[i]) {
                setIndicatorString(2, "try find path for index: " + i);
                if (tryFindDenPath(i)) {
                    denHasPath[i] = true;
                }
            }
        }
    }

    private boolean tryFindDenPath(int i) {
        MapLocation begin = startLocation;
        MapLocation end = zombieDen[i];
        int directPath = getDirectPathCost(begin, end);
        int bugPathCost = getBugPathCost(begin, end);
        setIndicatorString(2, String.format("cost from %s to %s is %d", begin, end, directPath));
        if (directPath < 0) {
            return false;
        }

        return true;
    }

    private int getBugPathCost(MapLocation begin, MapLocation end, int maxLength) {
        MapLocation current = begin;
        int pathLength = 0;

        while (!current.equals(end)) {
            int x = current.x % 100;
            int y = current.y % 100;
            if (!recordedLocation[x][y]) {
                return -1;
            }

            if (++pathLength > maxLength) {
                return -1;
            }
        }

        return pathLength;
    }

    private int getDirectPathCost(MapLocation begin, MapLocation end) {
        MapLocation current = begin;
        int cost = 0;

        while (!current.equals(end)) {
            int x = current.x % 100;
            int y = current.y % 100;
            if (!recordedLocation[x][y]) {
                return -1;
            }

            cost += 1 + RubbleUtil.getRoundsToMakeMovable(rubble[x][y]);
            current = current.add(current.directionTo(end));
        }

        return cost;
    }

    private void addNearbyDensToDenQueue() {
        for (RobotInfo zombie : nearbyZombies) {
            if (zombie.type == RobotType.ZOMBIEDEN
                    && zombie != lastZombieAddedToMessageStore) {
                zombieDenQueue.add(new RobotData(zombie.ID, zombie.location, (int) zombie.health, zombie.type));
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

        if (RobotUtil.allAreType(nearbyZombies, RobotType.ZOMBIEDEN)
                && getRoundsTillNextSpawn(roundNumber) > 3) {
            return;
        }

        if (rand.nextInt(4) == 0) {
            exploreDirection = getExploreDirection(exploreDirection);
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
            boolean canSeeDen = RobotUtil.anyAreType(nearbyZombies, RobotType.ZOMBIEDEN);
            MapLocation lookaheadLocation = currentLocation.add(exploreDirection, LOOKAHEAD_LENGTH);
            Direction newDirection = null;
            while (canSeeDen
                    || !rc.onTheMap(lookaheadLocation)) {
                canSeeDen = false; // one-time flag
                newDirection = getExploreDirection(exploreDirection);
                lookaheadLocation = currentLocation.add(newDirection, LOOKAHEAD_LENGTH);
            }

            if (newDirection != null) {
                exploreDirection = newDirection;
            }
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