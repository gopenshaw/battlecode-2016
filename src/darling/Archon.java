package darling;

import battlecode.common.*;
import darling.message.MessageBuilder;
import darling.message.MessageParser;
import darling.util.AverageMapLocation;
import darling.util.DirectionUtil;
import darling.util.RobotUtil;

public class Archon extends Robot {
    RobotType[] buildQueue = {RobotType.SCOUT};
    int buildQueuePosition = 0;

    AverageMapLocation pastZombieLocation = new AverageMapLocation(4);
    private final int MAX_ZOMBIE_DISTANCE = 100;

    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    public void doTurn() throws GameActionException {
        repairRobots();
        broadcastID();
        observeSignals();
        runFromRobotsAndZombiesThatCanAttack();
        buildRobot();
        moveTowardZombies();
    }

    private void runFromRobotsAndZombiesThatCanAttack() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        RobotInfo[] nearbyEnemies = senseNearbyEnemies();
        if (RobotUtil.anyCanAttack(nearbyEnemies, currentLocation)
                || RobotUtil.anyCanAttack(nearbyZombies, currentLocation)) {
            Direction away = DirectionUtil.getDirectionAwayFrom(nearbyEnemies, nearbyZombies, rc);
            tryMove(away);
        }
    }

    private void moveTowardZombies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        MapLocation zombieLocation = pastZombieLocation.getAverage();
        if (zombieLocation != null
            && currentLocation.distanceSquaredTo(zombieLocation) > MAX_ZOMBIE_DISTANCE) {
            tryMoveToward(zombieLocation);
            setIndicatorString(2, "moving toward " + zombieLocation);
        }
    }

    private void broadcastID() throws GameActionException {
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.buildIdMessage(rc.getHealth(), rc.getID(), RobotType.ARCHON, currentLocation);
        rc.broadcastMessageSignal(messageBuilder.getFirst(), messageBuilder.getSecond(), 100);
    }

    private void observeSignals() {
        Signal[] signals = rc.emptySignalQueue();
        for (Signal s : signals) {
            if (s.getTeam() == team) {
                int[] message = s.getMessage();
                if (message == null) continue;
                MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                if (parser.getMessageType() == MessageType.ZOMBIE) {
                    pastZombieLocation.add(parser.getRobotData().location);
                }
            }
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
    private void buildRobot() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (buildQueuePosition < buildQueue.length) {
            if (tryBuild(buildQueue[buildQueuePosition])) {
                buildQueuePosition++;
            }
        }
        else {
            tryBuild(RobotType.SOLDIER);
        }
    }
}
