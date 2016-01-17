package jeremy_the_second;

import battlecode.common.*;
import jeremy_the_second.message.Message;
import jeremy_the_second.message.MessageBuilder;
import jeremy_the_second.message.MessageParser;
import jeremy_the_second.util.AverageMapLocation;
import jeremy_the_second.util.DirectionUtil;

public class Archon extends Robot {

    private final int ZOMBIE_BROADCAST_RADIUS = senseRadius; // spawned robots are close

    private RobotType[] buildQueue = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER};

    private Signal[] roundSignals;
    private RobotData zombieToAttack;
    private RobotData zombieDen;
    private int buildQueuePosition = 0;
    private RobotInfo[] nearbyZombies;
    private AverageMapLocation previousZombieLocation = new AverageMapLocation(5);

    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        readBroadcasts();
        broadcastZombies();
        senseZombies();
        moveAwayFromZombies();
        buildRobots();
        moveIfSafe();
        repairRobots();
    }

    private void broadcastZombies() throws GameActionException {
        if (zombieDen != null) {
            rc.setIndicatorString(0, "broadcasting zombie den to attack.");
            Message zombieDenMessage = MessageBuilder.buildRobotMessage(zombieDen.health,
                    zombieDen.id, zombieDen.type, zombieDen.location, MessageType.ZOMBIE);
            rc.broadcastMessageSignal(zombieDenMessage.getFirst(),
                    zombieDenMessage.getSecond(), ZOMBIE_BROADCAST_RADIUS);
        }
        if (zombieToAttack != null) {
            rc.setIndicatorString(0, "broadcasting zombie to attack.");
            Message zombieToAttackMessage = MessageBuilder.buildRobotMessage(zombieToAttack.health,
                    zombieToAttack.id, zombieToAttack.type, zombieToAttack.location, MessageType.ZOMBIE);
            rc.broadcastMessageSignal(zombieToAttackMessage.getFirst(),
                    zombieToAttackMessage.getSecond(), ZOMBIE_BROADCAST_RADIUS);
        }
    }

    private void readBroadcasts() {
        roundSignals = rc.emptySignalQueue();
        zombieToAttack = getZombieToAttack();
        if (zombieDen == null) {
            zombieDen = getZombieDen();
        }
    }

    private RobotData getZombieToAttack() {
        for (Signal s : roundSignals) {
            if (s.getTeam() != team) continue;

            int[] message = s.getMessage();
            if (message == null) continue;

            MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
            setIndicatorString(0, " " + parser.getMessageType());
            if (parser.getMessageType() == MessageType.ZOMBIE) {
                RobotData robotData = parser.getRobotData();
                if (robotData.type != RobotType.ZOMBIEDEN) {
                    return robotData;
                }
            }
        }

        return null;
    }

    private RobotData getZombieDen() {
        for (Signal s : roundSignals) {
            if (s.getTeam() != team) continue;

            int[] message = s.getMessage();
            if (message == null) continue;

            MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
            setIndicatorString(0, " " + parser.getMessageType());
            if (parser.getMessageType() == MessageType.ZOMBIE) {
                RobotData robotData = parser.getRobotData();
                if (robotData.type == RobotType.ZOMBIEDEN) {
                    return robotData;
                }
            }
        }

        return null;
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

        MapLocation towardZombies = previousZombieLocation.getAverage();
        if (towardZombies == null
                || towardZombies.equals(currentLocation)) {
            tryMove(getRandomDirection());
        } else {
            setIndicatorString(0, "move toward zombies: " + towardZombies);
            tryMoveToward(towardZombies);
        }
    }

    private void buildRobots() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (tryBuild(buildQueue[buildQueuePosition % buildQueue.length])) {
            buildQueuePosition++;
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
}
