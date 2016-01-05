package alpha;

import battlecode.common.*;

import java.util.Random;

public abstract class Robot {
    protected final Random rand;
    protected final Team team;
    protected final Team enemy;
    private final RobotController rc;

    protected int senseRadius;

    private final Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
        Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

    public Robot(RobotController rc) {
        this.rc = rc;
        rand = new Random(rc.getID());
        team = rc.getTeam();
        enemy = team.opponent();
        senseRadius = rc.getType().sensorRadiusSquared;
    }

    public void run(RobotController rc) {
        try {
            while(true) {
                doTurn(rc);
                Clock.yield();
            }
        }
        catch (GameActionException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    protected void updateType(RobotController rc) {
        senseRadius = rc.getType().sensorRadiusSquared;
    }

    protected abstract void doTurn(RobotController rc) throws GameActionException;

    protected RobotInfo[] senseNearbyEnemies() {
        return rc.senseNearbyRobots(senseRadius, enemy);
    }

    protected RobotInfo[] senseNearbyZombies() {
        return rc.senseNearbyRobots(senseRadius, Team.ZOMBIE);
    }

    protected void tryMoveToward(MapLocation zombieLocation) throws GameActionException {
        tryMove(rc.getLocation().directionTo(zombieLocation));
    }

    protected void tryMove(Direction direction) throws GameActionException {
        rc.setIndicatorString(0, "trying to move " + direction);
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
    }

    protected void tryBuild(RobotType robotType) throws GameActionException {
        //--Assuming we have the parts to build
        //--Build robot in some random direction
        for (int i = 0; i < 8; i++) {
            if (rc.canBuild(directions[i], robotType)) {
                rc.build(directions[i], robotType);
                return;
            }
        }
    }
}
