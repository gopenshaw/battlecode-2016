package alpha;

import battlecode.common.*;

public class Viper extends Robot {
    public Viper(RobotController rc) {
        super(rc);
    }

    private MapLocation enemyLocation = null;
    private int enemyId = 0;

    @Override
    protected void doTurn(RobotController rc) throws GameActionException {
        if (rc.isWeaponReady()) {
            RobotInfo[] enemyInAttackRange = senseAttackableEnemies();
            if (enemyInAttackRange.length > 0) {
                RobotInfo lowestHealth = Util.getLowestHealthRobot(enemyInAttackRange);
                rc.attackLocation(lowestHealth.location);
                return;
            }
        }

        if (!rc.isCoreReady()) {
            return;
        }

        updateDestination(rc);

        if (enemyLocation == null) {
            tryMove(getRandomDirection());
        }
        else {
            tryMoveToward(enemyLocation);
        }
    }

    private void updateDestination(RobotController rc) {
        Signal[] signals = rc.emptySignalQueue();
        for (Signal s : signals) {
            if (s.getTeam() == enemy) {
                continue;
            }

            int[] message = s.getMessage();
            if (message == null) continue;

            RobotData sg = SignalUtil.readSignal(s, rc.getLocation());
            setIndicatorString(0, "found a signal w/ a message");
            if (sg.robotId == enemyId
                    || enemyId == 0) {
                enemyLocation = sg.location;
                enemyId = sg.robotId;
            }

            break;
        }
    }
}
