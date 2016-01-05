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
        if (rc.canMove(direction)) {
            rc.move(direction);
        }
    }
}
