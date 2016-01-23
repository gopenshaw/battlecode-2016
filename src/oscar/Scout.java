package oscar;

import battlecode.common.*;
import oscar.message.Message;
import oscar.message.MessageBuilder;
import oscar.message.MessageParser;
import oscar.message.Serializer;
import oscar.message.consensus.ZombiesDeadConsensus;
import oscar.nav.SquarePath;
import oscar.util.*;

public class Scout extends Robot {
    private static final int ROUNDS_TO_REVERSE = 4;
    private static final int MIN_PAIRING_ROUND = 300;
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
    private int[] denDirectLength = new int[Config.MAX_DENS];
    private int[] denRightBugLength = new int[Config.MAX_DENS];
    private MapLocation[][] denWaypoint = new MapLocation[Config.MAX_DENS][Config.MAX_WAYPOINTS];
    private int[] waypointCount = new int[Config.MAX_DENS];

    private MapLocation[] zombieDen = new MapLocation[Config.MAX_DENS];
    private int zombieDenCount;
    private MapLocation unknownLocation;

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
            if (!zombiesDead.isConsensusReached()) {
                discoverDestroyedDens();
                readDenMessages();
            }

            checkNearbyDensForPath();
//            addNearbyDensToDenQueue();
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
            moveToSafety();
            moveCloser();
            broadcastAllTurrets();
            broadcastZombies();
            unpairIfZombiesAreClose();
        }
    }

    private void firstRound() {
        if (startLocation == null) {
            startLocation = rc.getLocation();
            setIndicatorString(0, "start location is " + startLocation);
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

    private void checkNearbyDensForPath() throws GameActionException {
        if (roundNumber > 200) {
            //--TODO remove this (obviously)
            return;
        }

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
                    Message path;
                    if (denDirectLength[i] <= denRightBugLength[i]
                            || waypointCount[i] > 3) {
                        path = MessageBuilder.buildDenPathMessage(zombieDen[i], null, null);
                    }
                    else {
                        path = MessageBuilder.buildDenPathMessage(
                                zombieDen[i], denWaypoint[i][1], denWaypoint[i][2]);
                    }

                    rc.broadcastMessageSignal(path.getFirst(), path.getSecond(), senseRadius * 8);
                }
            }
        }
    }

    private boolean tryFindDenPath(int denIndex) throws GameActionException {
        MapLocation begin = startLocation;
        MapLocation end = zombieDen[denIndex];
        int directPath = getDirectPathCost(begin, end);
        if (directPath < 0) {
            return false;
        }

        if (waypointCount[denIndex] == 0) {
            denWaypoint[denIndex][0] = begin;
            waypointCount[denIndex] = 1;
        }

        int rightBugCost = getBugPathCost(begin, denIndex, true, 50);
        if (rightBugCost < 0) {
            return false;
        }

        rc.addMatchObservation(String.format("direction path to %s cost %d", end, directPath));
        rc.addMatchObservation(String.format("bug path to %s cost %d", end, rightBugCost));
        denDirectLength[denIndex] = directPath;
        denRightBugLength[denIndex] = rightBugCost;

        int numberOfWaypoints = waypointCount[denIndex];
        for (int i = 0; i < numberOfWaypoints; i++) {
            rc.addMatchObservation(String.format("PRE-COMPRESSION: firstWaypoint %d location %s", i, denWaypoint[denIndex][i]));
        }

        compressWaypoints(denIndex);
        numberOfWaypoints = waypointCount[denIndex];
        for (int i = 0; i < numberOfWaypoints; i++) {
            rc.addMatchObservation(String.format("POST-COMPRESSION: firstWaypoint %d location %s", i, denWaypoint[denIndex][i]));
        }

        if (numberOfWaypoints == 3
                && id == 3158) {
            rc.addMatchObservation(String.format("BROADCAST: path toward %s", zombieDen[denIndex]));
            rc.addMatchObservation(String.format("SERIALIZED: %d", Serializer.encode(zombieDen[denIndex])));
            Message message = MessageBuilder.buildDenPathMessage(zombieDen[denIndex], denWaypoint[denIndex][1],
                    denWaypoint[denIndex][2]);
            rc.broadcastMessageSignal(message.getFirst(), message.getSecond(), senseRadius * 6);
        }

        return true;
    }

    private void compressWaypoints(int denIndex) {
        //--see if a firstWaypoint can reach the destination
        //--if it can, remove all intermediate waypoints
        int numberOfWaypoints = waypointCount[denIndex];
        MapLocation destination = zombieDen[denIndex];
        for (int i = 1; i < numberOfWaypoints - 1; i++) {
            MapLocation source = denWaypoint[denIndex][i];
            if (directPathNoRubble(source, destination) == Ternary.TRUE) {
                waypointCount[denIndex] = i + 1;
                rc.addMatchObservation(String.format("after compression waypoint count is %d", i + 1));
                return;
            }
        }
    }

    private int getBugPathCost(MapLocation begin, int denIndex, boolean turnRight, int maxLength) throws GameActionException {
        MapLocation end = zombieDen[denIndex];
        if (unknownLocation != null) {
            rc.addMatchObservation(String.format("WAITING for %s - bug from %s to %s", unknownLocation, begin, end));
            return -1;
        }

        rc.addMatchObservation(String.format("TRYING - bug from %s to %s", begin, end));
        MapLocation current = begin;
        int pathLength = 0;
        Direction direction = current.directionTo(end);
        rc.addMatchObservation(String.format("checking in front of wall %s %s", current, direction));
        Ternary inFrontOfWall = inFrontOfWall(current, direction);
        if (inFrontOfWall == Ternary.UNKNOWN) {
            rc.addMatchObservation(String.format("can't know if in front of wall %s %s", current, direction));
            return -1;
        }

        boolean followingWall = inFrontOfWall == Ternary.TRUE ? true : false;
        rc.addMatchObservation(String.format("following wall? %s", followingWall));
        int distanceStartBugging;
        if (followingWall) {
            distanceStartBugging = current.distanceSquaredTo(end);
        }
        else {
            distanceStartBugging = 0;
        }

        rc.addMatchObservation(String.format("distance start bugging %d", distanceStartBugging));

        int d = turnRight ? 7 : 1;
        while (!current.equals(end)) {
            pathLength = pathLength + 2;
            if (pathLength > maxLength) {
                return -1;
            }

            Ternary reachable = reachableFromLastWaypoint(current, denIndex);
            if (reachable == Ternary.UNKNOWN) {
                unknownLocation = current;
                rc.addMatchObservation(String.format("unknown location to firstWaypoint %s", current));
                return -1;
            }

            if (reachable == Ternary.TRUE) {
                denWaypoint[denIndex][waypointCount[denIndex]] = current;
            }
            else {
                if (denWaypoint[denIndex][waypointCount[denIndex]] == null) {
                    denWaypoint[denIndex][waypointCount[denIndex]] = current;
                }

                rc.addMatchObservation(String.format("WAYPOINT %d: %s", waypointCount[denIndex], denWaypoint[denIndex][waypointCount[denIndex]]));
                waypointCount[denIndex]++;
            }

            int x = current.x % 100;
            int y = current.y % 100;
            if (!recordedLocation[x][y]) {
                unknownLocation = current;
                rc.addMatchObservation(String.format("setting unknown location %s", current));
                return -1;
            }

            if (followingWall) {
                //--keep the wall on our left (default) and follow the wall until
                // we are closer than when we started bugging
                int currentDistance = current.distanceSquaredTo(end);
                if (currentDistance < distanceStartBugging) {
                    followingWall = false;
                }
                else {
                    int directionNumber = getDirectionNumber(direction);
                    Direction checkDirection = directions[(directionNumber + 2 * d) % 8];
                    Direction followDirection = getFollowDirection(current, checkDirection, turnRight);
                    if (followDirection == null) {
                        rc.addMatchObservation(String.format("could not find follow direction %s %s", current, checkDirection));
                        return -1;
                    }

                    rc.addMatchObservation(String.format("FOLLOW - from position %s going %s", current, followDirection));
                    current = current.add(followDirection);
                    direction = followDirection;
                    continue;
                }
            }

            if (!followingWall) { // might change to not following mid move
                direction = current.directionTo(end);
                inFrontOfWall = inFrontOfWall(current, direction);
                if (inFrontOfWall == Ternary.UNKNOWN) {
                    rc.addMatchObservation(String.format("can't know if in front of wall %s %s", current, direction));
                    return -1;
                }

                if (inFrontOfWall == Ternary.TRUE) {
                    rc.addMatchObservation(String.format("in front of wall %s %s", current, direction));
                    //--measure the current distance to destination and start following wall
                    distanceStartBugging = current.distanceSquaredTo(end);
                    followingWall = true;
                    Direction followDirection = getFollowDirection(current, direction, turnRight);
                    if (followDirection == null) {
                        rc.addMatchObservation(String.format("could not find follow direction %s %s", current, direction));
                        return -1;
                    }

                    rc.addMatchObservation(String.format("WALL - from position %s going %s", current, followDirection));
                    current = current.add(followDirection);
                    direction = followDirection;
                }
                else {
                    //--go straight ahead
                    rc.addMatchObservation(String.format("STRAIGHT - from position %s going %s", current, direction));
                    current = current.add(direction);
                }
            }
        }

        rc.addMatchObservation(String.format("found path of length %d", pathLength));
        return pathLength;
    }

    private Ternary reachableFromLastWaypoint(MapLocation destination, int denIndex) {
        MapLocation source = denWaypoint[denIndex][waypointCount[denIndex] - 1];
        return directPathNoRubble(source, destination);
    }

    private Ternary directPathNoRubble(MapLocation source, MapLocation destination) {
        //--TODO should allows small rotations
        if (source.equals(destination)) {
            return Ternary.TRUE;
        }

        MapLocation current = source;
        while (!current.equals(destination)) {
            int x = current.x % 100;
            int y = current.y % 100;
            if (!recordedLocation[x][y]) {
                return Ternary.UNKNOWN;
            }

            if (rubble[x][y] > 0) {
                rc.addMatchObservation(String.format("WAYPOINT check found rubble %s source %s dest %s", current, source, destination));
                return Ternary.FALSE;
            }

            current = current.add(current.directionTo(destination));
        }

        return Ternary.TRUE;
    }

    private Direction getFollowDirection(MapLocation location, Direction direction, boolean turnRight) throws GameActionException {
        Direction currentDirection = direction;
        int currentDirectionNumber = getDirectionNumber(direction);
        MapLocation next = location.add(currentDirection);
        int d = turnRight ? 1 : 7;
        while (true) {
            if (!onTheMap(next)) {
                return null;
            }

            int x = next.x % 100;
            int y = next.y % 100;
            if (!recordedLocation[x][y]) {
                unknownLocation = next;
                rc.addMatchObservation(String.format("setting unknown location %s", next));
                return null;
            }

            if (rubble[x][y] == 0) {
                return currentDirection;
            }

            currentDirectionNumber  = (currentDirectionNumber + d) % 8;
            currentDirection = directions[currentDirectionNumber];
            next = location.add(currentDirection);
        }
    }

    private Ternary inFrontOfWall(MapLocation current, Direction direction) throws GameActionException {
        MapLocation next = current.add(direction);
        if (!onTheMap(next)) {
            return Ternary.FALSE;
        }

        int x = next.x % 100;
        int y = next.y % 100;
        if (!recordedLocation[x][y]) {
            unknownLocation = next;
            rc.addMatchObservation(String.format("setting unknown location %s", next));
            return Ternary.UNKNOWN;
        }

        return rubble[x][y] > 0 ? Ternary.TRUE : Ternary.FALSE;
    }

    private boolean onTheMap(MapLocation next) {
        //we need to check map bounds
        return true;
    }

    private int getDirectPathCost(MapLocation begin, MapLocation end) {
        MapLocation current = begin;
        int cost = 0;

        while (!current.equals(end)) {
            int x = current.x % 100;
            int y = current.y % 100;
            if (!recordedLocation[x][y]) {
                unknownLocation = current;
                rc.addMatchObservation(String.format("setting unknown location %s", current));
                return -1;
            }

            cost += 2 + (2 * RubbleUtil.getRoundsToMakeMovable(rubble[x][y])); // 2 to move, 2 to clear rubble
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

        if (unknownLocation != null) {
            if (rc.canSenseLocation(unknownLocation)) {
                unknownLocation = null;
                rc.addMatchObservation("clearing unknown location");
            }
            else {
                tryMoveToward(unknownLocation);
                return;
            }
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
