package hermit;

import battlecode.common.*;
import hermit.message.Message;
import hermit.message.MessageBuilder;
import hermit.util.DirectionUtil;

public class Archon extends Robot {

    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        moveAwayFromZombies();
        tryBuild(RobotType.GUARD);
        requestSpace();
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
