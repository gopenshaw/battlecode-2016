package bernie;

import battlecode.common.*;
import bernie.util.DirectionUtil;
import bernie.util.RobotUtil;
import bernie.util.SignalUtil;
import scala.xml.PrettyPrinter;

public class Soldier extends Robot {
    private MapLocation zombieDen;

    public Soldier(RobotController rc) {
        super(rc);
    }

    @Override
    public void doTurn() throws GameActionException {
        readSignals();

        tryAttackAndKite();

        tryMoveTowardZombieDen();
    }

    private void tryMoveTowardZombieDen() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (zombieDen != null) {
            if (rc.canSense(zombieDen)
                    && rc.senseRobotAtLocation(zombieDen) == null) {
                zombieDen = null;
            }
            else {
                tryMoveToward(zombieDen);
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
                    && SignalUtil.getType(s) == SignalType.ENEMY
                    && SignalUtil.getRobotData(s, currentLocation).type == RobotType.ZOMBIEDEN) {
                zombieDen = SignalUtil.getRobotData(s, currentLocation).location;
            }
        }
    }
}
