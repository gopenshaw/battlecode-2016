package bernie;

import battlecode.common.*;
import bernie.message.MessageBuilder;
import bernie.message.MessageParser;
import bernie.util.AverageMapLocation;
import bernie.util.DirectionUtil;
import bernie.util.RobotUtil;

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
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.buildIdMessage(rc.getHealth(), rc.getID(), RobotType.ARCHON, currentLocation);
        rc.broadcastMessageSignal(messageBuilder.getFirst(), messageBuilder.getSecond(), 100);
        observeSignals();

        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        RobotInfo[] nearbyEnemies = senseNearbyEnemies();
        if (RobotUtil.anyCanAttack(nearbyEnemies, currentLocation)
                || RobotUtil.anyCanAttack(nearbyZombies, currentLocation)) {
            Direction away = DirectionUtil.getDirectionAwayFrom(nearbyEnemies, nearbyZombies, rc);
            tryMove(away);
            return;
        }

        buildRobot();

        if (rc.isCoreReady()) {
            MapLocation zombieLocation = pastZombieLocation.getAverage();
            if (zombieLocation != null
                && currentLocation.distanceSquaredTo(zombieLocation) > MAX_ZOMBIE_DISTANCE) {
                tryMoveToward(zombieLocation);
                setIndicatorString(2, "moving toward " + zombieLocation);
            }
        }
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

            if (r.health < lowestHealth) {
                lowestHealth = r.health;
                robotToRepair = r;
            }
        }

        if (robotToRepair == null) {
            return;
        }

        if (robotToRepair.health < robotToRepair.type.maxHealth) {
            rc.repair(robotToRepair.location);
        }
    }
    private boolean buildRobot() throws GameActionException {
        if (buildQueuePosition < buildQueue.length) {
            if (tryBuild(buildQueue[buildQueuePosition])) {
                buildQueuePosition++;
                return true;
            }
        }
        else {
            if (tryBuild(RobotType.SOLDIER)) return true;
        }

        return false;
    }
}
