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
        RobotInfo[] nearbyEnemies = senseNearbyEnemies();
        if (rc.getRoundNum() > lastRoundBroadcasted + BROADCAST_DELAY) {
            for (RobotInfo enemy : nearbyEnemies) {
                if (enemy.type == RobotType.ARCHON) {
                    int encodedLocation = LocationUtil.encode(enemy.location);
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

        boolean moved = trySafeMove(direction, nearbyEnemies, senseNearbyZombies());
        if (!moved) {
            setIndicatorString(2, "could not find safe move");
            tryMove(direction);
        }
    }
}
