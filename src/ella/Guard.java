package ella;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import ella.util.DirectionUtil;
import ella.util.RobotUtil;

public class Guard extends Robot {
    private RobotInfo[] attackableZombies;
    private RobotInfo[] nearbyZombies;
    private RobotInfo[] adjacentTeammates;

    public Guard(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        senseRobots();
        attackZombies();
        moveTowardZombies();
        spreadOut();
    }

    private void senseRobots() {
        attackableZombies = senseAttackableZombies();
        nearbyZombies = senseNearbyZombies();
        adjacentTeammates = rc.senseNearbyRobots(2, team);
    }

    private void attackZombies() throws GameActionException {
        if (!rc.isWeaponReady()) {
            return;
        }

        if (attackableZombies.length > 0) {
            rc.attackLocation(attackableZombies[0].location);
        }
    }

    private void moveTowardZombies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        int adjacentTurrets = RobotUtil.getCountOfType(adjacentTeammates, RobotType.TURRET);
        if (adjacentTurrets < 1) {
            return;
        }

        if (attackableZombies.length == 0
                && nearbyZombies.length > 0) {
            tryMoveToward(nearbyZombies[0].location);
        }
    }

    private void spreadOut() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (adjacentTeammates.length > 6) {
            tryMove(DirectionUtil.getDirectionAwayFrom(adjacentTeammates, currentLocation));
        }
    }
}
