package charlie2;

import battlecode.common.*;

public class Scout extends Robot {
    private int signalsBroadcast = 0;
    private boolean broadcastRubbleDone = false;

    private StringBuilder broadcast = new StringBuilder();

    private RobotInfo[] zombies;
    private RobotInfo[] enemies;

    private Direction direction = Direction.SOUTH;

    public Scout(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        signalsBroadcast = 0;

        broadcastZombies();
        broadcastEnemies();

        broadcastParts();

        explore();

        setIndicatorString(0, broadcast.toString());
        broadcast = new StringBuilder();
    }

    private void explore() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (!rc.onTheMap(rc.getLocation().add(direction))) {
            direction = direction.rotateLeft().rotateLeft();
        }
        boolean moved = trySafeMove(direction, enemies, zombies);
        if (!moved) {
            setIndicatorString(2, "could not find safe move");
            tryMove(direction);
        }
    }

    private void broadcastParts() throws GameActionException {
        if (broadcastRubbleDone) return;
        int currentX = currentLocation.x;
        int currentY = currentLocation.y;
        for (int i = -5; i <= 5; i++) {
            for (int j = -5; j <= 5; j++) {
                int x = currentX + i;
                int y = currentY + j;
                MapLocation mapLocation = new MapLocation(x, y);
                if (rc.onTheMap(mapLocation)) {
                    double rubble = rc.senseParts(mapLocation);
                    if (rubble > 0) {
                        if (++signalsBroadcast > GameConstants.MESSAGE_SIGNALS_PER_TURN) {
                            return;
                        }

                        SignalUtil.broadcastParts(mapLocation, senseRadius * 2, rc);
                    }
                }
            }
        }

        broadcastRubbleDone = true;
    }

    private void broadcastEnemies() throws GameActionException {
        enemies = senseNearbyEnemies();
        broadcastRobots(enemies);
    }

    private void broadcastZombies() throws GameActionException {
        zombies = senseNearbyZombies();
        broadcastRobots(zombies);
    }

    private void broadcastRobots(RobotInfo[] robots) throws GameActionException {
        for (RobotInfo robot : robots) {
            if (++signalsBroadcast > GameConstants.MESSAGE_SIGNALS_PER_TURN) {
                break;
            }

            SignalUtil.broadcastEnemy(robot, 200, rc);
            broadcast.append(robot.location + " " + robot.health + "; ");
        }
    }
}
