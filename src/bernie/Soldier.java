package bernie;

import battlecode.common.*;
import scala.tools.nsc.backend.icode.analysis.CopyPropagation;

public class Soldier extends Robot {
    private MapLocation lastKnownEnemyLocation = null;

    public Soldier(RobotController rc) {
        super(rc);
    }

    @Override
    public void doTurn(RobotController rc) throws GameActionException {
        if (rc.isWeaponReady()) {
            RobotInfo[] attackableEnemies = senseAttackableEnemies();
            if (attackableEnemies.length > 0) {
                RobotInfo robotToAttack = Util.getLowestHealthRobot(attackableEnemies);
                rc.attackLocation(robotToAttack.location);
                return;
            }

            RobotInfo[] attackableZombies = senseAttackableZombies();
            if (attackableZombies.length > 0) {
                RobotInfo robotToAttack = Util.getLowestHealthRobot(attackableZombies);
                rc.attackLocation(robotToAttack.location);
                return;
            }
        }


        if (!rc.isCoreReady()) {
            return;
        }

        Signal[] signals = rc.emptySignalQueue();
        for (Signal s : signals) {
            if (s.getTeam() == team) {
                int[] message = s.getMessage();
                if (message == null) continue;

                lastKnownEnemyLocation = LocationUtil.decode(message[0], rc.getLocation());
                break;
            }
        }

        if (lastKnownEnemyLocation != null) {
            tryMoveToward(lastKnownEnemyLocation);
        }
    }
}
