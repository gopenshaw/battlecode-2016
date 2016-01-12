package ella;

import battlecode.common.*;
import ella.util.AverageMapLocation;

public class Archon extends Robot {
    private int id;
    private MapLocation baseLocation;

    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        getIdAndBaseLocation();
        if (roundNumber < 2) return;

        repairRobots();
        moveTowardBase();
        tryBuild(RobotType.TURRET);
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

    private void moveTowardBase() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (currentLocation.distanceSquaredTo(baseLocation) > 2) {
            tryMoveToward(baseLocation);
        }
    }

    private void getIdAndBaseLocation() throws GameActionException {
        int radiusSquared = 2000;
        if (roundNumber == 0) {
            rc.broadcastSignal(radiusSquared);

            Signal[] signals = rc.emptySignalQueue();
            for (Signal s : signals) {
                if (s.getTeam() == team) {
                    id++;
                }
            }

            setIndicatorString(1, "id " + id);
        }

        if (roundNumber == 1) {
            rc.broadcastSignal(radiusSquared);

            AverageMapLocation averageMapLocation = new AverageMapLocation(GameConstants.NUMBER_OF_ARCHONS_MAX);
            averageMapLocation.add(currentLocation);
            Signal[] signals = rc.emptySignalQueue();
            for (Signal s : signals) {
                if (s.getTeam() == team) {
                    averageMapLocation.add(s.getLocation());
                }
            }

            baseLocation = averageMapLocation.getAverage();
        }
    }
}
