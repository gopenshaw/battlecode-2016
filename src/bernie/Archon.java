package bernie;

import battlecode.common.*;
import bernie.util.DirectionUtil;
import bernie.util.RobotUtil;

public class Archon extends Robot {
    RobotType[] buildQueue = {RobotType.SCOUT};
    int buildQueuePosition = 0;

    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    public void doTurn() throws GameActionException {
        repairRobots();

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
