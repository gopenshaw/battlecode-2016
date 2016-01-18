package jeremy;

import battlecode.common.*;
import jeremy.message.*;
import jeremy.util.*;

public class Scout extends Robot {
    private static final int ROUNDS_TO_REVERSE = 4;
    private final int ZOMBIE_BROADCAST_RADIUS = senseRadius * 4;
    private int DEN_BROADCAST_REPEAT_ROUNDS = 100;
    private final EventMemory eventMemory;
    private Direction exploreDirection;
    private final int LOOKAHEAD_LENGTH = 5;
    private RobotInfo[] nearbyZombies;
    private int ignoreEnemiesRound;
    private MessageStore messageStore;
    private RobotInfo[] nearbyEnemies;
    private Signal[] roundSignals;
    private boolean zombiesDead;
    private RobotInfo lastEnemy;
    private RobotInfo lastZombieAddedToMessageStore = null;
    private RobotInfo[] nearbyFriendlies;
    private RobotInfo myPair;

    public Scout(RobotController rc) {
        super(rc);
        messageStore = new MessageStore(10);
        eventMemory = new EventMemory(rc.getRoundNum());
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        senseRobots();
        updateConnectionWithPair();
        getPairIfUnpaired();
        if (myPair == null) {
            makeDenMessages();
            broadcastZombies();
            broadcastParts();
            broadcastEnemy();
            doRepeatedBroadcasts();
            moveAwayFromZombies();
            broadcastAnnouncements();
            explore();
        }
        else {
            moveTowardMyPair();
            broadcastTargets();
        }
    }

    private void broadcastTargets() throws GameActionException {
        RobotInfo[] highValueTargets = RobotUtil.getRobotsOfType(nearbyEnemies, RobotType.TURRET, RobotType.ARCHON);
        if (highValueTargets == null
                || highValueTargets.length == 0) {
            return;
        }

        RobotInfo closest = RobotUtil.getClosestRobotToLocation(highValueTargets, currentLocation);
        Message target = MessageBuilder.buildTargetMessage(closest);
        rc.broadcastMessageSignal(target.getFirst(), target.getSecond(), 2);
        setIndicatorString(1, "broadcasting target " + closest);
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
            setIndicatorString(2, "i have no pair");
            return;
        }

        if (!rc.canSenseRobot(myPair.ID)) {
            myPair = null;
            setIndicatorString(2, "my pair is gone :(");
            return;
        }

