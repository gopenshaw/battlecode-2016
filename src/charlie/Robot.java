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

    protected RobotInfo getLowestHealthAttackable(RobotInfo[] robots) {
        double lowestHealth = 100000;
        RobotInfo lowestHealthRobot = null;
        for (RobotInfo robot : robots) {
            if (robot.health < lowestHealth) {
                lowestHealth = robot.health;
                lowestHealthRobot = robot;
            }
        }

        return lowestHealthRobot;
    }
}
