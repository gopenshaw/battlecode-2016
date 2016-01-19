package louie;

import battlecode.common.*;
import louie.message.MessageParser;
import louie.nav.Bug;
import louie.util.AverageMapLocation;
import louie.util.DirectionUtil;
import louie.util.LocationUtil;
import louie.util.RobotUtil;

public class Archon extends Robot {

    private RobotType[] lowUnitCountBuildQueue = {
            RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
    };

    private RobotType[] highUnitCountBuildQueue = {
            RobotType.SCOUT, RobotType.TURRET,
    };

    private int lowUnitQueuePosition = 0;
    private int highUnitQueuePosition = 0;

    private RobotInfo[] nearbyZombies;
    private AverageMapLocation previousZombieLocation = new AverageMapLocation(5);
    private Signal[] roundSignals;
    private MapLocation previousPartLocation;
    private RobotInfo[] nearbyEnemies;
    private double lastRoundHealth;
    private RobotInfo[] nearbyFriendlies;

    public Archon(RobotController rc) {
        super(rc);
        Bug.init(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        senseRobots();
        moveAwayFromZombiesAndEnemies();
        moveIfUnderAttack();
        buildRobots();
        convertAdjacentNeutrals();
        moveWithArmy();
        getParts();
        repairRobots();
        lastRoundHealth = rc.getHealth();
    }

    private void moveWithArmy() throws GameActionException {
        if (nearbyEnemies.length > 0
                || nearbyZombies.length > 0
                || nearbyFriendlies.length == 0
                || !rc.isCoreReady()) {
            return;
        }

        tryMove(DirectionUtil.getDirectionToward(nearbyFriendlies, currentLocation));
    }

    private void moveIfUnderAttack() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (rc.getHealth() < lastRoundHealth) {
            tryMove(DirectionUtil.getDirectionToward(nearbyFriendlies, currentLocation));
        }
    }

    private void convertAdjacentNeutrals() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] adjacentNeutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
        if (adjacentNeutrals.length > 0) {
            rc.activate(adjacentNeutrals[0].location);
        }
    }

    private void moveAwayFromZombiesAndEnemies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (nearbyZombies.length > 0
                || RobotUtil.anyCanAttack(nearbyEnemies, currentLocation)) {
            Direction runDirection = DirectionUtil.getDirectionAwayFrom(nearbyEnemies, nearbyZombies, currentLocation);
            //--TODO check if we are going into a corner or some other trap
            tryMove(runDirection);
        }
    }

    private void senseRobots() {
        nearbyZombies = senseNearbyZombies();
        nearbyEnemies = senseNearbyEnemies();
        nearbyFriendlies = senseNearbyFriendlies();
        setIndicatorString(0, "nearby enemies: " + nearbyEnemies.length);
        setIndicatorString(0, "nearby zom: " + nearbyZombies.length);
        for (RobotInfo zombie : nearbyZombies) {
            previousZombieLocation.add(zombie.location);
        }
    }

    private void buildRobots() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (rc.getRobotCount() > Config.HIGH_UNIT_COUNT) {
            if (tryBuild(highUnitCountBuildQueue[highUnitQueuePosition % highUnitCountBuildQueue.length])) {
                highUnitQueuePosition++;
            }
        }
        else {
            if (tryBuild(lowUnitCountBuildQueue[lowUnitQueuePosition % lowUnitCountBuildQueue.length])) {
                lowUnitQueuePosition++;
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
                    }
                }
            }
        }

        if (previousPartLocation != null) {
            tryMoveOnto(previousPartLocation);
        }
    }
}
