package jeremy;

import battlecode.common.*;
import jeremy.message.AnnouncementMode;
import jeremy.message.AnnouncementSubject;
import jeremy.message.MessageParser;
import jeremy.nav.Bug;
import jeremy.util.*;

public class Archon extends Robot {

    private RobotType[] buildQueue = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
        RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
        RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER };

    private int buildQueuePosition = 0;
    private RobotInfo[] nearbyZombies;
    private AverageMapLocation previousZombieLocation = new AverageMapLocation(5);
    private Signal[] roundSignals;
    private MapLocation previousPartLocation;
    private EventMemory eventMemory;
    private boolean zombiesDead;
    private MapLocation enemyLocation;

    public Archon(RobotController rc) {
        super(rc);
        Bug.init(rc);
        eventMemory = new EventMemory(0);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        readAnnouncements();
        readSignals();
        senseZombies();
        moveAwayFromZombies();
        moveAwayFromEnemies();
        buildRobots();
        moveIfSafe();
        getParts();
        repairRobots();
    }

    private void readSignals() {
        for (Signal s : roundSignals) {
            if (s.getTeam() == team) {
                int[] message = s.getMessage();
                if (message == null) continue;
                MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                if (parser.getMessageType() == MessageType.ENEMY) {
                    enemyLocation = parser.getRobotData().location;
                    break;
                }
            }
        }
    }

    private void moveAwayFromEnemies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] nearbyEnemies = senseNearbyEnemies();
        if (nearbyEnemies.length > 0) {
            tryMove(DirectionUtil.getDirectionAwayFrom(nearbyEnemies, currentLocation));
        }
    }

    private void readAnnouncements() throws GameActionException {
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

    private void senseZombies() {
        nearbyZombies = senseNearbyZombies();
        for (RobotInfo zombie : nearbyZombies) {
            previousZombieLocation.add(zombie.location);
        }
    }

    private void moveIfSafe() throws GameActionException {
        if (nearbyZombies.length > 0
                || !rc.isCoreReady()) {
            return;
        }

        setIndicatorString(0, "enemy location is " + enemyLocation);

        RobotInfo[] nearbyNeutrals = senseNearbyNeutrals();
        if (nearbyNeutrals.length > 0) {
            RobotInfo closestNeutral = RobotUtil.getClosestRobotToLocation(nearbyNeutrals, currentLocation);
            Bug.setDestination(closestNeutral.location);
            if (currentLocation.isAdjacentTo(closestNeutral.location)) {
                rc.activate(closestNeutral.location);
            }
            else {
                tryMove(Bug.getDirection(currentLocation));
            }

            return;
        }

        if (zombiesDead
                && enemyLocation != null
                && currentLocation.distanceSquaredTo(enemyLocation) > 15 * 15) {
            tryMoveToward(enemyLocation);
            return;
        }

        MapLocation towardZombies = previousZombieLocation.getAverage();
        if (towardZombies == null
                || towardZombies.equals(currentLocation)) {
            tryMove(getRandomDirection());
        }
        else {
            setIndicatorString(0, "move toward zombies: " + towardZombies);
            tryMoveToward(towardZombies);
        }
    }

    private void buildRobots() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (zombiesDead) {
            tryBuild(RobotType.VIPER);
        }
        else {
            if (tryBuild(buildQueue[buildQueuePosition % buildQueue.length])) {
                buildQueuePosition++;
            }
        }
    }

    private void moveAwayFromZombies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (nearbyZombies.length > 0) {
            tryMove(DirectionUtil.getDirectionAwayFrom(nearbyZombies, currentLocation));
        }
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
        if (!rc.isCoreReady()) {
            return;
        }

        if (previousPartLocation != null
                && currentLocation.equals(previousPartLocation)) {
            previousPartLocation = null;
        }

        MapLocation[] partsLocations = rc.sensePartLocations(senseRadius);
        if (partsLocations.length > 0) {
            setIndicatorString(0, "i sense parts");
            MapLocation closest = LocationUtil.findClosestLocation(partsLocations, currentLocation);
            tryMoveOnto(closest);
            return;
        }

        if (previousPartLocation == null) {
            for (Signal s : roundSignals) {
                if (s.getTeam() == team) {
                    int[] message = s.getMessage();
                    if (message == null) continue;
                    MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                    if (parser.getMessageType() == MessageType.PARTS) {
                        previousPartLocation = parser.getPartsData().location;
                        setIndicatorString(2, "received part signal: " + previousPartLocation);
                    }
                }
            }
        }

        if (previousPartLocation != null) {
            setIndicatorString(1, "moving toward " + previousPartLocation);
            tryMoveOnto(previousPartLocation);
        }
    }
}