        myPair = rc.senseRobot(myPair.ID);
        broadcastPairMessage(myPair);
    }

    private void broadcastPairMessage(RobotInfo myPair) throws GameActionException {
        setIndicatorString(2, "broadcasting pair message for " + myPair.ID);
        Message pairMessage = MessageBuilder.buildPairingMessage(myPair);
        rc.broadcastMessageSignal(pairMessage.getFirst(), pairMessage.getSecond(), senseRadius * 2);
    }

    private void getPairIfUnpaired() throws GameActionException {
        if (myPair != null) {
            return;
        }

        RobotInfo[] turrets = RobotUtil.getRobotsOfType(nearbyFriendlies, RobotType.TURRET, RobotType.TTM);
        if (turrets == null
                || turrets.length == 0) {
            return;
        }

        RobotInfo unpairedTurret = getUnpairedTurret(turrets, roundSignals);
        if (unpairedTurret == null) {
            return;
        }

        myPair = unpairedTurret;
        broadcastPairMessage(myPair);
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
                MessageParser parser = new MessageParser(message[0], message[1], currentLocation);

                if (parser.getMessageType() == MessageType.PAIR
                        && parser.pairs(robot)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void broadcastParts() throws GameActionException {
        if (!zombiesDead
                || roundNumber % 2 == 1) {
            return;
        }

        MapLocation[] parts = rc.sensePartLocations(RobotType.ARCHON.sensorRadiusSquared);
        if (parts.length > 0) {
            Message partsMessage = MessageBuilder.buildPartsMessage(currentLocation);
            rc.broadcastMessageSignal(partsMessage.getFirst(), partsMessage.getSecond(), 400);
        }
    }

    private void broadcastEnemy() throws GameActionException {
        if (!zombiesDead
                || lastEnemy == null) {
            return;
        }

        Message enemyMessage = MessageBuilder.buildEnemyMessage(lastEnemy, roundNumber);
        messageStore.addMessage(enemyMessage, roundNumber + 200);
    }

    private void broadcastAnnouncements() throws GameActionException {
        boolean zombiesDeadProposed = false;
        boolean zombiesDeadDenied = false;
        for (Signal s : roundSignals) {
            if (s.getTeam() == team) {
                int[] message = s.getMessage();
                if (message == null) continue;
                MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                if (parser.getMessageType() == MessageType.ANNOUNCEMENT) {
                    if (parser.getAnnouncementSubject() == AnnouncementSubject.ZOMBIES_DEAD) {
                        if (parser.getAnnouncementMode() == AnnouncementMode.PROPOSE) {
                            zombiesDeadProposed = true;
                        }
                        else if (parser.getAnnouncementMode() == AnnouncementMode.DENY) {
                            zombiesDeadDenied = true;
                        }
                    }
                }
            }
        }

        if (zombiesDeadProposed
                && !zombiesDeadDenied
                && eventMemory.hasMemory(roundNumber)
                && eventMemory.happenedRecently(Event.ZOMBIE_SPOTTED, roundNumber)) {
            Message denial = MessageBuilder.buildAnnouncement(AnnouncementSubject.ZOMBIES_DEAD, AnnouncementMode.DENY);
            rc.broadcastMessageSignal(denial.getFirst(), denial.getSecond(), 80 * 80 * 2);
            eventMemory.record(Event.ZOMBIES_DEAD_DENIED, roundNumber);
        }

        if (!zombiesDead
                && !zombiesDeadProposed
                && eventMemory.hasMemory(roundNumber)
                && !eventMemory.happenedRecently(Event.ZOMBIE_SPOTTED, roundNumber)
                && !eventMemory.happenedRecently(Event.BROADCAST_ZOMBIES_DEAD, roundNumber)) {
            Message message = MessageBuilder.buildAnnouncement(AnnouncementSubject.ZOMBIES_DEAD,
                    AnnouncementMode.PROPOSE);
            rc.broadcastMessageSignal(message.getFirst(), message.getSecond(), 80 * 80 * 2);
            eventMemory.record(Event.BROADCAST_ZOMBIES_DEAD, roundNumber);
        }

        if (zombiesDeadProposed) {
            eventMemory.record(Event.ZOMBIES_DEAD_PROPOSED, roundNumber);
        }

        if (zombiesDeadDenied) {
            eventMemory.record(Event.ZOMBIES_DEAD_DENIED, roundNumber);
        }

        if (eventMemory.happedLastRound(Event.ZOMBIES_DEAD_PROPOSED, roundNumber)
                && !zombiesDeadDenied
                && !eventMemory.happedLastRound(Event.ZOMBIES_DEAD_DENIED, roundNumber)) {
            zombiesDead = true;
        }
    }

    private void makeDenMessages() {
        for (RobotInfo zombie : nearbyZombies) {
            if (zombie.type == RobotType.ZOMBIEDEN
                    && zombie != lastZombieAddedToMessageStore) {
                messageStore.addMessage(MessageBuilder.buildZombieMessage(zombie, roundNumber),
                        roundNumber + DEN_BROADCAST_REPEAT_ROUNDS);
                lastZombieAddedToMessageStore = zombie;
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
        nearbyFriendlies = senseNearbyFriendlies();

        if (nearbyZombies.length > 0) {
            eventMemory.record(Event.ZOMBIE_SPOTTED, roundNumber);
        }

        if (nearbyEnemies.length > 0) {
            lastEnemy = nearbyEnemies[0];
        }
    }

    private void moveAwayFromZombies() throws GameActionException {
        if (nearbyZombies.length == 0
                || !rc.isCoreReady()) {
            return;
        }

        if (rand.nextInt(4) == 0) {
            exploreDirection = getExploreDirection(exploreDirection);
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
}
