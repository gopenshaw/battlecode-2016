package veil;

import battlecode.common.*;
import veil.message.Message;
import veil.message.MessageBuilder;
import veil.message.MessageParser;
import veil.util.DirectionUtil;
import veil.util.LocationUtil;
import veil.util.RobotUtil;

import java.util.Random;

public abstract class Robot {
    protected final Random rand;
    protected final Team team;
    protected final Team enemy;
    protected final RobotController rc;

    protected final int senseRadius;
    protected final int attackRadius;
    protected RobotType type;
    protected int id;

    protected static MapLocation currentLocation;
    protected int roundNumber;

    private StringBuilder[] debugString;

    private static final int MAX_RUBBLE_CAN_IGNORE = 50;
    private static final int MAX_RUBBLE_CAN_PASS = 10000;

    private static int[] rotations = {0, 7, 1, 6, 2, 5, 3};
    private static int[] forwardRotations = {0, 7, 1, 6, 2};
    private static int[] moveSequence1 = {0, 7, 1};
    private static int[] digSequence1 = {0, 7, 1};
    private static int[] moveSequence2 = {6, 2, 5, 3};

    protected final Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
        Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    private int maxBytecode;
    private int maxBytecodeRound;

    protected RobotData[] enemyTurrets = new RobotData[Config.MAX_ENEMY_TURRETS];
    protected MapLocation[] enemyTurretLocations = new MapLocation[Config.MAX_ENEMY_TURRETS];
    protected int enemyTurretCount = 0;

    protected static int sensorRadius;
    protected RobotInfo[][] locationsOfNearbyEnemies;
    protected int[][] locationLastFilled;
    protected Direction previousRoundsSafestDirection;

    public Robot(RobotController rc) {
        this.rc = rc;
        rand = new Random(rc.getID());
        team = rc.getTeam();
        enemy = team.opponent();
        currentLocation = rc.getLocation();

        id = rc.getID();
        type = rc.getType();

        sensorRadius =  (int) Math.pow(type.sensorRadiusSquared, 0.5) ;
        locationsOfNearbyEnemies = new RobotInfo[2 * sensorRadius + 1][2 * sensorRadius + 1];
        locationLastFilled = new int[2 * sensorRadius + 1][2 * sensorRadius + 1];
        previousRoundsSafestDirection = Direction.NONE;

        senseRadius = type.sensorRadiusSquared;
        attackRadius = type.attackRadiusSquared;

        if (Config.DEBUG) {
            debugString = new StringBuilder[3];
            for (int i = 0; i < 3; i++) {
                debugString[i] = new StringBuilder();
            }
        }
    }

