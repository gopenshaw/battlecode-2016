package jeremy;

import battlecode.common.*;
import jeremy.message.Message;
import jeremy.message.MessageBuilder;
import jeremy.util.DirectionUtil;
import jeremy.util.LocationUtil;
import jeremy.util.RobotUtil;

public class Scout extends Robot {
    private static final int ROUNDS_TO_REVERSE = 4;
    private final int ZOMBIE_BROADCAST_RADIUS = senseRadius * 4;
    private Direction exploreDirection;
    private final int LOOKAHEAD_LENGTH = 5;
    private RobotInfo[] nearbyZombies;
    private int ignoreEnemiesRound;

    public Scout(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        senseZombies();
        broadcastZombies();
        moveAwayFromZombies();
        explore();
    }

    private void broadcastZombies() throws GameActionException {
        if (nearbyZombies.length == 0) {
            return;
        }
        RobotInfo closestZombie = RobotUtil.getClosestRobotToLocation(nearbyZombies, currentLocation);
        Message zombieMessage = MessageBuilder.buildZombieMessage(closestZombie, roundNumber);
        rc.broadcastMessageSignal(zombieMessage.getFirst(), zombieMessage.getSecond(), ZOMBIE_BROADCAST_RADIUS);
    }

    private void senseZombies() {
        nearbyZombies = senseNearbyZombies();
        setIndicatorString(0, "zombies sensed: " + nearbyZombies.length);
    }

    private void moveAwayFromZombies() throws GameActionException {
        if (nearbyZombies.length == 0
                || !rc.isCoreReady()) {
            return;
        }

        tryMove(DirectionUtil.getDirectionAwayFrom(nearbyZombies, currentLocation));
    }

    private void explore() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (exploreDirection == null) {
            exploreDirection = getExploreDirection(null);
            setIndicatorString(1, "explore: " + exploreDirection);
        }

        if (senseNearbyEnemies().length > 0
                && roundNumber > ignoreEnemiesRound + ROUNDS_TO_REVERSE) {
            exploreDirection = exploreDirection.opposite();
            ignoreEnemiesRound = roundNumber;
        }
        else {
            //--check ahead
            MapLocation lookaheadLocation = currentLocation.add(exploreDirection, LOOKAHEAD_LENGTH);
            Direction newDirection = null;
            while (!rc.onTheMap(lookaheadLocation)) {
                newDirection = getExploreDirection(exploreDirection);
                setIndicatorString(1, "new: " + newDirection);
                lookaheadLocation = currentLocation.add(newDirection, LOOKAHEAD_LENGTH);
            }

            if (newDirection != null) {
                exploreDirection = newDirection;
            }
        }


        if (rc.isCoreReady()) {
            tryMove(exploreDirection);
        }
    }


    private Direction getExploreDirection(Direction previousDirection) {
        if (previousDirection == null) {
            return getRandomDirection();
        }

        Direction newDirection = getRandomDirection();
        while (newDirection.equals(exploreDirection)
                || newDirection.opposite() == exploreDirection) {
            newDirection = getRandomDirection();
        }

        return newDirection;
    }
}
