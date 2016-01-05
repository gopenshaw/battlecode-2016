package alpha;

import battlecode.common.*;

public class Soldier extends Robot {
    public Soldier(RobotController rc) {
        super(rc);
    }

    @Override
    public void doTurn(RobotController rc) throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] nearbyEnemies = senseNearbyEnemies();
        if (nearbyEnemies.length > 0) {
            moveToward(nearbyEnemies[0], rc);
        }
        else {
            moveRandom(rc);
        }
    }

    private void moveRandom(RobotController rc) throws GameActionException {
        Direction[] d = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
                Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
        Direction random = d[rand.nextInt(8)];
        if (rc.canMove(random)) {
            rc.move(random);
        }
    }

    private void moveToward(RobotInfo nearbyRobot, RobotController rc) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        Direction d = currentLocation.directionTo(nearbyRobot.location);
        if (rc.canMove(d)) {
            rc.move(d);
        }
    }
}
