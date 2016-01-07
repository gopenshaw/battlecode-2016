package team014;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Soldier extends Robot {
    public Soldier(RobotController rc) {
        super(rc);
    }

    @Override
    public void doTurn(RobotController rc) throws GameActionException {
        RobotInfo[] nearbyZombies = senseNearbyZombies();
        RobotInfo[] nearbyEnemies = senseNearbyEnemies();

        RobotInfo robotToAttack = findAttackableZombieOrRobot(nearbyZombies, nearbyEnemies);
        if (robotToAttack != null
                && rc.isWeaponReady()) {
            rc.attackLocation(robotToAttack.location);
            return;
        }
    }
}
