package bernie;

import battlecode.common.*;
import battlecode.common.Signal;
import bernie.util.DirectionUtil;
import bernie.util.RobotUtil;
import bernie.util.SignalUtil;

public class Soldier extends Robot {
    private MapLocation enemyLocation;
    StringBuilder sb = new StringBuilder();

    public Soldier(RobotController rc) {
        super(rc);
    }

    @Override
    public void doTurn() throws GameActionException {
        readSignals();
        setIndicatorString(0, sb.toString());
        sb = new StringBuilder();
        setIndicatorString(1, "" + currentLocation);

        tryAttackAndKite();

        tryMoveTowardEnemies();
    }

    private void tryMoveTowardEnemies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (enemyLocation != null) {
            if (rc.canSense(enemyLocation)
                    && rc.senseRobotAtLocation(enemyLocation) == null) {
                enemyLocation = null;
            }
            else {
                tryMoveToward(enemyLocation);
            }
        }
    }

    private void tryAttackAndKite() throws GameActionException {
        RobotInfo[] attackableZombies = senseAttackableZombies();
        if (attackableZombies.length > 0) {
            if (rc.isWeaponReady()) {
                RobotInfo robotToAttack = RobotUtil.getLowestHealthRobot(attackableZombies);
                rc.attackLocation(robotToAttack.location);
            }
        }

        RobotInfo[] attackableEnemies = senseAttackableEnemies();
        if (attackableEnemies.length > 0) {
            if (rc.isWeaponReady()) {
                RobotInfo robotToAttack = RobotUtil.getLowestHealthRobot(attackableEnemies);
                rc.attackLocation(robotToAttack.location);
            }
        }

        if (!rc.isCoreReady()) {
            return;
        }

        if (RobotUtil.anyCanAttack(attackableZombies, currentLocation)) {
            tryMove(DirectionUtil.getDirectionAwayFrom(attackableZombies, rc));
        }
    }

    private void readSignals() {
        Signal[] signals = rc.emptySignalQueue();
        for (Signal s : signals) {
            if (s.getTeam() == team
                    && SignalUtil.getType(s) == SignalType.ENEMY) {
                sb.append(SignalUtil.getRobotData(s, currentLocation));
                enemyLocation = SignalUtil.getRobotData(s, currentLocation).location;
            }
        }
    }
}
