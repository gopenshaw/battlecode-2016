package bernie;

import battlecode.common.*;
import bernie.util.RobotUtil;

import java.util.Random;

public abstract class Robot {
    protected final Random rand;
    protected final Team team;
    protected final Team enemy;
    protected final RobotController rc;

    protected int senseRadius;
    protected int attackRadius;

    protected MapLocation currentLocation;
    protected int roundNumber;

    private final Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
        Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

    public Robot(RobotController rc) {
        this.rc = rc;
        rand = new Random(rc.getID());
        team = rc.getTeam();
        enemy = team.opponent();
        currentLocation = rc.getLocation();

        updateTypeParams(rc);
    }

    public void run() {
        while(true) {
            try {
                currentLocation = rc.getLocation();
                roundNumber = rc.getRoundNum();
                doTurn();
                Clock.yield();
            } catch (GameActionException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    protected void updateTypeParams(RobotController rc) {
        RobotType type = rc.getType();
        senseRadius = type.sensorRadiusSquared;
        attackRadius = type.attackRadiusSquared;
    }

    protected abstract void doTurn() throws GameActionException;

    protected RobotInfo[] senseNearbyEnemies() {
        return rc.senseNearbyRobots(senseRadius, enemy);
    }

    protected RobotInfo[] senseAttackableEnemies() {
        return rc.senseNearbyRobots(attackRadius, enemy);
    }

    protected RobotInfo[] senseAttackableZombies() {
        return rc.senseNearbyRobots(attackRadius, Team.ZOMBIE);
    }

    protected RobotInfo[] senseNearbyZombies() {
        return rc.senseNearbyRobots(senseRadius, Team.ZOMBIE);
    }

    public RobotInfo[] senseNearbyNeutrals() {
        return rc.senseNearbyRobots(senseRadius, Team.NEUTRAL);
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
                && !RobotUtil.anyCanAttack(nearbyEnemies, next)
                && !RobotUtil.anyCanAttack(nearbyZombies, next);
    }

    protected void tryMoveClockwise(Direction direction) throws GameActionException {
        if (rc.canMove(direction)) {
            rc.move(direction);
            return;
        }

        Direction right = direction.rotateRight();
        if (rc.canMove(right)) {
            rc.move(right);
            return;
        }

        for (int i = 0; i < 6; i++) {
            right = right.rotateRight();
            if (rc.canMove(right)) {
                rc.move(right);
                return;
            }
        }
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

    protected boolean tryBuild(RobotType robotType) throws GameActionException {
        if (rc.getTeamParts() < robotType.partCost) {
            return false;
        }

        //--Build robot in some random direction
        for (int i = 0; i < 8; i++) {
            if (rc.canBuild(directions[i], robotType)) {
                rc.build(directions[i], robotType);
                return true;
            }
        }

        return false;
    }

    protected RobotInfo findAttackableRobot(RobotInfo[] robots) {
        for (RobotInfo r : robots) {
            if (rc.canAttackLocation(r.location)) {
                return r;
            }
        }

        return null;
    }

    protected Direction getRandomDirection() {
        return directions[rand.nextInt(8)];
    }

    protected void setIndicatorString(int i, String s) {
        int roundNum = rc.getRoundNum();
        rc.setIndicatorString(i, String.format("%d: %s", roundNum, s));
    }
}
