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
            rc.setIndicatorString(2, "received team signal: " + message[0] + " " + message[1]);
            int currentEnemy = message[1];
            if (currentEnemy == enemyId
                    || enemyId == 0) {
                enemyLocation = LocationUtil.decode(message[0], rc.getLocation());
                enemyId = currentEnemy;
            }

            break;
        }
    }
}
