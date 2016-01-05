package alpha;

import battlecode.common.*;

import java.util.Random;

public class Robot {
    protected final Random rand;
    protected final Team team;
    protected final Team enemy;
    protected final int senseRadius;
    private final RobotController rc;

    public Robot(RobotController rc) {
        this.rc = rc;
        rand = new Random(rc.getID());
        team = rc.getTeam();
        enemy = team.opponent();
        senseRadius = rc.getType().sensorRadiusSquared;
    }

    protected RobotInfo[] senseNearbyEnemies() {
        return rc.senseNearbyRobots(senseRadius, enemy);
    }

    protected RobotInfo[] senseNearbyZombies() {
        return rc.senseNearbyRobots(senseRadius, Team.ZOMBIE);
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
}
