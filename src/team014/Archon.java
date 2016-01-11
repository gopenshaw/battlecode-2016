package team014;

import battlecode.common.*;
import team014.dataStructures.AverageMapLocation;
import team014.dataStructures.BoundedQueue;

public class Archon extends Robot {
    private int id;
    private boolean scoutBuilt;
    private MapLocation baseLocation;
    private int archonCount;

    public Archon(RobotController rc) {
        super(rc);
    }

    RobotType[] buildQueue = {};
    int buildQueuePosition = 0;

    BoundedQueue<MapLocation> partsLocations = new BoundedQueue<MapLocation>(50);
    MapLocation partsDestination = null;

    AverageMapLocation pastZombieLocation = new AverageMapLocation(4);

    @Override
    public void doTurn() throws GameActionException {
        getIdAndBaseLocation();
        if (roundNumber < 2) return;

        Signal[] signals = rc.emptySignalQueue();
        getArchonCount(signals);
        scanForParts(signals);

        repairRobots();

        noticeZombies();

        if (!rc.isCoreReady()) return;

        if (moveTowardBase()) return;

        //if (moveAwayFromEnemiesAndZombies()) return;

        if (buildRobot()) return;

        if (makeSpace()) return;

        //goToParts();
    }

    private void getArchonCount(Signal[] signals) throws GameActionException {
        archonCount = 1;
        for (Signal s : signals) {
            if (s.getTeam() == team
                && SignalUtil.getType(s) == SignalType.ID
                && SignalUtil.getRobotType(s) == RobotType.ARCHON) {
                archonCount++;
            }
        }

        setIndicatorString(2, "archon count is " + archonCount);
        SignalUtil.broadcastID(RobotType.ARCHON, getBaseRadius() * 3, rc);
    }

    private boolean makeSpace() throws GameActionException {
        RobotInfo[] adjacentRobots = rc.senseNearbyRobots(2);
        if (adjacentRobots.length > 3) {
            MapLocation zombieLocation = pastZombieLocation.getAverage();
            if (zombieLocation == null) {
                tryMove(DirectionUtil.getDirectionAwayFrom(adjacentRobots, currentLocation));
            }
            else {
                tryMove(zombieLocation.directionTo(currentLocation));
            }
        }

        return false;
    }

    private void noticeZombies() {
        RobotInfo[] nearbyZombies = senseNearbyZombies();
        for (int i = 0; i < nearbyZombies.length && i < 5; i++) {
            pastZombieLocation.add(nearbyZombies[i].location);
        }
    }

    private boolean moveTowardBase() throws GameActionException {
        if (currentLocation.distanceSquaredTo(baseLocation) > getBaseRadius()) {
            tryMoveToward(baseLocation);
            return true;
        }

        return false;
    }

    private int getBaseRadius() {
        int baseGrowth = (int)Math.sqrt(roundNumber);
        return 10 + baseGrowth;
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

    private boolean moveAwayFromEnemiesAndZombies() throws GameActionException {
        RobotInfo[] nearbyZombies = senseNearbyZombies();
        RobotInfo[] enemies = senseNearbyEnemies();
        if (nearbyZombies.length > 0
                || enemies.length > 0) {
            Direction away = DirectionUtil.getDirectionAwayFrom(enemies, nearbyZombies, currentLocation);
            tryMove(away);
            return true;
        }

        return false;
    }

    private void scanForParts(Signal[] signals) {
        for (Signal s : signals) {
            if (s.getTeam() == team
                    && SignalUtil.getType(s) == SignalType.PARTS) {
                partsLocations.add(SignalUtil.getPartsLocation(s, currentLocation));
            }
        }

        setIndicatorString(0, partsLocations.getSize() + " parts locations in queue");
    }

    private void goToParts() throws GameActionException {
        if (currentLocation.equals(partsDestination)) {
            partsDestination = null;
        }

        if (partsDestination != null
                && rc.canSense(partsDestination)
                && rc.senseParts(partsDestination) == 0) {
            partsDestination = null;
        }

        if (partsDestination != null
            || partsLocations.getSize() > 0) {
            if (partsDestination == null) {
                partsDestination = partsLocations.remove();
            }

            tryMoveToward(partsDestination);
        }
    }

    private boolean buildRobot() throws GameActionException {
        if (rand.nextInt() % archonCount != id) {
            //--balance building between the archons
            return false;
        }

        if (buildQueuePosition < buildQueue.length) {
            if (tryBuild(buildQueue[buildQueuePosition])) {
                buildQueuePosition++;
                return true;
            }
        }
        else {
            if (id == 0
                && !scoutBuilt) {
                if (tryBuild(RobotType.SCOUT)) {
                    scoutBuilt = true;
                    return true;
                }
            }

            if (tryBuild(RobotType.TURRET)) return true;
        }

        return false;
    }

    protected boolean tryBuild(RobotType robotType) throws GameActionException {
        if (rc.getTeamParts() < robotType.partCost) {
            return false;
        }

        //--Build robot in some random direction
        for (int i = 0; i < 8; i++) {
            if (rc.canBuild(directions[i], robotType)) {
                rc.build(directions[i], robotType);
                return true;
            }
        }

        return false;
    }
}
