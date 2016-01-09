package charlie;

import battlecode.common.*;
import charlie.dataStructures.BoundedQueue;

public class Archon extends Robot {
    public Archon(RobotController rc) {
        super(rc);
    }

    RobotType[] buildQueue = {RobotType.SCOUT};
    int buildQueuePosition = 0;

    BoundedQueue<MapLocation> partsLocations = new BoundedQueue<MapLocation>(50);
    MapLocation partsDestination = null;

    @Override
    public void doTurn() throws GameActionException {
        Signal[] signals = rc.emptySignalQueue();
        scanForParts(signals);

        if (!rc.isCoreReady()) {
            return;
        }

        if (buildRobot()) {
            return;
        }

        goToParts();
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
        if (buildQueuePosition < buildQueue.length) {
            if (tryBuild(buildQueue[buildQueuePosition])) {
                buildQueuePosition++;
                return true;
            }
        }
        else {
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
