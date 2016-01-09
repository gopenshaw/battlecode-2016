package bernie;

import battlecode.common.*;

public class Soldier extends Robot {
    public Soldier(RobotController rc) {
        super(rc);
    }

    @Override
    public void doTurn(RobotController rc) throws GameActionException {
        RobotInfo[] attackableZombies = senseAttackableZombies();
        if (attackableZombies.length > 0) {
            if (!rc.isWeaponReady()) {
                return;
            }

            RobotInfo robotToAttack = Util.getLowestHealthRobot(attackableZombies);
            rc.attackLocation(robotToAttack.location);
            return;
        }

        RobotInfo[] attackableEnemies = senseAttackableEnemies();
        if (attackableEnemies.length > 0) {
            if (!rc.isWeaponReady()) {
                return;
            }

            RobotInfo robotToAttack = Util.getLowestHealthRobot(attackableEnemies);
            rc.attackLocation(robotToAttack.location);
            return;
        }

        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] teammates = rc.senseNearbyRobots(2, team);
        if (teammates.length > 3) {
            Direction away = DirectionUtil.getDirectionAwayFrom(teammates, rc);
            tryMove(away);
        }
    }
}
