package jeremy;

import battlecode.common.*;
import jeremy.message.*;
import jeremy.util.DirectionUtil;
import jeremy.util.Event;
import jeremy.util.EventMemory;
import jeremy.util.RobotUtil;

public class Scout extends Robot {
    private static final int ROUNDS_TO_REVERSE = 4;
    private final int ZOMBIE_BROADCAST_RADIUS = senseRadius * 4;
    private final EventMemory eventMemory;
    private Direction exploreDirection;
    private final int LOOKAHEAD_LENGTH = 5;
    private RobotInfo[] nearbyZombies;
    private int ignoreEnemiesRound;
    private MessageStore messageStore;
    private RobotInfo[] nearbyEnemies;
    private Signal[] roundSignals;
    private boolean zombiesDead;

    public Scout(RobotController rc) {
        super(rc);
        messageStore = new MessageStore(10);
        eventMemory = new EventMemory(rc.getRoundNum());
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        senseRobots();
        makeDenMessages();
        broadcastZombies();
        broadcastParts();
        doRepeatedBroadcasts();
        moveAwayFromZombies();
        broadcastAnnouncements();
        explore();
    }

    private void broadcastParts() throws GameActionException {
        if (!zombiesDead) {
            return;
        }

        setIndicatorString(0, "broadcasting parts");
        MapLocation[] parts = rc.sensePartLocations(RobotType.ARCHON.sensorRadiusSquared);
        if (parts.length > 0) {
            Message partsMessage = MessageBuilder.buildPartsMessage(currentLocation);
            rc.broadcastMessageSignal(partsMessage.getFirst(), partsMessage.getSecond(), 400);
        }
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
                            setIndicatorString(0, "received zombie dead proposal");
                        }
                        else if (parser.getAnnouncementMode() == AnnouncementMode.DENY) {
                            zombiesDeadDenied = true;
                            setIndicatorString(0, "received zombie dead denial");
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
            setIndicatorString(1, "I deny zombies are dead");
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
            setIndicatorString(1, "I propose zombies are dead");
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
            setIndicatorString(2, "I conclude zombies are dead");
            zombiesDead = true;
        }
    }

    private void makeDenMessages() {
        for (RobotInfo zombie : nearbyZombies) {
            if (zombie.type == RobotType.ZOMBIEDEN) {
                messageStore.addMessage(MessageBuilder.buildZombieMessage(zombie, roundNumber),
                        roundNumber + 100);
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

        if (nearbyZombies.length > 0) {
            eventMemory.record(Event.ZOMBIE_SPOTTED, roundNumber);
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
}