    public void run() {
        while(true) {
            try {
                currentLocation = rc.getLocation();
                roundNumber = rc.getRoundNum();
                doTurn();

                if (Config.DEBUG) {
                    resetDebugStrings();
                    updateMaxBytecode();
                }

                Clock.yield();
            } catch (GameActionException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void updateMaxBytecode() {
        int currentBytecode = Clock.getBytecodeNum();
        if (currentBytecode > maxBytecode) {
            maxBytecode = currentBytecode;
            maxBytecodeRound = roundNumber;
        }

        if (Config.BYTECODE_DEBUG) {
            setIndicatorString(2, "used " + maxBytecode + " bytecode in round " + maxBytecodeRound);
        }
    }

    private void resetDebugStrings() {
        for (int i = 0; i < debugString.length; i++) {
            debugString[i] = new StringBuilder();
        }
    }

    protected abstract void doTurn() throws GameActionException;

    protected RobotInfo[] senseNearbyEnemies() {
        return rc.senseNearbyRobots(senseRadius, enemy);
    }


    protected RobotInfo[] senseNearbyFriendlies() {
        return rc.senseNearbyRobots(senseRadius, team);
    }

    protected RobotInfo[] senseAttackableEnemies() {
        return rc.senseNearbyRobots(attackRadius, enemy);
    }

    protected RobotInfo[] senseAttackableZombies() {
        return rc.senseNearbyRobots(attackRadius, Team.ZOMBIE);
    }

    protected RobotInfo[] senseNearbyZombies() {
        return rc.senseNearbyRobots(senseRadius, Team.ZOMBIE);
    }

    public RobotInfo[] senseNearbyNeutrals() {
        return rc.senseNearbyRobots(senseRadius, Team.NEUTRAL);
    }

    protected void tryMoveToward(MapLocation location) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        if (currentLocation.equals(location)) {
            return;
        }

        Direction moveDirection = currentLocation.directionTo(location);
        if (type == RobotType.ARCHON
                || type == RobotType.TTM
                || type == RobotType.TURRET) {
            tryMove(moveDirection);
        }
        else {
            tryMoveDig(moveDirection);
        }
    }

    private void tryMoveDig(Direction targetDirection) throws GameActionException {
        int initialDirection = getDirectionNumber(targetDirection);
        if (initialDirection < 0) {
            return;
        }

        Direction currentDirection;
        for (int i = 0; i < moveSequence1.length; i++) {
            currentDirection = directions[(initialDirection + moveSequence1[i]) % 8];
            if (rc.canMove(currentDirection)) {
                rc.move(currentDirection);
                return;
            }
        }

        for (int i = 0; i < digSequence1.length; i++) {
            currentDirection = directions[(initialDirection + digSequence1[i]) % 8];
            if (tryDig(currentLocation, currentDirection)) {
                return;
            }
        }

        for (int i = 0; i < moveSequence2.length; i++) {
            currentDirection = directions[(initialDirection + moveSequence2[i]) % 8];
            if (rc.canMove(currentDirection)) {
                rc.move(currentDirection);
                return;
            }
        }

        tryClearRubble(targetDirection);
    }

    private boolean tryDig(MapLocation currentLocation, Direction direction) throws GameActionException {
        MapLocation nextLocation = currentLocation.add(direction);
        if (rc.onTheMap(nextLocation)
                && rc.senseRubble(nextLocation) < MAX_RUBBLE_CAN_PASS) {
            rc.clearRubble(direction);
            return true;
        }

        return false;
    }

    protected void trySafeMoveTowardTurret(RobotData turret) throws GameActionException {
        RobotData[] turretArray = {turret};
        trySafeMoveDigToward(turret.location, turretArray, 1);
    }

    protected boolean trySafeMoveToward(MapLocation location, RobotData[] nearbyEnemies) throws GameActionException {
        Direction direction = currentLocation.directionTo(location);
        return trySafeMove(direction, nearbyEnemies);
    }

    protected boolean trySafeMoveToward(MapLocation location, RobotInfo[] nearbyEnemies, RobotInfo[] nearbyZombies) throws GameActionException {
        Direction direction = currentLocation.directionTo(location);
        return trySafeMove(direction, nearbyEnemies, nearbyZombies);
    }

    protected boolean trySafeMoveTowardGrid(MapLocation location, MapLocation[] enemyTurretLocations) throws GameActionException {
        Direction direction = currentLocation.directionTo(location);
        return trySafeMoveGrid(direction, enemyTurretLocations);
    }

    protected boolean trySafeMoveToward(MapLocation location, MapLocation[] enemyTurretLocations) throws GameActionException {
        Direction direction = currentLocation.directionTo(location);
        return trySafeMove(direction, enemyTurretLocations);
    }

    protected void trySafeMoveDigOnto(MapLocation location, RobotData[] nearbyEnemies, int enemyCount) throws GameActionException {
        if (currentLocation.isAdjacentTo(location)
                && rc.senseRubble(location) > 100) {
            rc.clearRubble(currentLocation.directionTo(location));
        }
        else {
            Direction direction = currentLocation.directionTo(location);
            trySafeMoveDig(direction, nearbyEnemies, enemyCount);
        }
    }

    protected void trySafeMoveDigToward(MapLocation location, RobotData[] nearbyEnemies, int enemyCount) throws GameActionException {
        Direction direction = currentLocation.directionTo(location);
        trySafeMoveDig(direction, nearbyEnemies, enemyCount);
    }

    protected void trySafeMoveDig(Direction targetDirection, RobotData[] nearbyEnemies, int enemyCount) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        int initialDirection = getDirectionNumber(targetDirection);
        if (initialDirection < 0) {
            return;
        }

        Direction currentDirection;
        for (int i = 0; i < moveSequence1.length; i++) {
            currentDirection = directions[(initialDirection + moveSequence1[i]) % 8];
            if (canMoveSafely(currentDirection, currentLocation, nearbyEnemies, enemyCount)) {
                rc.move(currentDirection);
                return;
            }
        }

        for (int i = 0; i < digSequence1.length; i++) {
            currentDirection = directions[(initialDirection + digSequence1[i]) % 8];
            if (tryDig(currentLocation, currentDirection)) {
                return;
            }
        }

        for (int i = 0; i < moveSequence2.length; i++) {
            currentDirection = directions[(initialDirection + moveSequence2[i]) % 8];
            if (canMoveSafely(currentDirection, currentLocation, nearbyEnemies, enemyCount)) {
                rc.move(currentDirection);
                return;
            }
        }

        tryClearRubble(targetDirection);
    }

    protected boolean trySafeMoveGrid(Direction direction, MapLocation[] enemyTurretLocations) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        int initialDirection = getDirectionNumber(direction);
        if (initialDirection < 0) {
            return false;
        }

        for (int i = 0; i < moveSequence1.length; i++) {
            Direction d = directions[(initialDirection + moveSequence1[i]) % 8];
            MapLocation next = currentLocation.add(d);
            if (canMoveSafelyGrid(d, next, enemyTurretLocations)) {
                rc.move(d);
                return true;
            }
        }

        for (int i = 0; i < moveSequence2.length; i++) {
            Direction d = directions[(initialDirection + moveSequence2[i]) % 8];
            MapLocation next = currentLocation.add(d);
            if (canMoveSafelyGrid(d, next, enemyTurretLocations)) {
                rc.move(d);
                return true;
            }
        }

        return false;
    }

