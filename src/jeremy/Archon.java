package jeremy;

import battlecode.common.*;
import jeremy.message.Message;
import jeremy.message.MessageBuilder;
import jeremy.util.AverageMapLocation;
import jeremy.util.BoundedQueue;
import jeremy.util.DirectionUtil;

public class Archon extends Robot {

    private RobotType[] buildQueue = {RobotType.SCOUT};
    private int buildQueuePosition = 0;
    private RobotInfo[] nearbyZombies;
    private AverageMapLocation previousZombieLocation = new AverageMapLocation(5);

    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        senseZombies();
        moveAwayFromZombies();
        buildRobots();
        moveIfSafe();
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

        if (buildQueuePosition < buildQueue.length) {
            if (tryBuild(buildQueue[buildQueuePosition])) {
                buildQueuePosition++;
            }
        }
        else {
            tryBuild(RobotType.SOLDIER);
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
}
