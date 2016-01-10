package charlie;

import battlecode.common.*;

public class Scout extends Robot {
    private int signalsBroadcast = 0;
    private boolean broadcastRubbleDone = false;

    private Direction previousDirection = null;

    private StringBuilder broadcast = new StringBuilder();

    public Scout(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        signalsBroadcast = 0;

        broadcastZombies();
        broadcastEnemies();

        broadcastParts();

        tryExplore();

        setIndicatorString(0, broadcast.toString());
        broadcast = new StringBuilder();
    }

    private void tryExplore() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (signalsBroadcast > 0 || nextToWall()) {
            previousDirection = directions[rand.nextInt(8)];
        }
        else {
            if (previousDirection == null) {
                previousDirection = directions[rand.nextInt(8)];
            }

            tryMove(previousDirection);
        }
    }

    private boolean nextToWall() throws GameActionException {
        for (int i = 0; i < directions.length; i++) {
            if (!rc.onTheMap(currentLocation.add(directions[i]))) {
                return true;
            }
        }

        return false;
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
        RobotInfo[] enemies = senseNearbyEnemies();
        broadcastRobots(enemies);
    }

    private void broadcastZombies() throws GameActionException {
        RobotInfo[] zombies = senseNearbyZombies();
        broadcastRobots(zombies);
    }

    private void broadcastRobots(RobotInfo[] robots) throws GameActionException {
        for (RobotInfo robot : robots) {
            if (++signalsBroadcast > GameConstants.MESSAGE_SIGNALS_PER_TURN) {
                break;
            }

            SignalUtil.broadcastEnemy(robot, senseRadius * 2, rc);
            broadcast.append(robot.location + " " + robot.health + "; ");
        }
    }
}
