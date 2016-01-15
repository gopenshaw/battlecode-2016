package jeremy;

import battlecode.common.*;
import jeremy.message.Message;
import jeremy.message.MessageBuilder;
import jeremy.util.DirectionUtil;

public class Archon extends Robot {

    private RobotType[] buildQueue = {RobotType.SCOUT};
    private int buildQueuePosition = 0;

    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        moveAwayFromZombies();
        buildRobots();
//        requestSpace();
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

    private void requestSpace() throws GameActionException {
        MapLocation[] adjacent = MapLocation.getAllMapLocationsWithinRadiusSq(currentLocation, 2);
        for (MapLocation location : adjacent) {
            if (rc.senseRobotAtLocation(location) == null) {
                return;
            }
        }

        Message spreadMessage = MessageBuilder.buildSpreadMessage(rc.getHealth(), rc.getID(), type, currentLocation);
        sendMessage(spreadMessage, 2);
    }

    private void moveAwayFromZombies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        if (nearbyZombies.length > 0) {
            tryMove(DirectionUtil.getDirectionAwayFrom(nearbyZombies, currentLocation));
        }
    }
}
