package pampa;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import pampa.util.DirectionUtil;
import pampa.util.RobotUtil;

public class Guard extends Robot {
    private RobotInfo[] attackableZombies;
    private RobotInfo[] nearbyZombies;
    private RobotInfo[] attackableEnemies;
    private RobotInfo[] nearbyEnemies;

    public Guard(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        senseRobots();
        moveAwayFromDens();
        attackZombies();
        moveTowardZombies();
        attackEnemies();
        moveTowardEnemies();
    }

    private void moveTowardEnemies() throws GameActionException {
        if (attackableEnemies.length > 0
                || !rc.isCoreReady()) {
            return;
        }

        rc.move(DirectionUtil.getDirectionToward(nearbyEnemies, currentLocation));
    }

    private void attackEnemies() throws GameActionException {
        if (attackableEnemies.length == 0
                || !rc.isWeaponReady()) {
            return;
        }

        RobotInfo lowestHealth = RobotUtil.getLowestHealthRobot(attackableEnemies);
        rc.attackLocation(lowestHealth.location);
    }

    private void moveAwayFromDens() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo den = RobotUtil.getRobotOfType(nearbyZombies, RobotType.ZOMBIEDEN);
        if (den == null
                || !den.location.isAdjacentTo(currentLocation)) {
            return;
        }

        if (getRoundsTillNextSpawn(roundNumber) <= 2) {
            setIndicatorString(0, "try move away from den");
            tryMove(den.location.directionTo(currentLocation));
        }
    }

    private void senseRobots() {
        attackableZombies = senseAttackableZombies();
        attackableEnemies = senseAttackableEnemies();
        nearbyEnemies = senseNearbyEnemies();
        nearbyZombies = senseNearbyZombies();
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
        if (!rc.isCoreReady()
                || attackableZombies.length > 0
                || nearbyZombies.length == 0) {
            return;
        }

        RobotInfo zombieToMoveToward = getZombieToMoveToward(nearbyZombies);
        if (zombieToMoveToward != null) {
            setIndicatorString(0, "go to zombie " + zombieToMoveToward.location);
            tryMoveToward(zombieToMoveToward.location);
        }
    }

    private RobotInfo getZombieToMoveToward(RobotInfo[] nearbyZombies) {
        RobotInfo zombie = RobotUtil.getRobotCanAttack(nearbyZombies);
        if (zombie != null) {
            return zombie;
        }

        boolean denIsSafe = getRoundsTillNextSpawn(roundNumber) > 2;
        for (RobotInfo robot : nearbyZombies) {
            if (robot.type != RobotType.ZOMBIEDEN
                    || denIsSafe) {
                return robot;
            }
        }

        return null;
    }
}
