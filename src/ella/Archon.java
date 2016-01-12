package ella;

import battlecode.common.*;
import ella.util.AverageMapLocation;

public class Archon extends Robot {
    private int id;
    private MapLocation baseLocation;
    private boolean builtScout;
    private int turretCount;

    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        getIdAndBaseLocation();
        if (roundNumber < 2) return;

        countRobots();
        repairRobots();
        moveTowardBase();
        buildRobot();
        clearRubble();
    }

    private void countRobots() {
        turretCount = 0;
        RobotInfo[] nearbyFriendlies = rc.senseNearbyRobots(senseRadius, team);
        for (RobotInfo r : nearbyFriendlies) {
            if (r.type == RobotType.TURRET) {
                turretCount++;
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

    private void buildRobot() throws GameActionException {
        if (!builtScout
                && turretCount > 5 + id) {
            if (tryBuild(RobotType.SCOUT)) {
                builtScout = true;
            }
        }

        tryBuild(RobotType.TURRET);
    }

    private void clearRubble() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        for (int i = 0; i < 8; i++) {
            if (rc.senseRubble(currentLocation.add(directions[i])) >= 100) {
                rc.clearRubble(directions[i]);
                return;
            }
        }
    }
}
