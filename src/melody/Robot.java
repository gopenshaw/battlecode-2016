package melody;

import battlecode.common.*;
import melody.message.Message;
import melody.message.MessageParser;
import melody.util.RobotUtil;

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

    protected MapLocation currentLocation;
    protected int roundNumber;

    private StringBuilder[] debugString;

    private final int MAX_RUBBLE_CAN_IGNORE = 50;

    protected final Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
        Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    private int maxBytecode;
    private int maxBytecodeRound;

    protected RobotData[] enemyTurrets = new RobotData[Config.MAX_ENEMY_TURRETS];

    public Robot(RobotController rc) {
        this.rc = rc;
        rand = new Random(rc.getID());
        team = rc.getTeam();
        enemy = team.opponent();
        currentLocation = rc.getLocation();

        id = rc.getID();
        type = rc.getType();
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

    protected void tryMoveOnto(MapLocation location) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        if (currentLocation.equals(location)) {
            return;
        }

        if (currentLocation.isAdjacentTo(location)
                && rc.senseRubble(location) >= 100) {
            rc.clearRubble(currentLocation.directionTo(location));
            return;
        }

        Direction moveDirection = currentLocation.directionTo(location);
        if (type == RobotType.ARCHON
                || type == RobotType.TTM) {
            tryMove(moveDirection);
        }
        else {
            tryMoveDig(moveDirection);
        }
    }

    protected void tryMoveToward(MapLocation location) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        if (currentLocation.equals(location)) {
            return;
        }

        Direction moveDirection = currentLocation.directionTo(location);
        if (type == RobotType.ARCHON
                || type == RobotType.TTM) {
            tryMove(moveDirection);
        }
        else {
            tryMoveDig(moveDirection);
        }
    }

    private void tryMoveDig(Direction targetDirection) throws GameActionException {
        int[] moveSequence1 = {0, 7, 1};
        int[] digSequence1 = {0, 7, 1};
        int[] moveSequence2 = {6, 2, 5, 3};
        int initialDirection = getDirectionNumber(targetDirection);
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

    protected boolean trySafeMoveToward(MapLocation location, RobotData[] nearbyEnemies) throws GameActionException {
        Direction direction = currentLocation.directionTo(location);
        return trySafeMove(direction, nearbyEnemies);
    }

    protected boolean trySafeMove(Direction direction,
                                  RobotData[] nearbyEnemies) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        MapLocation next = currentLocation.add(direction);
        if (canMoveSafely(direction, next, nearbyEnemies)) {
            rc.move(direction);
            return true;
        }

        Direction left = direction.rotateLeft();
        next = currentLocation.add(left);
        if (canMoveSafely(left, next, nearbyEnemies)) {
            rc.move(left);
            return true;
        }

        Direction right = direction.rotateRight();
        next = currentLocation.add(right);
        if (canMoveSafely(right, next, nearbyEnemies)) {
            rc.move(right);
            return true;
        }

        for (int i = 0; i < 2; i++) {
            left = left.rotateLeft();
            next = currentLocation.add(left);
            if (canMoveSafely(left, next, nearbyEnemies)) {
                rc.move(left);
                return true;
            }

            right = right.rotateRight();
            next = currentLocation.add(right);
            if (canMoveSafely(right, next, nearbyEnemies)) {
                rc.move(right);
                return true;
            }
        }

        return tryClearRubble(direction);
    }

    private boolean canMoveSafely(Direction direction, MapLocation next, RobotData[] nearbyEnemies) {
        return rc.canMove(direction)
                && !RobotUtil.anyCanAttack(nearbyEnemies, next);
    }

    protected boolean trySafeMove(Direction direction,
                               RobotInfo[] nearbyEnemies,
                               RobotInfo[] nearbyZombies) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        MapLocation next = currentLocation.add(direction);
        if (canMoveSafely(direction, next, nearbyEnemies, nearbyZombies)) {
            rc.move(direction);
            return true;
        }

        Direction left = direction.rotateLeft();
        next = currentLocation.add(left);
        if (canMoveSafely(left, next, nearbyEnemies, nearbyZombies)) {
            rc.move(left);
            return true;
        }

        Direction right = direction.rotateRight();
        next = currentLocation.add(right);
        if (canMoveSafely(right, next, nearbyEnemies, nearbyZombies)) {
            rc.move(right);
            return true;
        }

        for (int i = 0; i < 2; i++) {
            left = left.rotateLeft();
            next = currentLocation.add(left);
            if (canMoveSafely(left, next, nearbyEnemies, nearbyZombies)) {
                rc.move(left);
                return true;
            }

            right = right.rotateRight();
            next = currentLocation.add(right);
            if (canMoveSafely(right, next, nearbyEnemies, nearbyZombies)) {
                rc.move(right);
                return true;
            }
        }

        return false;
    }

    private boolean canMoveSafely(Direction direction, MapLocation next, RobotInfo[] nearbyEnemies, RobotInfo[] nearbyZombies) {
        return rc.canMove(direction)
                && !RobotUtil.anyCanAttack(nearbyEnemies, next)
                && !RobotUtil.anyCanAttack(nearbyZombies, next);
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

    protected void tryMove(Direction targetDirection) throws GameActionException {
        int[] rotations = {0, 7, 1, 6, 2, 5, 3};
        int initialDirection = getDirectionNumber(targetDirection);
        Direction currentDirection;
        for (int i = 0; i < rotations.length; i++) {
            currentDirection = directions[(initialDirection + rotations[i]) % 8];
            if (rc.canMove(currentDirection)) {
                rc.move(currentDirection);
                return;
            }
        }

        if (rc.getType() != RobotType.TTM) {
            tryClearRubble(targetDirection);
        }
    }

    private boolean tryClearRubble(Direction direction) throws GameActionException {
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

    protected MessageParser[] getParsersForMessagesOfType(Signal[] signals, MessageType messageType, int max) {
        MessageParser[] parsers = new MessageParser[max];
        int signalCount = signals.length;
        int signalsOfType = 0;
        for (int i = 0; i < signalCount; i++) {
            Signal signal = signals[i];
            if (signal.getTeam() == team) {
                int[] message = signal.getMessage();
                if (message == null) {
                    continue;
                }

                MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                if (parser.getMessageType() == messageType) {
                    parsers[signalsOfType] = parser;
                    signalsOfType++;
                }

                if (signalsOfType == max) {
                    return parsers;
                }
            }
        }

        return parsers;
    }

    protected MessageParser getParserForFirstMessageOfType(Signal[] signals, MessageType messageType) {
        int count = signals.length;
        for (int i = 0; i < count; i++) {
            Signal signal = signals[i];
            if (signal.getTeam() == team) {
                int[] message = signal.getMessage();
                if (message == null) {
                    continue;
                }

                MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                if (parser.getMessageType() == messageType) {
                    return parser;
                }
            }
        }

        return null;
    }

    protected void getTurretBroadcasts(Signal[] roundSignals) {
        int maxTurrets = Config.MAX_ENEMY_TURRETS;
        int count = 0;
        MessageParser[] parsers = getParsersForMessagesOfType(roundSignals, MessageType.ENEMY_TURRET, maxTurrets);
        for (int i = 0; i < maxTurrets; i++) {
            if (parsers[i] == null) {
                break;
            }

            enemyTurrets[i] = parsers[i].getRobotData();
            count++;
        }

        setIndicatorString(0, "enemy turret count: " + count);
    }
}
