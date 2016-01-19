package melody;

import battlecode.common.*;
import melody.message.*;
import melody.util.*;

public class Scout extends Robot {
    private static final int ROUNDS_TO_REVERSE = 4;
    private final int ZOMBIE_BROADCAST_RADIUS = senseRadius * 3;
    private final EventMemory eventMemory;
    private Direction exploreDirection;
    private final int LOOKAHEAD_LENGTH = 5;
    private RobotInfo[] nearbyZombies;
    private int ignoreEnemiesRound;
    private RobotInfo[] nearbyEnemies;
    private Signal[] roundSignals;
    private boolean zombiesDead;
    private RobotInfo lastEnemy;
    private RobotInfo lastZombieAddedToMessageStore = null;
    private RobotInfo[] nearbyFriendlies;
    private RobotInfo myPair;

    private RobotQueueNoDuplicates zombieDens;
    private BoundedQueue<Integer> destroyedDens;

    private boolean[] denDestroyed = new boolean[32001];

    public Scout(RobotController rc) {
        super(rc);
        eventMemory = new EventMemory(rc.getRoundNum());

        zombieDens = new RobotQueueNoDuplicates(Config.MAX_DENS);
        destroyedDens = new BoundedQueue<Integer>(Config.MAX_DENS);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        senseRobots();
        updateConnectionWithPair();
        getPairIfUnpaired();
        if (myPair == null) {
            setIndicatorString(0, "zombies dead " + zombiesDead);
            discoverDestroyedDens();
            readDenMessages();
            addNearbyDensToDenQueue();
            broadcastZombies();
            broadcastDensAndDestroyedDens();
            broadcastEnemy();
            moveAwayFromZombies();
            broadcastAnnouncements();
            explore();
        }
        else {
            moveTowardMyPair();
            broadcastTargets();
            int nearbyTTMs = RobotUtil.getCountOfType(nearbyFriendlies, RobotType.TTM);
            int nearbyTurrets = RobotUtil.getCountOfType(nearbyFriendlies, RobotType.TURRET);
            int desiredFriendlies = (nearbyTTMs + nearbyTurrets) * 6;
            if (nearbyFriendlies.length < desiredFriendlies) {
                rc.broadcastSignal(senseRadius);
            }
        }
    }

    private void readDenMessages() {
        int maxDenMessages = 20;
        MessageParser[] parsers = getParsersForMessagesOfType(roundSignals, MessageType.ZOMBIE, maxDenMessages);
        for (int i = 0; i < maxDenMessages; i++) {
            if (parsers[i] == null) {
                break;
            }

            RobotData zombie = parsers[i].getRobotData();
            if (zombie.type == RobotType.ZOMBIEDEN
                    && !denDestroyed[zombie.id]) {
                zombieDens.add(zombie);
            }
        }
    }

    private void discoverDestroyedDens() {
        checkLocationWeCanSense();
        checkBroadcastsForDestroyedDens();
    }

    private void checkBroadcastsForDestroyedDens() {
        int maxParsers = 10;
        MessageParser[] parsers = getParsersForMessagesOfType(roundSignals, MessageType.DESTROYED_DENS, maxParsers);
        for (int i = 0; i < maxParsers; i++) {
            if (parsers[i] == null) {
                break;
            }

            DestroyedDenData densFromBroadcast = parsers[i].getDestroyedDens();
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
        int count = zombieDens.getSize();
        for (int i = 0; i < count; i++) {
            RobotData den = zombieDens.remove();
            if (rc.canSenseLocation(den.location)
                    && !rc.canSenseRobot(den.id)) {
                denDestroyed[den.id] = true;
                destroyedDens.add(den.id);
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

        setIndicatorString(1, "broadcasting destroyed: " + denData);
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
        setIndicatorString(1, "broadcasting den: " + den);
        Message message = MessageBuilder.buildZombieMessage(den, roundNumber);
        rc.broadcastMessageSignal(message.getFirst(), message.getSecond(), senseRadius * 2);
    }

    private void broadcastTargets() throws GameActionException {
        if (nearbyEnemies.length == 0) {
            return;
        }

        RobotInfo[] highValueTargets = RobotUtil.getRobotsOfType(nearbyEnemies, RobotType.TURRET, RobotType.ARCHON, RobotType.SCOUT);
        if (highValueTargets != null
                && highValueTargets.length != 0) {
            RobotInfo closest = RobotUtil.getClosestRobotToLocation(highValueTargets, currentLocation);
            Message target = MessageBuilder.buildTargetMessage(closest);
            rc.broadcastMessageSignal(target.getFirst(), target.getSecond(), 2);
        }
        else {
            RobotInfo closest = RobotUtil.getClosestRobotToLocation(nearbyEnemies, currentLocation);
            Message target = MessageBuilder.buildTargetMessage(closest);
            rc.broadcastMessageSignal(target.getFirst(), target.getSecond(), 2);
        }
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

    private void broadcastEnemy() throws GameActionException {
        if (!zombiesDead
                || lastEnemy == null) {
            return;
        }

        Message enemyMessage = MessageBuilder.buildEnemyMessage(lastEnemy, roundNumber);
        rc.broadcastMessageSignal(enemyMessage.getFirst(), enemyMessage.getSecond(), senseRadius * 4);
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
            RobotInfo closest = RobotUtil.getClosestRobotToLocation(nearbyEnemies, currentLocation);
            lastEnemy = closest;
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

        tryMove(DirectionUtil.getDirectionAwayFrom(nearbyZombies, nearbyEnemies, currentLocation));
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
