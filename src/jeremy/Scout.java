package jeremy;

import battlecode.common.*;
import jeremy.message.Message;
import jeremy.message.MessageBuilder;
import jeremy.message.MessageParser;
import jeremy.message.MessageStore;
import jeremy.util.DirectionUtil;
import jeremy.util.RobotUtil;

public class Scout extends Robot {
    private static final int ROUNDS_TO_REVERSE = 4;
    private final int ZOMBIE_BROADCAST_RADIUS = senseRadius * 4;
    private Direction exploreDirection;
    private final int LOOKAHEAD_LENGTH = 5;
    private RobotInfo[] nearbyZombies;
    private int ignoreEnemiesRound;
    private MessageStore messageStore;
    private boolean broadcastKillEnemy;
    private boolean receivedKillEnemyBroadcast;
    private RobotInfo recentEnemy;
    private RobotInfo[] nearbyEnemies;
    private int seenZombieRound;

    public Scout(RobotController rc) {
        super(rc);
        messageStore = new MessageStore(10);
    }

    @Override
    protected void doTurn() throws GameActionException {
        senseRobots();
        makeDenMessages();
        getBroadcasts();
        broadcastZombies();
        doRepeatedBroadcasts();
        moveAwayFromZombies();
        updateSeenEnemy();
        broadcastKillEnemy();
        explore();
    }

    private void updateSeenEnemy() {
        if (nearbyEnemies.length > 0) {
            recentEnemy = nearbyEnemies[0];
        }
    }

    private void broadcastKillEnemy() throws GameActionException {
        if (broadcastKillEnemy
                || receivedKillEnemyBroadcast
                || recentEnemy == null
                || seenZombieRecently()) {
            return;
        }

        Message message = MessageBuilder.buildEnemyMessage(recentEnemy, roundNumber);
        rc.broadcastMessageSignal(message.getFirst(), message.getSecond(), 80 * 80 * 2);
        broadcastKillEnemy = true;
    }

    private boolean seenZombieRecently() {
        int recent = 200;
        return seenZombieRound + recent > roundNumber;
    }

    private void makeDenMessages() {
        for (RobotInfo zombie : nearbyZombies) {
            if (zombie.type == RobotType.ZOMBIEDEN) {
                messageStore.addMessage(MessageBuilder.buildZombieMessage(zombie, roundNumber),
                        roundNumber + 100);
                setIndicatorString(1, "adding " + zombie.location + " " + (roundNumber + 100) + " to message store");
            }
        }
    }

    private void doRepeatedBroadcasts() throws GameActionException {
        if (rc.getMessageSignalCount() > 0) {
            return;
        }

        Message message = messageStore.getNextMessage(roundNumber);
        if (message != null) {
            MessageParser parser = new MessageParser(message.getFirst(), message.getSecond(), currentLocation);
            setIndicatorString(2, "broadcasting " + parser.getMessageType() + " "
                    + parser.getRobotData().location + " from message store");
            rc.broadcastMessageSignal(message.getFirst(), message.getSecond(), senseRadius * 4);
        }
    }

    private void broadcastZombies() throws GameActionException {
        if (nearbyZombies.length == 0) {
            return;
        }

        RobotInfo closestZombie = RobotUtil.getClosestRobotToLocation(nearbyZombies, currentLocation);
        Message zombieMessage = MessageBuilder.buildZombieMessage(closestZombie, roundNumber);
        rc.broadcastMessageSignal(zombieMessage.getFirst(), zombieMessage.getSecond(), ZOMBIE_BROADCAST_RADIUS);
    }

    private void senseRobots() {
        nearbyZombies = senseNearbyZombies();
        nearbyEnemies = senseNearbyEnemies();

        if (nearbyZombies.length > 0) {
            setIndicatorString(0, "seen zombie");
            seenZombieRound = roundNumber;
        }
    }

    private void moveAwayFromZombies() throws GameActionException {
        if (nearbyZombies.length == 0
                || !rc.isCoreReady()) {
            return;
        }

        tryMove(DirectionUtil.getDirectionAwayFrom(nearbyZombies, currentLocation));
    }

    private void explore() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (exploreDirection == null) {
            exploreDirection = getExploreDirection(null);
        }

        if (nearbyEnemies.length > 0
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

    public void getBroadcasts() {
        Signal[] roundSignals = rc.emptySignalQueue();
        for (Signal s : roundSignals) {
            if (s.getTeam() == team) {
                int[] message = s.getMessage();
                if (message == null) continue;
                MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                if (parser.getMessageType() == MessageType.ENEMY) {
                    receivedKillEnemyBroadcast = true;
                }
                else if (parser.getMessageType() == MessageType.ZOMBIE) {
                    seenZombieRound = roundNumber;
                }
            }
        }
    }
}