    protected boolean trySafeMove(Direction direction, MapLocation[] enemyTurretLocations) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        int initialDirection = getDirectionNumber(direction);
        if (initialDirection < 0) {
            return false;
        }

        for (int i = 0; i < moveSequence1.length; i++) {
            Direction d = directions[(initialDirection + moveSequence1[i]) % 8];
            MapLocation next = currentLocation.add(d);
            if (canMoveSafely(d, next, enemyTurretLocations)) {
                rc.move(d);
                return true;
            }
        }

        for (int i = 0; i < digSequence1.length; i++) {
            Direction d = directions[(initialDirection + digSequence1[i]) % 8];
            if (tryDig(currentLocation, d)) {
                return true;
            }
        }

        for (int i = 0; i < moveSequence2.length; i++) {
            Direction d = directions[(initialDirection + moveSequence2[i]) % 8];
            MapLocation next = currentLocation.add(d);
            if (canMoveSafely(d, next, enemyTurretLocations)) {
                rc.move(d);
                return true;
            }
        }

        return false;
    }

    protected boolean trySafeMove(Direction direction,
                                  RobotData[] nearbyEnemies) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        int initialDirection = getDirectionNumber(direction);
        if (initialDirection < 0) {
            return false;
        }

        for (int i = 0; i < rotations.length; i++) {
            Direction d = directions[(initialDirection + rotations[i]) % 8];
            MapLocation next = currentLocation.add(d);
            if (canMoveSafely(d, next, nearbyEnemies)) {
                rc.move(d);
                return true;
            }
        }

        return tryClearRubble(direction);
    }

    private boolean canMoveSafely(Direction direction, MapLocation next, RobotData[] nearbyEnemies) {
        return rc.canMove(direction)
                && !RobotUtil.anyCanAttack(nearbyEnemies, next);
    }

    private boolean canMoveSafelyGrid(Direction direction, MapLocation next, MapLocation[] nearbyTurrets) {
        if (next.x  % 2 == next.y % 2) {
            return false;
        }

        int turretAttackRange = RobotType.TURRET.attackRadiusSquared;
        return rc.canMove(direction)
                && !LocationUtil.anyWithinRange(nearbyTurrets, next, turretAttackRange);
    }

    private boolean canMoveSafely(Direction direction, MapLocation next, MapLocation[] nearbyTurrets) {
        int turretAttackRange = RobotType.TURRET.attackRadiusSquared;
        return rc.canMove(direction)
                && !LocationUtil.anyWithinRange(nearbyTurrets, next, turretAttackRange);
    }

    private boolean canMoveSafely(
            Direction direction, MapLocation next, RobotData[] nearbyEnemies, int enemyCount) {
        return rc.canMove(direction)
                && !RobotUtil.anyCanAttack(nearbyEnemies, enemyCount, next);
    }

    protected boolean trySafeMove(Direction direction, RobotInfo[] nearbyEnemies) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        int initialDirection = getDirectionNumber(direction);
        if (initialDirection < 0) {
            return false;
        }

        for (int i = 0; i < rotations.length; i++) {
            Direction d = directions[(initialDirection + rotations[i]) % 8];
            MapLocation next = currentLocation.add(d);
            if (canMoveSafely(d, next, nearbyEnemies)) {
                rc.move(d);
                return true;
            }
        }

        return tryClearRubble(direction);
    }

    protected Direction getSafeMoveDirectionConsideringTTMsTurrets(Direction direction, RobotInfo[] nearbyEnemies) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        int initialDirection = getDirectionNumber(direction);
        if (initialDirection < 0) {
            return null;
        }

        for (int i = 0; i < rotations.length; i++) {
            Direction d = directions[(initialDirection + rotations[i]) % 8];
            MapLocation next = currentLocation.add(d);
            if (canMoveSafelyConsiderTTMsTurrets(d, next, nearbyEnemies)) {
                return d;
            }
        }

        return null;
    }

    protected boolean trySafeMove(Direction direction, RobotInfo[] nearbyEnemies, RobotInfo[] nearbyZombies) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        int initialDirection = getDirectionNumber(direction);
        if (initialDirection < 0) {
            return false;
        }

        for (int i = 0; i < rotations.length; i++) {
            Direction d = directions[(initialDirection + rotations[i]) % 8];
            MapLocation next = currentLocation.add(d);
            if (canMoveSafely(d, next, nearbyEnemies, nearbyZombies)) {
                rc.move(d);
                return true;
            }
        }

        if (type.canClearRubble()) {
            return tryClearRubble(direction);
        }
        else {
            return false;
        }
    }

    private boolean canMoveSafely(Direction direction, MapLocation next, RobotInfo[] nearbyEnemies, RobotInfo[] nearbyZombies) {
        return rc.canMove(direction)
                && !RobotUtil.anyCanAttack(nearbyEnemies, next)
                && !RobotUtil.anyCanAttack(nearbyZombies, next);
    }

    private boolean canMoveSafelyConsiderTTMsTurrets(Direction direction, MapLocation next, RobotInfo[] nearbyEnemies) {
        return rc.canMove(direction)
                && !RobotUtil.anyCanAttackConsideringTTMsTurrets(nearbyEnemies, next);
    }

    private boolean canMoveSafely(Direction direction, MapLocation next, RobotInfo[] nearbyEnemies) {
        return rc.canMove(direction)
                && !RobotUtil.anyCanAttack(nearbyEnemies, next);
    }

    protected void clearRubble() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        for (int i = 0; i < directions.length; i++) {
            if (tryClearRubble(directions[i])) {
                return;
            }
        }
    }

    public void tryDigMove(Direction direction) throws GameActionException {
        if (rc.canMove(direction)) {
            rc.move(direction);
            return;
        }

        if (rc.getType() != RobotType.TTM) {
            if (tryClearRubble(direction)) {
                return;
            }
        }

        Direction left = direction.rotateLeft();
        if (rc.canMove(left)) {
            rc.move(left);
            return;
        }

        Direction right = direction.rotateRight();
        if (rc.canMove(right)) {
            rc.move(right);
            return;
        }

        for (int i = 0; i < 2; i++) {
            left = left.rotateLeft();
            if (rc.canMove(left)) {
                rc.move(left);
                return;
            }

            right = right.rotateRight();
            if (rc.canMove(right)) {
                rc.move(right);
                return;
            }
        }
    }

    protected void tryMoveForward(Direction targetDirection) throws GameActionException {
        int initialDirection = getDirectionNumber(targetDirection);
        if (initialDirection < 0) {
            return;
        }

        Direction currentDirection;
        for (int i = 0; i < forwardRotations.length; i++) {
            currentDirection = directions[(initialDirection + rotations[i]) % 8];
            if (rc.canMove(currentDirection)) {
                rc.move(currentDirection);
                return;
            }
        }

        if (rc.getType() == RobotType.TTM
                || rc.getType() == RobotType.TURRET) {
            return;
        }

        tryClearRubble(targetDirection);
    }

    protected Direction getTryMoveDirection(Direction targetDirection) throws GameActionException {
        //--TODO make an overload that takes dx and dy
        //  and if it cannot move in the target direction it decides
        //  its initial rotation based off the dx and dy preference
        int initialDirection = getDirectionNumber(targetDirection);
        if (initialDirection < 0) {
            return null;
        }

        Direction currentDirection;
        for (int i = 0; i < rotations.length; i++) {
            currentDirection = directions[(initialDirection + rotations[i]) % 8];
            if (rc.canMove(currentDirection)) {
                return currentDirection;
            }
        }

        return null;
    }

    protected void tryMove(Direction targetDirection) throws GameActionException {
        //--TODO make an overload that takes dx and dy
        //  and if it cannot move in the target direction it decides
        //  its initial rotation based off the dx and dy preference
        int initialDirection = getDirectionNumber(targetDirection);
        if (initialDirection < 0) {
            return;
        }

        Direction currentDirection;
        for (int i = 0; i < rotations.length; i++) {
            currentDirection = directions[(initialDirection + rotations[i]) % 8];
            if (rc.canMove(currentDirection)) {
                rc.move(currentDirection);
                return;
            }
        }

        if (rc.getType() == RobotType.TTM
                || rc.getType() == RobotType.TURRET) {
            return;
        }

        tryClearRubble(targetDirection);
    }

    private boolean tryClearRubble(Direction direction) throws GameActionException {
        if (rc.getType() == RobotType.TTM
                || rc.getType() == RobotType.ARCHON) {
            return false;
        }

        MapLocation nextLocation = rc.getLocation().add(direction);

        double rubble = rc.senseRubble(nextLocation);

        if (rubble <= MAX_RUBBLE_CAN_IGNORE) {
            return false;
        }

        rc.clearRubble(direction);
        return true;
    }

    protected boolean tryBuild(RobotType robotType) throws GameActionException {
        if (!rc.isCoreReady()) {
            return false;
        }

        if (rc.getTeamParts() < robotType.partCost) {
            return false;
        }

        //--Build robot in some random direction
        for (int i = 0; i < 8; i++) {
            if (rc.canBuild(directions[i], robotType)) {
                rc.build(directions[i], robotType);
                return true;
            }
        }

        Message spreadMessage = MessageBuilder.buildSpreadMessage();
        rc.broadcastMessageSignal(spreadMessage.getFirst(), spreadMessage.getSecond(), 8);

        return false;
    }

    protected boolean tryBuild(RobotType robotType, Direction initialDirection) throws GameActionException {
        if (!rc.isCoreReady()) {
            return false;
        }

        if (rc.getTeamParts() < robotType.partCost) {
            return false;
        }

        int startingPosition = getDirectionNumber(initialDirection);
        if (startingPosition < 0) {
            startingPosition = 0;
        }

        int[] d = {0, 1, 7, 2, 6, 3, 5, 4};
        for (int i = 0; i < 8; i++) {
            Direction direction = directions[(startingPosition + d[i]) % 8];
            if (rc.canBuild(direction, robotType)) {
                rc.build(direction, robotType);
                return true;
            }
        }

        return false;
    }

    protected RobotInfo findAttackableRobot(RobotInfo[] robots) {
        for (RobotInfo r : robots) {
            if (rc.canAttackLocation(r.location)) {
                return r;
            }
        }

        return null;
    }

    protected Direction getRandomDirection() {
        return directions[rand.nextInt(8)];
    }

    protected void setIndicatorString(int i, String s) {
        if (Config.DEBUG) {
            if (debugString[i].length() == 0) {
                debugString[i].append(rc.getRoundNum() + " |");
            }

            debugString[i].append(s + " -");
            rc.setIndicatorString(i, debugString[i].toString());
        }
    }

    protected void setBytecodeIndicator(int i, String s) {
        if (Config.BYTECODE_DEBUG) {
            if (debugString[i].length() == 0) {
                debugString[i].append(rc.getRoundNum() + " |");
            }

            debugString[i].append(s + " " + Clock.getBytecodeNum() + " -- ");
            rc.setIndicatorString(i, debugString[i].toString());
        }
    }

    protected int getDirectionNumber(Direction direction) {
        for (int i = 0; i < 8; i++) {
            if (directions[i] == direction) {
                return i;
            }
        }

        return -1;
    }

    protected void sendMessage(Message message, int radius) throws GameActionException {
        rc.broadcastMessageSignal(message.getFirst(), message.getSecond(), radius);
    }

    protected int getRoundsTillNextSpawn(int currentRound) {
        int[] schedule = rc.getZombieSpawnSchedule().getRounds();
        for (int i = 0; i < schedule.length; i++) {
            if (schedule[i] < currentRound) {
                continue;
            }

            return schedule[i] - currentRound;
        }

        return Integer.MAX_VALUE;
    }

    protected int[][] getMessagesOfType(Signal[] signals, MessageType messageType) {
        int signalCount = signals.length;
        int messageCount = 0;
        for (int i = 0; i < signalCount; i++) {
            Signal signal = signals[i];
            if (signal.getTeam() == team) {
                int[] message = signal.getMessage();
                if (message == null) {
                    continue;
                }

                if (MessageParser.matchesType(message, messageType)) {
                    messageCount++;
                }
            }
        }

        int[][] messages = new int[messageCount][2];
        int index = 0;
        for (int i = 0; i < signalCount; i++) {
            Signal signal = signals[i];
            if (signal.getTeam() == team) {
                int[] message = signal.getMessage();
                if (message == null) {
                    continue;
                }

                if (MessageParser.matchesType(message, messageType)) {
                    messages[index][0] = message[0];
                    messages[index][1] = message[1];
                    index++;
                }
            }
        }

        return messages;
    }

    protected int[] getFirstMessageOfType(Signal[] signals, MessageType messageType) {
        int count = signals.length;
        for (int i = 0; i < count; i++) {
            Signal signal = signals[i];
            if (signal.getTeam() == team) {
                int[] message = signal.getMessage();
                if (message == null) {
                    continue;
                }

                if (MessageParser.matchesType(message, messageType)) {
                    return message;
                }
            }
        }

        return null;
    }

    protected void getTurretBroadcasts(Signal[] roundSignals) {
        enemyTurretCount = 0;
        int[][] messages = getMessagesOfType(roundSignals, MessageType.ENEMY_TURRET);
        for (int i = 0; i < messages.length; i++) {
            if (messages[i] == null) {
                break;
            }

            enemyTurrets[i] = MessageParser.getRobotData(messages[i]);
            enemyTurretCount++;
            if (enemyTurretCount == Config.MAX_ENEMY_TURRETS) {
                break;
            }
        }
    }

    protected void getTurretBroadcastsLocationOnly(Signal[] roundSignals) {
        enemyTurretCount = 0;
        int[][] messages = getMessagesOfType(roundSignals, MessageType.ENEMY_TURRET);
        for (int i = 0; i < messages.length; i++) {
            if (messages[i] == null) {
                break;
            }

            enemyTurretCount++;
            enemyTurretLocations[i] = MessageParser.getLocation(messages[i]);
            if (enemyTurretCount == Config.MAX_ENEMY_TURRETS) {
                break;
            }
        }
    }

    static final int MAX_ENEMIES_IN_DIRECTION = 5;
    static final int MAX_DISTANCE_TO_RUBBLE = 35;
    static final int MAX_DISTANCE_TO_OFF_MAP = 35;

    protected Direction safestDirectionTooRunTo(RobotInfo[] nearbyEnemies, RobotInfo[] nearbyZombies) throws GameActionException {
        Direction directionAwayFromEnemies = DirectionUtil.getDirectionAwayFrom(nearbyEnemies, nearbyZombies, currentLocation);

        setLocationsOfEnemies(nearbyEnemies);
        setLocationsOfEnemies(nearbyZombies);

        Direction safestDirection = Direction.EAST;
        double safestDirectionScore = 0;

        for (int i = 0; i < directions.length; i++) {
            Direction direction = directions[i];

            int enemyCountInCurrentDirection = 0;
            int rubbleDistanceInCurrentDirection = MAX_DISTANCE_TO_RUBBLE;
            int offMapDistanceInCurrentDirection = MAX_DISTANCE_TO_OFF_MAP;

            for (int j = 1; j <= sensorRadius; j++) {
                int x = j * direction.dx + currentLocation.x;
                int y = j * direction.dy + currentLocation.y;
                MapLocation location = new MapLocation(x, y);

                if (rc.canSenseLocation(location) && !rc.onTheMap(location)) {
                    offMapDistanceInCurrentDirection = currentLocation.distanceSquaredTo(location);
                    break;
                }

                if (rc.senseRubble(location) > 100) {
                    rubbleDistanceInCurrentDirection = currentLocation.distanceSquaredTo(location);
                    break;
                }

                int normalizedX = j * direction.dx + sensorRadius;
                int normalizedY = j * direction.dy + sensorRadius;

                if (locationLastFilled[normalizedX][normalizedY] == roundNumber) {
                    enemyCountInCurrentDirection++;
                }
            }

            double currentDirectionScore = (1.0 - (double) enemyCountInCurrentDirection / (double) MAX_ENEMIES_IN_DIRECTION)
                    * ((double) offMapDistanceInCurrentDirection / (double) MAX_DISTANCE_TO_OFF_MAP)
                    * ((double) rubbleDistanceInCurrentDirection / (double) MAX_DISTANCE_TO_RUBBLE)
                    * (direction.equals(directionAwayFromEnemies) ? 1.0 : 0.8)
                    * (direction.equals(previousRoundsSafestDirection) ? 1.0 : 0.9);

            if (currentDirectionScore > safestDirectionScore) {
                safestDirectionScore = currentDirectionScore;
                safestDirection = direction;
            }
        }

        return previousRoundsSafestDirection = safestDirection;
    }

    private void setLocationsOfEnemies(RobotInfo[] locationsOfEnemies) {
        for (RobotInfo zombie : locationsOfEnemies) {
            int xLocation = zombie.location.x - currentLocation.x + sensorRadius;
            int yLocation = zombie.location.y - currentLocation. y+ sensorRadius;

            if (xLocation >= 2 * sensorRadius + 1
                    || xLocation < 0
                    || yLocation >= 2 * sensorRadius + 1
                    || yLocation < 0) {
                continue;
            }

            locationsOfNearbyEnemies[xLocation][yLocation] = zombie;
            locationLastFilled[xLocation][yLocation] = roundNumber;
        }
    }
}
