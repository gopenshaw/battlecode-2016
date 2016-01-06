package alpha;

import battlecode.common.*;

public class Scout extends Robot {
    public Scout(RobotController rc) {
        super(rc);
    }

    Direction direction = Direction.SOUTH;
    int lastRoundBroadcasted = 0;
    final int BROADCAST_DELAY = 10;

    @Override
    protected void doTurn(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() > lastRoundBroadcasted + BROADCAST_DELAY) {
            RobotInfo[] nearbyEnemies = senseNearbyEnemies();
            for (RobotInfo enemy : nearbyEnemies) {
                if (enemy.type == RobotType.ARCHON) {
                    int encodedLocation = LocationUtil.encode(enemy.location);
                    rc.setIndicatorString(1, "encoded " + encodedLocation);
                    rc.broadcastMessageSignal(encodedLocation, enemy.ID, 2000);
                }
            }

            lastRoundBroadcasted = rc.getRoundNum();
        }

        if (!rc.isCoreReady()) {
            return;
        }

        if (!rc.onTheMap(rc.getLocation().add(direction))) {
            direction = direction.rotateLeft().rotateLeft();
        }

        tryMove(direction);
    }
}
