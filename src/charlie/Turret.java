package charlie;

import battlecode.common.*;

public class Turret extends Robot {
    public Turret(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        Signal[] signals = rc.emptySignalQueue();

        if (!rc.isWeaponReady()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        MapLocation attackLocation = null;
        int lowestHealth = 1000000;
        for (int i = 0; i < signals.length; i++) {
            if (signals[i].getTeam() == team
                    && SignalUtil.getType(signals[i]) == SignalType.ENEMY) {
                RobotData robotData = SignalUtil.getRobotData(signals[i], currentLocation);
                sb.append(robotData + "; ");
                if (robotData.health < lowestHealth
                        && rc.canAttackLocation(robotData.location)) {
                    attackLocation = robotData.location;
                    lowestHealth = robotData.health;
                }
            }
        }

        setIndicatorString(1, sb.toString());

        if (attackLocation != null) {
            setIndicatorString(0, "health " + lowestHealth + " " + attackLocation);
            rc.attackLocation(attackLocation);
        }
    }
}
