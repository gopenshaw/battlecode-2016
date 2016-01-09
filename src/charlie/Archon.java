package charlie;

import battlecode.common.*;

public class Archon extends Robot {
    public Archon(RobotController rc) {
        super(rc);
    }

    RobotType[] buildQueue = {RobotType.SCOUT};
    int buildQueuePosition = 0;

    @Override
    public void doTurn() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (buildRobot()) {
            return;
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
