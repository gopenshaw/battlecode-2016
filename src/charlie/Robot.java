package charlie;

import battlecode.common.*;

import java.util.Random;

public abstract class Robot {
    protected final Random rand;
    protected final Team team;
    protected final Team enemy;
    protected final RobotController rc;

    protected int senseRadius;
    protected int attackRadius;
    protected MapLocation currentLocation;

    protected final Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
        Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

    public Robot(RobotController rc) {
        this.rc = rc;
        rand = new Random(rc.getID());
        team = rc.getTeam();
        enemy = team.opponent();

        updateTypeParams();
    }

    protected abstract void doTurn() throws GameActionException;

    public void run(RobotController rc) {
        while (true) {
            try {
                currentLocation = rc.getLocation();
                doTurn();
                Clock.yield();
            }
            catch (GameActionException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    protected void updateTypeParams() {
        RobotType type = rc.getType();
        senseRadius = type.sensorRadiusSquared;
        attackRadius = type.attackRadiusSquared;
    }


    protected void setIndicatorString(int i, String s) {
        int roundNum = rc.getRoundNum();
        rc.setIndicatorString(i, String.format("%d: %s", roundNum, s));
    }

    protected RobotInfo[] senseNearbyEnemies() {
        return rc.senseNearbyRobots(senseRadius, enemy);
    }

    protected RobotInfo[] senseNearbyZombies() {
        return rc.senseNearbyRobots(senseRadius, Team.ZOMBIE);
    }

    protected void tryMoveToward(MapLocation location) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        Direction moveDirection = currentLocation.directionTo(location);

        if (location.isAdjacentTo(currentLocation)) {
            double rubble = rc.senseRubble(location);
            if (rubble >= 100) {
                rc.clearRubble(moveDirection);
                return;
            }
        }

        tryMove(moveDirection);
    }

    protected boolean trySafeMove(Direction direction,
                                  RobotInfo[] nearbyEnemies,
                                  RobotInfo[] nearbyZombies) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        MapLocation next = currentLocation.add(direction);
        if (canMoveSafely(direction, next, nearbyEnemies, nearbyZombies)) {
            rc.move(direction);
            return true;
        }

        Direction left = direction.rotateLeft();
        next = currentLocation.add(left);
        if (canMoveSafely(left, next, nearbyEnemies, nearbyZombies)) {
            rc.move(left);
            return true;
        }

        Direction right = direction.rotateRight();
        next = currentLocation.add(right);
        if (canMoveSafely(right, next, nearbyEnemies, nearbyZombies)) {
            rc.move(right);
            return true;
        }

        for (int i = 0; i < 2; i++) {
            left = left.rotateLeft();
            next = currentLocation.add(left);
            if (canMoveSafely(left, next, nearbyEnemies, nearbyZombies)) {
                rc.move(left);
                return true;
            }

            right = right.rotateRight();
            next = currentLocation.add(right);
            if (canMoveSafely(right, next, nearbyEnemies, nearbyZombies)) {
                rc.move(right);
                return true;
            }
        }

        return false;
    }

    private boolean canMoveSafely(Direction direction, MapLocation next, RobotInfo[] nearbyEnemies, RobotInfo[] nearbyZombies) {
        return rc.canMove(direction)
                && !Util.anyCanAttack(nearbyEnemies, next)
                && !Util.anyCanAttack(nearbyZombies, next);
    }

    protected void tryMove(Direction direction) throws GameActionException {
        if (rc.canMove(direction)) {
            rc.move(direction);
            return;
        }

        Direction left = direction.rotateLeft();
        if (rc.canMove(left)) {
            rc.move(left);
            return;
        }

        Direction right = direction.rotateRight();
        if (rc.canMove(right)) {
            rc.move(right);
            return;
        }

        for (int i = 0; i < 2; i++) {
            left = left.rotateLeft();
            if (rc.canMove(left)) {
                rc.move(left);
                return;
            }

            right = right.rotateRight();
            if (rc.canMove(right)) {
                rc.move(right);
                return;
            }
        }

        tryClearRubble(direction);
    }

    private void tryClearRubble(Direction direction) throws GameActionException {
        MapLocation nextLocation = rc.getLocation().add(direction);
        double rubble = rc.senseRubble(nextLocation);
        if (rubble > 100) {
            rc.clearRubble(direction);
        }
    }
}
