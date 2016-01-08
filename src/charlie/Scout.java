package charlie;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Scout extends Robot {
    public Scout(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        rc.broadcastMessageSignal(0, rc.getRoundNum(), senseRadius * 2);
        /*
        RobotInfo[] enemies = senseNearbyEnemies();
        setIndicatorString(0, String.format("sensed %d robots", enemies.length));
        int signalsSent = 0;
        StringBuilder sb = new StringBuilder();
        for (RobotInfo enemy : enemies) {
            rc.broadcastMessageSignal(Serializer.encode(enemy.location), (int) enemy.health, senseRadius * 2);
            sb.append(enemy.location + " " + enemy.health + "; ");
            if (++signalsSent >= 20) {
                break;
            }
        }

        setIndicatorString(1, sb.toString());
        setIndicatorString(2, "messages completed");
        */
    }
}
